# SonarQube Custom Deprecation Plugin - AI Coding Agent Instructions

## Project Overview
This is a SonarQube plugin that detects usage of project-specific deprecated APIs in Java code through JSON configuration. It extends SonarQube's Java analyzer to report custom deprecations not marked with `@Deprecated`.

**Core Architecture:**
- **Plugin Entry**: [CustomDeprecationPlugin.java](src/main/java/net/bis5/sonarqube/customdeprecation/CustomDeprecationPlugin.java) - registers extensions with SonarQube
- **Check Registrar**: [CustomDeprecationCheckRegistrar.java](src/main/java/net/bis5/sonarqube/customdeprecation/CustomDeprecationCheckRegistrar.java) - implements CheckRegistrar interface to register rule checks
- **Rule Definition**: [CustomDeprecationRulesDefinition.java](src/main/java/net/bis5/sonarqube/customdeprecation/CustomDeprecationRulesDefinition.java) - defines rule metadata (key: `custodeprecation:CustomDeprecation`)
- **AST Visitor**: [CustomDeprecationCheck.java](src/main/java/net/bis5/sonarqube/customdeprecation/CustomDeprecationCheck.java) - traverses Java AST using SonarQube's `IssuableSubscriptionVisitor`
- **Config Model**: [DeprecatedApiConfig.java](src/main/java/net/bis5/sonarqube/customdeprecation/DeprecatedApiConfig.java) - parses JSON configuration with Gson

## Critical Implementation Details

### AST Traversal Pattern
The check visits 4 tree node kinds: `METHOD_INVOCATION`, `NEW_CLASS`, `MEMBER_SELECT`, `IDENTIFIER`. Each visitor extracts FQCN, member name, and arguments, then checks against parsed configs:
```java
// Method calls: extracts fqcn from symbol.owner(), member from methodSymbol.name()
// Constructors: uses member = "<init>" (special marker for constructors)
// Fields: MEMBER_SELECT for qualified access (e.g., Constants.OLD_VALUE)
// Static imports: IDENTIFIER nodes (must check isStatic() and filter out declarations)
```

### Argument Matching Convention
Arguments are formatted in **source-style notation** (not descriptor format):
- No args: `"()"`
- Single arg: `"(java.lang.String)"`
- Multiple args: `"(java.lang.String,int)"`
- Arrays/varargs: `"(java.lang.String[])"`
- `null` or empty string in config → matches ALL overloads

See `argumentsFromTypes()` method - handles unknown types by falling back to `java.lang.Object`.

### Test Structure
Tests in [CustomDeprecationCheckTest.java](src/test/java/net/bis5/sonarqube/customdeprecation/CustomDeprecationCheckTest.java) use SonarQube's `CheckVerifier` framework:
```java
CheckVerifier.newVerifier()
    .onFile("src/test/files/ScenarioX_Description.java")
    .withCheck(check)  // check with deprecatedApis JSON configured
    .verifyIssues();   // validates // Noncompliant comments in test files
```

Test files in [src/test/files/](src/test/files/) use `// Noncompliant {{expected message}}` comments to mark expected violations.

**Scenario Coverage**: Basic calls (1), static methods (2), fields (3), constructors (4), static imports (5), multiple configs (6), signature matching (7), all overloads (8), inheritance (9), argument edge cases (11).

## Development Workflows

### Build & Test
```bash
mvn clean package    # Builds plugin JAR to target/
mvn test            # Runs JUnit tests with CheckVerifier
```

### License Headers
All Java files require Apache 2.0 SPDX headers:
```bash
mvn license:update-file-header  # Auto-applies LICENSE_HEADER.txt
```
Headers apply to `src/main/java/**`, `src/test/java/**`, `src/test/files/**`.

### Plugin Packaging
Uses `sonar-packaging-maven-plugin` with:
- `pluginKey`: `custodeprecation`
- `pluginClass`: `net.bis5.sonarqube.customdeprecation.CustomDeprecationPlugin`
- Requires `sonar-java` plugin
- Shades Gson dependency (other deps are `provided` scope)

### Release Process
Activate `release` profile for GPG signing:
```bash
mvn clean deploy -Prelease
```
Uses `central-publishing-maven-plugin` for Maven Central deployment.

## Key Constraints & Gotchas

1. **Symbol Resolution**: Check always validates `symbol != null` - malformed code may return null symbols. Log warnings but never crash.

2. **Identifier Filtering**: `visitIdentifier()` must exclude:
   - Method invocation identifiers (handled by `METHOD_INVOCATION`)
   - Member select sub-expressions (handled by `MEMBER_SELECT`)
   - Variable declaration names (e.g., `int OLD_VALUE = 5;` should not trigger)

3. **Config Parsing Errors**: If JSON is invalid, `setContext()` logs warning and runs with empty config (fail-safe, not fail-fast).

4. **Inheritance Handling**: Deprecated method in superclass triggers when called on subclass instance (see Scenario9) - relies on SonarQube's semantic model.

5. **Resource Files**: Rule metadata lives in `src/main/resources/org/sonar/l10n/java/rules/custodeprecation/`:
   - `CustomDeprecation.json` - defines severity, type, tags
   - `CustomDeprecation.html` - HTML description for SonarQube UI (file name must match RULE_KEY)

## Dependencies
- **SonarQube API**: 9.14.0.375 (provided)
- **SonarJava Plugin**: 7.16.0.30901 (provided) - provides AST, semantic model, and `IssuableSubscriptionVisitor`
- **Gson**: 2.13.2 (shaded) - only external dependency packaged in plugin
- **Java**: Requires Java 17 (source/target)

## Adding New Detection Scenarios

1. Add test file to `src/test/files/ScenarioX_Description.java` with `// Noncompliant` markers
2. Add test method to `CustomDeprecationCheckTest` with appropriate JSON config
3. If new tree node kind needed, add to `nodesToVisit()` and implement visitor
4. Update README.md examples if introducing new config patterns

## Common Pitfalls
- Don't use `Tree.parent()` excessively - expensive operation, cache results
- Always check `symbol.owner() != null` before calling `symbol.owner().type()`
- Test both qualified (`Constants.OLD_VALUE`) and static import (`OLD_VALUE`) access patterns
- Remember constructors use `<init>` as member name, not the class name
