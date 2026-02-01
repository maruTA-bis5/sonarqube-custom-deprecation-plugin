# SonarQube Custom Deprecation Plugin 実装プラン（ブラッシュアップ版）

## 概要
ライブラリ側で非推奨マークしていないAPIを、利用プロジェクト側で非推奨扱いにするためのSonarQubeプラグイン。
本プランは、自動実装エージェントが「人間からの指摘・指示・承認を受けずに」完全に実装を完遂できることを目標に設計されています。

## 技術仕様

### 環境
- **Java**: 17
- **ビルドツール**: Maven 3.8.1 以上
- **対象SonarQube**: 9.9
    - API version: 9.14.0.375
- **対象SonarJava**: 7.16.0.30901
- **SonarLint対応**: 必須
- **プラグイン形式**: JAR（sonar-packaging-maven-plugin で自動生成）

### Javadoc 参照URL（自律調査の必須情報）
API利用方法が不明な場合は、以下のURL規則でJavadocを参照する。

**URL規則**:
`https://javadoc.io/static/${groupId}/${artifactId}/${version}/index.html`

**例（sonar-plugin-api 9.14.0.375）**:
https://javadoc.io/static/org.sonarsource.api.plugin/sonar-plugin-api/9.14.0.375/index.html

### 検知対象
以下のJavaコード要素の使用を検知：
- ✅ メソッド呼び出し（インスタンスメソッド・静的メソッド）
- ✅ フィールドアクセス（定数含む）
- ✅ コンストラクタ呼び出し（含む `<init>` メンバ）
- ✅ 静的インポート
- ✅ スーパークラスのメソッド呼び出し（継承チェーン対応）
- ❌ アノテーション使用（対象外）

### Issue設定
- **カテゴリ**: Maintainability
- **Severity**: Minor
- **Remediation**: 設定で指定した移行先を表示
- **報告メッセージ形式**: `This API is deprecated for this project. [migration_text] ([note_text])`

---

## プロジェクト構造

```
sonarqube-custom-deprecation-plugin/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── net/bis5/sonarqube/custodeprecation/
│   │   │       ├── CustomDeprecationPlugin.java          # プラグインエントリポイント
│   │   │       ├── CustomDeprecationRulesDefinition.java # ルール定義
│   │   │       ├── CustomDeprecationCheck.java           # メイン検査ロジック
│   │   │       └── DeprecatedApiConfig.java              # 設定パース用モデル
│   │   └── resources/
│   │       └── org/sonar/l10n/java/rules/custodeprecation/
│   │           ├── CustomDeprecationCheck.html           # ルール説明HTML
│   │           └── CustomDeprecationCheck.json           # ルールメタデータ
│   └── test/
│       ├── java/
│       │   └── net/bis5/sonarqube/custodeprecation/
│       │       └── CustomDeprecationCheckTest.java       # ユニットテスト
│       └── files/
│           └── CustomDeprecationCheck.java               # テストケース用サンプルコード
└── README.md
```

---

## 実装ステップ（詳細仕様付き）

### Phase 1: プロジェクトセットアップ & 自動テストアーティファクト作成

#### 1.1 pom.xml作成（完全な依存関係構成）
ビルド可能な完全な `pom.xml` を作成。以下は必須項目：

**必須プロパティ**:
```xml
<groupId>net.bis5.sonarqube</groupId>
<artifactId>sonarqube-custom-deprecation-plugin</artifactId>
<version>1.0.0</version>
<packaging>sonar-plugin</packaging>
<java.version>17</java.version>
<maven.compiler.source>17</maven.compiler.source>
<maven.compiler.target>17</maven.compiler.target>
<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
<sonar.api.version>9.14.0.375</sonar.api.version>
<sonarjava.version>7.16.0.30901</sonarjava.version>
```

**必須依存関係**:
- `org.sonarsource.api.plugin:sonar-plugin-api:${sonar.api.version}` (provided)
- `org.sonarsource.java:sonar-java-plugin:${sonarjava.version}` (provided)
- `org.sonarsource.java:java-frontend:${sonarjava.version}` (provided)
- `org.sonarsource.java:java-checks-testkit:${sonarjava.version}` (test)
- `com.google.code.gson:gson:2.10.1` (compile)
- `junit:junit:4.13.2` (test)

**必須ビルドプラグイン**:
- `sonar-packaging-maven-plugin:1.21.0.505` 
  - pluginKey: `custodeprecation`
  - pluginClass: `net.bis5.sonarqube.custodeprecation.CustomDeprecationPlugin`
  - pluginName: `Custom Deprecation`
  - pluginDescription: `Detects usage of project-specific deprecated APIs`
  - sonarLintSupported: `true`

#### 1.2 プラグインエントリポイント作成
**ファイル**: `src/main/java/net/bis5/sonarqube/custodeprecation/CustomDeprecationPlugin.java`

**実装要件**:
- `org.sonar.api.Plugin` インターフェースを実装
- `context.addExtension(CustomDeprecationRulesDefinition.class)` で ルール定義を登録
- `context.addExtension(CustomDeprecationCheck.class)` で チェックを登録

**エージェント実装時の判定基準**:
- SonarQube起動時にエラーが出ない
- プラグイン一覧に表示される

### Phase 2: ルール定義と検査ロジック（TDD厳守）

**⚠️ 実装方針：TDD（Test-Driven Development）の厳密な実践**

t-wadaのTDDサイクル（Red-Green-Refactor）を全フェーズで実装：
1. **Red**: テストファイルを作成し、期待動作をコード化。実行して失敗を確認。
2. **Green**: テストをパスさせる最小限の実装を追加。
3. **Refactor**: コードを整理・改善（テストは常にパス状態を維持）。

**テスト-実装 の順序が絶対条件**（コミット履歴で検証可能）

#### 2.1 テスト用ルールメタデータと説明
**ファイル**: `src/test/resources/org/sonar/l10n/java/rules/custodeprecation/CustomDeprecationCheck.json`
**ファイル**: `src/test/resources/org/sonar/l10n/java/rules/custodeprecation/CustomDeprecationCheck.html`

テスト実行時に必要なメタデータを含める。

#### 2.2 本体用ルールメタデータ
**ファイル**: `src/main/resources/org/sonar/l10n/java/rules/custodeprecation/CustomDeprecationCheck.json`

```json
{
  "title": "Custom deprecated API should not be used",
  "type": "CODE_SMELL",
  "status": "ready",
  "remediation": {
    "func": "Constant/Issue",
    "constantCost": "10min"
  },
  "tags": ["convention", "obsolete"],
  "defaultSeverity": "Minor"
}
```

#### 2.3 ルール説明HTML
**ファイル**: `src/main/resources/org/sonar/l10n/java/rules/custodeprecation/CustomDeprecationCheck.html`

簡潔で明確な説明：
```html
<h2>Why is this an issue?</h2>
<p>This rule detects usage of APIs marked as deprecated in your project's configuration.</p>

<h2>Configuration</h2>
<p>Configure the deprecated APIs in the <code>deprecatedApis</code> rule parameter as a JSON array.</p>

<h2>Noncompliant Code Example</h2>
<pre>
OldApi api = new OldApi();
api.oldMethod(); // Noncompliant
</pre>

<h2>Compliant Solution</h2>
<pre>
NewApi api = new NewApi();
api.newMethod();
</pre>
```

#### 2.4 ルール定義クラス
**ファイル**: `src/main/java/net/bis5/sonarqube/custodeprecation/CustomDeprecationRulesDefinition.java`

**実装要件**:
- `org.sonar.api.server.rule.RulesDefinition` を実装
- リポジトリキー: `custodeprecation`
- ルールキー: `CustomDeprecation`（**全ファイルで統一**）
- パラメータ名: `deprecatedApis`
- パラメータ型: `STRING` (JSON形式)
- デフォルト値: `[]` （空配列）
- 説明: 十分に詳細な説明を含める

**エージェント実装時の検証**:
- `mvn clean test` が成功（テストがパスする）
- 生成されたJARにルール定義が含まれる
#### 2.5 設定フォーマット（JSON）の完全仕様

```json
[
  {
    "fqcn": "com.example.library.OldClass",
    "member": "legacyMethod",
    "signature": "(Ljava/lang/String;)V",
    "migration": "Use com.example.library.NewClass#newMethod() instead",
    "note": "This method will be removed in version 3.0"
  },
  {
    "fqcn": "com.example.library.OldClass",
    "member": "legacyMethod",
    "signature": "(I)V",
    "migration": "Use com.example.library.NewClass#newMethod(int) instead",
    "note": "Overloaded version"
  },
  {
    "fqcn": "com.example.library.Constants",
    "member": "OLD_CONSTANT",
    "signature": null,
    "migration": "Use Constants.NEW_CONSTANT",
    "note": ""
  },
  {
    "fqcn": "com.example.library.DeprecatedClass",
    "member": "<init>",
    "signature": "(Ljava/lang/String;Ljava/lang/String;)V",
    "migration": "Use Builder pattern instead",
    "note": "Constructor with all parameters"
  }
]
```

**フィールド仕様**:
| フィールド | 型 | 必須 | 説明 |
|-----------|-----|------|------|
| `fqcn` | String | ✅ | 完全修飾クラス名（例: `com.example.OldClass`） |
| `member` | String | ✅ | メンバ名。コンストラクタは `<init>` |
| `signature` | String\|null | ❌ | JVM形式のメソッド/コンストラクタシグネチャ。`null` または未指定時は同名の全メンバが対象 |
| `migration` | String | ✅ | 推奨される移行先の説明（Issue表示時に含まれる） |
| `note` | String | ❌ | 補足情報（Issue表示時に括弧内に含まれる） |

**シグネチャ形式（JVM内部形式）**:
- メソッド: `(引数型...)戻り値型`
  - 例: `(Ljava/lang/String;I)V` = String, int を受け取り void を返す
  - 例: `()Ljava/lang/String;` = 引数なし、String を返す
- コンストラクタ: 常に `(引数型...)V`
  - 例: `(Ljava/lang/String;)V` = String を受け取る
- プリミティブ型コード: `I` (int), `J` (long), `Z` (boolean), `F` (float), `D` (double), `V` (void), etc.
- オブジェクト型コード: `Lパッケージ/クラス名;`
  - 例: `Ljava/lang/String;`, `Ljava/util/List;`
- 配列: `[` prefix
  - 例: `[Ljava/lang/String;` = String[]

**実装時の具体的な照合ロジック**:
1. 渡されたAST ノード（メソッド呼び出し、フィールドアクセスなど）から FQCN、メンバ名、シグネチャを抽出
2. 禁止API設定と比較
3. `fqcn` と `member` が一致したら以下を判定：
   - 設定の `signature` が `null` または未指定 → マッチ（同名の全メンバが対象）
   - 設定の `signature` が指定 → ノードのシグネチャと完全一致した場合のみマッチ

### Phase 3: 検査ロジック実装と対応するテスト

#### 3.1 設定パース用モデルクラス
**ファイル**: `src/main/java/net/bis5/sonarqube/custodeprecation/DeprecatedApiConfig.java`

**実装要件**:
```java
public class DeprecatedApiConfig {
    private String fqcn;
    private String member;
    private String signature;  // null 可能
    private String migration;
    private String note;
    
    // Getters
    public String getFqcn() { ... }
    public String getMember() { ... }
    public String getSignature() { ... }
    public String getMigration() { ... }
    public String getNote() { ... }
    
    /**
     * 渡されたAPIと照合
     * @param targetFqcn 検査対象のFQCN
     * @param targetMember 検査対象のメンバ名
     * @param targetSignature 検査対象のシグネチャ（メソッド/コンストラクタ時）
     * @return 設定と一致した場合 true
     */
    public boolean matches(String targetFqcn, String targetMember, String targetSignature) {
        if (!this.fqcn.equals(targetFqcn) || !this.member.equals(targetMember)) {
            return false;
        }
        // シグネチャが指定されていない場合は名前のみでマッチ
        if (this.signature == null || this.signature.isEmpty()) {
            return true;
        }
        // シグネチャが指定されている場合は完全一致を要求
        return this.signature.equals(targetSignature);
    }
    
    /**
     * JSON文字列をパース
     */
    public static List<DeprecatedApiConfig> parseFromJson(String jsonString) 
            throws JsonSyntaxException {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Gson gson = new Gson();
        return gson.fromJson(jsonString, 
            new com.google.gson.reflect.TypeToken<List<DeprecatedApiConfig>>(){}.getType());
    }
}
```

**エージェント検証時の確認項目**:
- JSON パース成功（正常系）
- 不正JSON時に `JsonSyntaxException` をスロー
- `null` / 空文字列時に空リスト返却
- `matches()` メソッドが仕様通りに動作
#### 3.2 検査ロジック実装クラス
**ファイル**: `src/main/java/net/bis5/sonarqube/custodeprecation/CustomDeprecationCheck.java`

**実装要件**:
- `org.sonar.plugins.java.api.IssuableSubscriptionVisitor` を継承
- `@org.sonar.check.Rule` アノテーションで ルールキー指定
- ルールプロパティ `deprecatedApis` を読み込み

**構成要素**:

1. **ルールプロパティ**:
```java
@Rule(key = "CustomDeprecation")
public class CustomDeprecationCheck extends IssuableSubscriptionVisitor {
    
    @org.sonar.check.RuleProperty(
        key = "deprecatedApis",
        name = "Deprecated APIs Configuration",
        description = "JSON array of deprecated API configurations",
        type = "TEXT",
        defaultValue = "[]"
    )
    public String deprecatedApis = "[]";
    
    private List<DeprecatedApiConfig> configs;
    
    @Override
    public void initialize(CheckContext context) {
        // JSON をパース
        try {
            this.configs = DeprecatedApiConfig.parseFromJson(deprecatedApis);
        } catch (Exception e) {
        // ログ出力（Loggerを使用。warn で1回のみ）
            this.configs = Collections.emptyList();
        }
    }
    // ...
}
```

2. **検査対象ノードの指定**:
```java
@Override
public Iterable<Tree.Kind> nodesToVisit() {
    return Arrays.asList(
        Tree.Kind.METHOD_INVOCATION,
        Tree.Kind.NEW_CLASS,
        Tree.Kind.MEMBER_SELECT,
        Tree.Kind.IDENTIFIER
    );
}
```

**重複検知防止のガード**:
- `METHOD_INVOCATION` でメソッド呼び出しはすべて処理する。
- `MEMBER_SELECT` は **フィールドアクセス** のみ処理（`symbol.isVariableSymbol()` で判定）。
  - メソッド参照やメソッド呼び出しの `memberSelect` には **報告しない**。
- `IDENTIFIER` は **静的インポートされたフィールド/定数のみ** を対象にする。
  - `tree.parent()` が `METHOD_INVOCATION` の `methodSelect` に使われている場合は **スキップ**（メソッド呼び出しは `METHOD_INVOCATION` で処理）。
  - `tree.parent()` が `MEMBER_SELECT` の一部である場合も **スキップ**（`MEMBER_SELECT` 側で処理）。

3. **ノード訪問ロジック**:
```java
@Override
public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
        visitMethodInvocation((MethodInvocationTree) tree);
    } else if (tree.is(Tree.Kind.NEW_CLASS)) {
        visitNewClass((NewClassTree) tree);
    } else if (tree.is(Tree.Kind.MEMBER_SELECT)) {
        visitMemberSelect((MemberSelectExpressionTree) tree);
    } else if (tree.is(Tree.Kind.IDENTIFIER)) {
        visitIdentifier((IdentifierTree) tree);
    }
}
```

**詳細な検査フロー**:

**METHOD_INVOCATION の場合**:
```
1. MethodInvocationTree から Symbol を取得
2. Symbol.MethodSymbol を確認
3. owner().type().fullyQualifiedName() で FQCN を取得
4. methodSymbol.name() でメンバ名を取得
5. methodSymbol.signature() でシグネチャを取得（JVM形式）
6. DeprecatedApiConfig.matches() で照合
7. マッチしたら reportIssue() を呼び出し
  - 静的インポートされたメソッド呼び出しもここで検知される
```

**NEW_CLASS の場合**:
```
1. NewClassTree から constructorSymbol() を取得
2. owner().type().fullyQualifiedName() で FQCN を取得
3. member = "<init>" として照合
4. methodSymbol.signature() でシグネチャを取得
5. DeprecatedApiConfig.matches() で照合（member は "<init>"）
6. マッチしたら reportIssue() を呼び出し
```

**MEMBER_SELECT の場合**:
```
1. MemberSelectExpressionTree から expression 部分の型を解決
2. getIdentifier() でメンバ名を取得
3. expression の型（メソッド呼び出し先オブジェクト）の FQCN を取得
4. Symbol API で **フィールドアクセス（VariableSymbol）** か確認
5. フィールドアクセスの場合のみ member = フィールド名 として照合
6. Symbol が解決できない場合は **スキップ**（誤検知防止）
7. マッチしたら reportIssue() を呼び出し
```

**IDENTIFIER の場合（静的インポート）**:
```
1. IdentifierTree の名前を取得
2. Symbol で解決できるか確認（`symbol.isVariableSymbol()` のみ対象）
3. 静的インポートで解決できた場合、`symbol.owner().type().fullyQualifiedName()` を取得
4. member = 識別子名 として照合
5. Symbol が解決できない場合は **スキップ**
6. マッチしたら reportIssue() を呼び出し
```

**エージェント実装時の判定基準**:
- 各ツリーノード種別の検査が期待通りに動作
- Symbol API を正しく使用している
- FQCN、メンバ名、シグネチャが正確に抽出される
- Issue メッセージが仕様通り（migration + note を含む）

#### 3.3 Issue報告メッセージ生成
```java
String message = String.format(
    "This API is deprecated for this project. %s%s",
    config.getMigration(),
    config.getNote() != null && !config.getNote().isEmpty() 
        ? " (" + config.getNote() + ")" 
        : ""
);
reportIssue(tree, message);
```

### Phase 4: テスト実装（TDD に従う）

#### 4.1 テスト用サンプルコード
**ファイル**: `src/test/files/CustomDeprecationCheck.java`

各テストシナリオに対応するコード（後述のテストから参照）

#### 4.2 テストクラス実装順序
**TDDサイクル**の厳密な実践。各テストメソッド追加時：
1. テストメソッドを追加（失敗する）
2. テストをパスさせる最小実装を追加
3. リファクタリング

**テストメソッド一覧**（実装順序）：

| # | テスト | ノードタイプ | 説明 |
|---|--------|-----------|------|
| 1 | `test_method_invocation_deprecated()` | METHOD_INVOCATION | インスタンスメソッド呼び出し検知 |
| 2 | `test_field_access_deprecated()` | MEMBER_SELECT | フィールドアクセス検知 |
| 3 | `test_constructor_call_deprecated()` | NEW_CLASS | コンストラクタ呼び出し検知 |
| 4 | `test_static_import_deprecated()` | IDENTIFIER | 静的インポート検知 |
| 5 | `test_signature_matching_specific()` | METHOD_INVOCATION | シグネチャ指定による特定オーバーロード選択 |
| 6 | `test_signature_unspecified_all_overloads()` | METHOD_INVOCATION | シグネチャ未指定時に全オーバーロード対象 |
| 7 | `test_multiple_deprecated_apis()` | 複合 | 複数禁止API設定の同時動作 |
| 8 | `test_same_name_different_class_not_reported()` | 複合 | 別クラスの同名メンバは検知されない（Symbol解決確認） |
| 9 | `test_inherited_method_deprecated()` | METHOD_INVOCATION | スーパークラスのメソッド呼び出し検知 |
| 10 | `test_empty_config_no_errors()` | 複合 | 空設定でエラーにならない |
| 11 | `test_invalid_json_config_handled()` | 複合 | 不正JSON設定でプラグインが停止しない |

**ファイル**: `src/test/java/net/bis5/sonarqube/custodeprecation/CustomDeprecationCheckTest.java`

**実装例**（テストメソッドの構造）:
```java
@Test
public void test_method_invocation_deprecated() {
    CustomDeprecationCheck check = new CustomDeprecationCheck();
    check.deprecatedApis = "[{\"fqcn\":\"com.example.OldApi\",\"member\":\"oldMethod\"," +
        "\"signature\":null,\"migration\":\"Use NewApi.newMethod()\",\"note\":\"\"}]";
    
    JavaCheckVerifier.verify(
        "src/test/files/CustomDeprecationCheck.java",
        check,
        JavaCheckVerifier.expectIssues()
            .withKey("CustomDeprecation")
            .withMessage("This API is deprecated for this project. Use NewApi.newMethod()")
    );
}
```

**エージェント実装時の検証**:
- `mvn clean test` がすべてパスする
- 各テストが期待通りのIssueを検出する
- 偽陽性（false positive）がない

### Phase 5: リソースファイルと統合

#### 5.1 プラグイン メタデータリソース
**ファイル**: `src/main/resources/org/sonar/l10n/java/rules/custodeprecation/CustomDeprecationCheck.html`

充実した HTML ドキュメント：
- Why is this an issue?
- Configuration
- Noncompliant Code Example
- Compliant Solution

#### 5.2 ビルド検証
```bash
mvn clean package
```

生成物: `target/sonarqube-custom-deprecation-plugin-1.0.0.jar`

確認項目：
- ビルド成功（BUILD SUCCESS）
- エラー/警告がない
- JAR ファイルが生成される

### Phase 6: 動作確認シナリオの実装と検証

動作確認シナリオの実装と検証

エージェントは実装後、以下の8つのシナリオで完全に動作することを自動確認する。テスト実装の一部として含める。

#### 6.1 シナリオ1: 基本的なメソッド呼び出し検知
テストケース（`src/test/files/` 内）:
```java
class Scenario1_BasicMethodCall {
    void test() {
        OldApi api = new OldApi();
        api.oldMethod();  // Noncompliant {{This API is deprecated for this project. Use NewApi.newMethod()}}
        
        NewApi newApi = new NewApi();
        newApi.newMethod();  // Compliant
    }
}
```

テスト設定:
```json
[{
    "fqcn": "com.example.OldApi",
    "member": "oldMethod",
    "signature": null,
    "migration": "Use NewApi.newMethod()",
    "note": ""
}]
```

**エージェント検証**: Issue が1件報告される（`api.oldMethod()` 行）

#### 6.2 シナリオ2: 静的メソッド呼び出し検知
テストケース:
```java
class Scenario2_StaticMethod {
    void test() {
        OldApi.staticMethod();  // Noncompliant {{...}}
        NewApi.staticMethod();  // Compliant
    }
}
```

**エージェント検証**: Issue が1件報告される（`OldApi.staticMethod()` 行）

#### 6.3 シナリオ3: 定数アクセス検知
テストケース:
```java
class Scenario3_FieldAccess {
    void test() {
        int value = Constants.OLD_VALUE;  // Noncompliant {{...}}
        int newValue = Constants.NEW_VALUE;  // Compliant
    }
}
```

テスト設定:
```json
[{
    "fqcn": "com.example.Constants",
    "member": "OLD_VALUE",
    "signature": null,
    "migration": "Use Constants.NEW_VALUE",
    "note": ""
}]
```

**エージェント検証**: Issue が1件報告される（`Constants.OLD_VALUE` 行）

#### 6.4 シナリオ4: コンストラクタ呼び出し検知
テストケース:
```java
class Scenario4_Constructor {
    void test() {
        OldClass obj = new OldClass();  // Noncompliant {{...}}
        NewClass obj2 = new NewClass();  // Compliant
    }
}
```

テスト設定:
```json
[{
    "fqcn": "com.example.OldClass",
    "member": "<init>",
    "signature": null,
    "migration": "Use NewClass instead",
    "note": ""
}]
```

**エージェント検証**: Issue が1件報告される（`new OldClass()` 行）

#### 6.5 シナリオ5: 静的インポート検知
テストケース:
```java
import static com.example.OldApi.oldMethod;

class Scenario5_StaticImport {
    void test() {
        oldMethod();  // Noncompliant {{...}}
    }
}
```

テスト設定: シナリオ1と同じ

**エージェント検証**: Issue が1件報告される（`oldMethod()` 行）

#### 6.6 シナリオ6: 複数禁止API設定
テストケース:
```java
class Scenario6_MultipleApis {
    void test() {
        OldApi api = new OldApi();
        api.method1();  // Noncompliant {{Use NewApi.method1()}}
        api.method2();  // Noncompliant {{Use NewApi.method2()}}
        
        int value = OtherOldApi.OLD_CONST;  // Noncompliant {{Use NEW_CONST}}
    }
}
```

テスト設定:
```json
[{
    "fqcn": "com.example.OldApi",
    "member": "method1",
    "signature": null,
    "migration": "Use NewApi.method1()",
    "note": ""
}, {
    "fqcn": "com.example.OldApi",
    "member": "method2",
    "signature": null,
    "migration": "Use NewApi.method2()",
    "note": ""
}, {
    "fqcn": "com.example.OtherOldApi",
    "member": "OLD_CONST",
    "signature": null,
    "migration": "Use NEW_CONST",
    "note": ""
}]
```

**エージェント検証**: Issue が3件報告される（各行に1件ずつ）

#### 6.7 シナリオ7: オーバーロードメソッドの選択的禁止
テストケース:
```java
class Scenario7_OverloadSignature {
    void test() {
        Api api = new Api();
        api.process("text");       // Noncompliant {{Use processNew(String)}}
        api.process(123);          // Compliant（シグネチャ不一致）
        api.process("a", "b");     // Compliant（シグネチャ不一致）
    }
}
```

テスト設定:
```json
[{
    "fqcn": "com.example.Api",
    "member": "process",
    "signature": "(Ljava/lang/String;)V",
    "migration": "Use processNew(String)",
    "note": ""
}]
```

**エージェント検証**: Issue が1件報告される（最初の `process("text")` 行のみ）

#### 6.8 シナリオ8: シグネチャ未指定時の全オーバーロード対象化
テストケース:
```java
class Scenario8_OverloadAll {
    void test() {
        Api api = new Api();
        api.process("text");       // Noncompliant {{Use processNew()}}
        api.process(123);          // Noncompliant {{Use processNew()}}
        api.process("a", "b");     // Noncompliant {{Use processNew()}}
    }
}
```

テスト設定:
```json
[{
    "fqcn": "com.example.Api",
    "member": "process",
    "signature": null,
    "migration": "Use processNew()",
    "note": ""
}]
```

**エージェント検証**: Issue が3件報告される（全ての `process()` 呼び出し）

#### 6.9 シナリオ8.5: 別クラスの同名メソッドは検知されない
テストケース:
```java
class Scenario8_5_DifferentClass {
    void test() {
        OldApi oldApi = new OldApi();
        oldApi.oldMethod();         // Noncompliant {{...}}
        
        DifferentApi differentApi = new DifferentApi();
        differentApi.oldMethod();   // Compliant（別クラスの同名メソッド）
    }
}
```

テスト設定: `"fqcn": "com.example.OldApi"` のみを対象

**エージェント検証**: Issue が1件報告される（`OldApi.oldMethod()` のみ、`DifferentApi.oldMethod()` は検知されない）

#### 6.10 シナリオ9: 継承したメソッドの呼び出し検知
テストケース:
```java
class ChildClass extends OldApi {
    // OldApi#oldMethod() を継承
}

class Scenario9_Inheritance {
    void test() {
        ChildClass child = new ChildClass();
        child.oldMethod();  // Noncompliant {{...}}（OldApi のメソッド）
    }
}
```

テスト設定: シナリオ1と同じ

**エージェント検証**: Issue が1件報告される（継承元メソッドも検知される）

### Phase 7: 統合テストとビルド

#### 7.1 ユニットテスト実行
```bash
mvn clean test
```

確認項目：
- すべてのテストメソッドがパスする（PASS）
- テストカバレッジが十分（最低80%）
- エラー/警告がない

#### 7.2 ビルドと成果物生成
```bash
mvn clean package
```

生成物: `target/sonarqube-custom-deprecation-plugin-1.0.0.jar`

確認項目：
- ビルド成功（BUILD SUCCESS）
- JARファイルが生成される
- JARファイルサイズが妥当（1MB～10MB程度）

#### 7.3 プラグイン JAR の内容検証
```bash
jar tf target/sonarqube-custom-deprecation-plugin-1.0.0.jar | grep -E "\.class$|\.html$|\.json$"
```

期待する内容：
- `net/bis5/sonarqube/custodeprecation/CustomDeprecationPlugin.class`
- `net/bis5/sonarqube/custodeprecation/CustomDeprecationRulesDefinition.class`
- `net/bis5/sonarqube/custodeprecation/CustomDeprecationCheck.class`
- `net/bis5/sonarqube/custodeprecation/DeprecatedApiConfig.class`
- `org/sonar/l10n/java/rules/custodeprecation/CustomDeprecationCheck.html`
- `org/sonar/l10n/java/rules/custodeprecation/CustomDeprecationCheck.json`

### Phase 8: ドキュメント作成

#### 8.1 README.md 作成
**ファイル**: `README.md`

必須セクション：
1. **プラグイン概要**: 何ができるかを1パラグラフで説明
2. **インストール手順**: ステップバイステップ
3. **使用方法**: JSON設定例を含む
4. **設定例**: 実際の禁止API設定例（3～5個）
5. **トラブルシューティング**: よくある問題と解決策
6. **ライセンス**: ライセンス情報

**エージェント作成時の確認**:
- MD ファイルが有効（ビルドに含まれる）
- コード例が実行可能な形式
- 設定例がコピペで使える

## 実装完了の客観的チェックリスト

以下のすべての項目が確認できた場合、実装は完全と判定される。

### コード構造
- [ ] 5つのJavaクラスが存在する
  - `CustomDeprecationPlugin.java` (pluginクラス)
  - `CustomDeprecationRulesDefinition.java` (ルール定義)
  - `CustomDeprecationCheck.java` (検査ロジック)
  - `DeprecatedApiConfig.java` (設定パース)
  - `CustomDeprecationCheckTest.java` (テストクラス)
- [ ] 2つのリソースファイルが存在する
  - `CustomDeprecationCheck.html`
  - `CustomDeprecationCheck.json`

### テスト実行
- [ ] `mvn clean test` が BUILD SUCCESS
- [ ] 10以上のテストメソッドがすべてPASS
- [ ] テスト結果に FAILURES が0
- [ ] テストログで各シナリオが個別に報告されている

### ビルド
- [ ] `mvn clean package` が BUILD SUCCESS
- [ ] `target/sonarqube-custom-deprecation-plugin-1.0.0.jar` が存在
- [ ] JARファイルのサイズが 1MB～10MB
- [ ] JARが解凍可能（`jar tf` で確認）

### 検査ロジック
- [ ] METHOD_INVOCATION（メソッド呼び出し）を検知
- [ ] NEW_CLASS（コンストラクタ）を検知
- [ ] MEMBER_SELECT（フィールドアクセス）を検知
- [ ] IDENTIFIER（静的インポート）を検知
- [ ] シグネチャ指定時に特定オーバーロードのみ検知
- [ ] シグネチャ未指定時に全オーバーロードを検知
- [ ] 別クラスの同名メンバは検知されない
- [ ] 継承元メソッドも検知される
- [ ] 複数禁止API設定が同時に動作
- [ ] 不正JSON設定でプラグインが停止しない
- [ ] 空設定でエラーにならない

### Issue 報告
- [ ] Issue メッセージが `"This API is deprecated for this project. [migration] ([note])"` フォーマット
- [ ] migration テキストが表示される
- [ ] note テキストが括弧内に表示される（note がある場合）
- [ ] Severity が Minor
- [ ] Issue タイプが CODE_SMELL

### TDD の実践（コミット履歴で確認）
- [ ] テストメソッド追加がそれに対応する実装より先（git log で確認）
- [ ] 少なくとも1回の "Red-Green-Refactor" サイクルが見える
- [ ] テストが失敗 → 実装 → テスト成功、の流れが可視化されている

### ドキュメント
- [ ] README.md が存在する
- [ ] インストール手順が記載されている
- [ ] JSON設定フォーマットの説明がある
- [ ] 実際の使用例が3個以上ある


## 実装完全判定の最終チェックリスト

実装を「完全」と判定するための客観的条件。以下の ALL をエージェント自身が自動検証する。

### 1. ファイル構成（自動検証）
```bash
# チェック方法
[ -f src/main/java/net/bis5/sonarqube/custodeprecation/CustomDeprecationPlugin.java ] && echo "✓" || echo "✗"
[ -f src/main/java/net/bis5/sonarqube/custodeprecation/CustomDeprecationRulesDefinition.java ] && echo "✓" || echo "✗"
[ -f src/main/java/net/bis5/sonarqube/custodeprecation/CustomDeprecationCheck.java ] && echo "✓" || echo "✗"
[ -f src/main/java/net/bis5/sonarqube/custodeprecation/DeprecatedApiConfig.java ] && echo "✓" || echo "✗"
[ -f src/test/java/net/bis5/sonarqube/custodeprecation/CustomDeprecationCheckTest.java ] && echo "✓" || echo "✗"
[ -f src/main/resources/org/sonar/l10n/java/rules/custodeprecation/CustomDeprecationCheck.html ] && echo "✓" || echo "✗"
[ -f src/main/resources/org/sonar/l10n/java/rules/custodeprecation/CustomDeprecationCheck.json ] && echo "✓" || echo "✗"
[ -f README.md ] && echo "✓" || echo "✗"
[ -f pom.xml ] && echo "✓" || echo "✗"
```

### 2. ビルド成功（自動検証）
```bash
cd /workspaces/sonarqube-custom-deprecation-plugin
mvn clean package -q
RESULT=$?
echo "Build result: $RESULT (0 = success)"
[ -f target/sonarqube-custom-deprecation-plugin-1.0.0.jar ] && echo "✓ JAR exists" || echo "✗ JAR missing"
```

### 3. テスト実行（自動検証）
```bash
mvn test -q
TEST_RESULT=$?
echo "Test result: $TEST_RESULT (0 = success)"

# テスト数の確認
TEST_COUNT=$(grep -c "public void test_" src/test/java/net/bis5/sonarqube/custodeprecation/CustomDeprecationCheckTest.java)
echo "Test methods: $TEST_COUNT (minimum 10 required)"
[ $TEST_COUNT -ge 10 ] && echo "✓" || echo "✗"
```

### 4. JAR ファイル検証（自動検証）
```bash
# JARファイル内容確認
JAR_PATH=target/sonarqube-custom-deprecation-plugin-1.0.0.jar

# 必須クラスファイルが含まれているか
jar tf $JAR_PATH | grep -q "CustomDeprecationPlugin.class" && echo "✓ Plugin" || echo "✗"
jar tf $JAR_PATH | grep -q "CustomDeprecationCheck.class" && echo "✓ Check" || echo "✗"
jar tf $JAR_PATH | grep -q "CustomDeprecationRulesDefinition.class" && echo "✓ RulesDefinition" || echo "✗"
jar tf $JAR_PATH | grep -q "DeprecatedApiConfig.class" && echo "✓ Config" || echo "✗"

# 必須リソースが含まれているか
jar tf $JAR_PATH | grep -q "CustomDeprecationCheck.html" && echo "✓ HTML" || echo "✗"
jar tf $JAR_PATH | grep -q "CustomDeprecationCheck.json" && echo "✓ JSON" || echo "✗"
```

### 5. 検査ロジック機能検証（テスト結果から）
テスト実行結果で以下の機能がカバーされていることを確認：
- ✓ Method Invocation Detection
- ✓ Field Access Detection
- ✓ Constructor Call Detection
- ✓ Static Import Detection
- ✓ Overload Method Signature Matching
- ✓ Overload Method No-Signature Matching All
- ✓ Multiple Deprecated APIs
- ✓ Different Class Same Name
- ✓ Inherited Method Detection
- ✓ Empty Configuration Handling
- ✓ Invalid JSON Handling

### 6. Symbol API 使用確認（コード検査）
```bash
# 検査ロジックが Symbol API を使用しているか確認
grep -q "Symbol.MethodSymbol" src/main/java/net/bis5/sonarqube/custodeprecation/CustomDeprecationCheck.java && echo "✓" || echo "✗"
grep -q "\.signature()" src/main/java/net/bis5/sonarqube/custodeprecation/CustomDeprecationCheck.java && echo "✓" || echo "✗"
grep -q "\.fullyQualifiedName()" src/main/java/net/bis5/sonarqube/custodeprecation/CustomDeprecationCheck.java && echo "✓" || echo "✗"
```

### 7. Issue メッセージ形式検証
テストの期待値で以下の形式が確認できること：
```
"This API is deprecated for this project. [migration] ([note])"
```

テストコードで検証：
```bash
grep -q "This API is deprecated for this project" src/test/java/net/bis5/sonarqube/custodeprecation/CustomDeprecationCheckTest.java && echo "✓" || echo "✗"
```

### 8. TDD 実践確認（git log で確認）
```bash
# git ログの確認
git log --oneline | head -20
# 以下のパターンが見えるか確認：
# 1. "test: add test for..." 
# 2. "feat: implement..." 
# 3. "refactor: improve..." 
# のサイクルが複数回見えること
```

### 9. README.md 完成度確認
```bash
# セクション確認
grep -q "# \|## \|### " README.md && echo "✓ Has sections" || echo "✗"
grep -q "Installation" README.md && echo "✓ Has installation" || echo "✗"
grep -q "json\|JSON" README.md && echo "✓ Has config example" || echo "✗"
grep -q "mvn\|maven" README.md && echo "✓ Has build steps" || echo "✗"
```

### 10. 最終動作確認
エージェント実装完了時に以下コマンドを実行し、すべてが成功すること：

```bash
#!/bin/bash
set -e

echo "========================================="
echo "Final Implementation Verification"
echo "========================================="

echo ""
echo "[1] File Structure Check"
FILES=(
  "src/main/java/net/bis5/sonarqube/custodeprecation/CustomDeprecationPlugin.java"
  "src/main/java/net/bis5/sonarqube/custodeprecation/CustomDeprecationRulesDefinition.java"
  "src/main/java/net/bis5/sonarqube/custodeprecation/CustomDeprecationCheck.java"
  "src/main/java/net/bis5/sonarqube/custodeprecation/DeprecatedApiConfig.java"
  "src/test/java/net/bis5/sonarqube/custodeprecation/CustomDeprecationCheckTest.java"
  "src/main/resources/org/sonar/l10n/java/rules/custodeprecation/CustomDeprecationCheck.html"
  "src/main/resources/org/sonar/l10n/java/rules/custodeprecation/CustomDeprecationCheck.json"
  "README.md"
  "pom.xml"
)

for file in "${FILES[@]}"; do
  if [ -f "$file" ]; then
    echo "✓ $file"
  else
    echo "✗ $file MISSING"
    exit 1
  fi
done

echo ""
echo "[2] Build Test"
mvn clean package -q
if [ -f target/sonarqube-custom-deprecation-plugin-1.0.0.jar ]; then
  echo "✓ JAR successfully built"
else
  echo "✗ JAR not found"
  exit 1
fi

echo ""
echo "[3] Unit Test Execution"
mvn test -q
if [ $? -eq 0 ]; then
  echo "✓ All tests passed"
else
  echo "✗ Tests failed"
  exit 1
fi

TEST_COUNT=$(grep -c "public void test_" src/test/java/net/bis5/sonarqube/custodeprecation/CustomDeprecationCheckTest.java)
echo "✓ Test methods: $TEST_COUNT"

echo ""
echo "[4] Code Quality Checks"
if grep -q "Symbol.MethodSymbol" src/main/java/net/bis5/sonarqube/custodeprecation/CustomDeprecationCheck.java; then
  echo "✓ Uses Symbol API"
else
  echo "✗ Symbol API not found"
  exit 1
fi

if grep -q "\.signature()" src/main/java/net/bis5/sonarqube/custodeprecation/CustomDeprecationCheck.java; then
  echo "✓ Uses signature() method"
else
  echo "✗ signature() method not found"
  exit 1
fi

echo ""
echo "[5] Issue Message Format"
if grep -q "This API is deprecated for this project" src/test/java/net/bis5/sonarqube/custodeprecation/CustomDeprecationCheckTest.java; then
  echo "✓ Correct message format in tests"
else
  echo "✗ Message format not found in tests"
  exit 1
fi

echo ""
echo "========================================="
echo "✓ ALL CHECKS PASSED - Implementation Complete"
echo "========================================="
```

## 実装開始時のチェックポイント

エージェント実装開始前に以下を確認：

### 環境確認
- ✓ Java 17 がインストール済み
- ✓ Maven 3.8.1 以上がインストール済み
- ✓ git がインストール済み（TDD実践のため）
- ✓ `/workspaces/sonarqube-custom-deprecation-plugin/` が存在

### 初期 git セットアップ
```bash
cd /workspaces/sonarqube-custom-deprecation-plugin
git init
git config user.email "agent@sonarqube.local"
git config user.name "Implementation Agent"
```

### 実装開始コマンド
```bash
# pom.xml を最初に作成してビルド環境をセットアップ
mvn --version  # 確認用

# 初回ビルド（依存関係ダウンロード）
mvn clean compile
```

---

## 重要な実装上の注意事項

### 1. Symbol API の厳密な使用
SonarJava の Symbol API を使用して FQCNとシグネチャを解決することが必須。
単純な文字列マッチングやAST だけでの判定では不十分。

**理由**: 同名の別クラスのメソッドと混同するのを防ぐ

### 2. シグネチャ形式の正確性
JVM 内部形式（例: `(Ljava/lang/String;I)V`）を正確に扱うこと。
`Symbol.MethodSymbol#signature()` メソッドが返す値と完全に一致する必要がある。

### 3. TDD の厳密な実践
テスト→実装→リファクタリング のサイクルを必ず守ること。
git コミット履歴でこのサイクルが可視化できなければ、TDD 実践とは言えない。

### 4. エラーハンドリング
不正な JSON 設定の場合、プラグイン全体が停止しないように防御的に実装する。
例外をキャッチして、警告ログを出力し、検査を続行すること。

### 5. テストカバレッジ
最低限 10 個のテストメソッドが必要。
各検査対象（メソッド・フィールド・コンストラクタ・静的インポート）、
オーバーロード処理、複数設定、エラーハンドリングを全てカバーすること。

---

## 実装完了時の成果物一覧

以下のファイル/フォルダが存在し、正常に動作すること：

```
sonarqube-custom-deprecation-plugin/
├── pom.xml                           (ビルド設定）
├── README.md                         (ユーザー向けドキュメント)
├── src/
│   ├── main/
│   │   ├── java/net/bis5/sonarqube/custodeprecation/
│   │   │   ├── CustomDeprecationPlugin.java
│   │   │   ├── CustomDeprecationRulesDefinition.java
│   │   │   ├── CustomDeprecationCheck.java
│   │   │   └── DeprecatedApiConfig.java
│   │   └── resources/org/sonar/l10n/java/rules/custodeprecation/
│   │       ├── CustomDeprecationCheck.html
│   │       └── CustomDeprecationCheck.json
│   └── test/
│       ├── java/net/bis5/sonarqube/custodeprecation/
│       │   └── CustomDeprecationCheckTest.java (10+ test methods)
│       └── files/
│           └── CustomDeprecationCheck.java (test scenarios)
├── target/
│   └── sonarqube-custom-deprecation-plugin-1.0.0.jar (最終成果物)
└── .git                              (git リポジトリ)
