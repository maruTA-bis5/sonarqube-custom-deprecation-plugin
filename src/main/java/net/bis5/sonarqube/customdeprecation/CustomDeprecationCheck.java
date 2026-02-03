/* SPDX-License-Identifier: Apache-2.0 */
package net.bis5.sonarqube.customdeprecation;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * SonarQube Java check for detecting usage of project-specific deprecated APIs.
 * Identifies method calls, field accesses, constructor calls, and static imports
 * that match the configured deprecated API list and reports them as issues.
 */
@Rule(key = CustomDeprecationRulesDefinition.RULE_KEY)
public class CustomDeprecationCheck extends IssuableSubscriptionVisitor {

    private static final Logger LOG = Loggers.get(CustomDeprecationCheck.class);

    /**
     * JSON array configuration of deprecated APIs to detect.
     * Each element must contain fqcn, member, optional arguments, migration, and optional note fields.
     * Default is an empty array, meaning no APIs are marked as deprecated.
     */
    @RuleProperty(
        key = "deprecatedApis",
        description = "JSON array of deprecated API configurations. Each element must have: fqcn (fully qualified class name), member (method/field name or '<init>' for constructors), arguments (optional source-style list), migration (recommended action), note (optional context)",
        type = "TEXT",
        defaultValue = "[]"
    )
    public String deprecatedApis = "[]";

    private List<DeprecatedApiConfig> configs = Collections.emptyList();

    @Override
    public void setContext(JavaFileScannerContext context) {
        super.setContext(context);
        try {
            this.configs = DeprecatedApiConfig.parseFromJson(deprecatedApis);
        } catch (Exception e) {
            LOG.warn("Failed to parse deprecatedApis configuration. The rule will run with an empty configuration.", e);
            this.configs = Collections.emptyList();
        }
    }

    @Override
    public List<Tree.Kind> nodesToVisit() {
        return Arrays.asList(
            Tree.Kind.METHOD_INVOCATION,
            Tree.Kind.NEW_CLASS,
            Tree.Kind.MEMBER_SELECT,
            Tree.Kind.IDENTIFIER
        );
    }

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

    private void visitMethodInvocation(MethodInvocationTree tree) {
        Symbol.MethodSymbol methodSymbol = tree.methodSymbol();
        if (methodSymbol == null) {
            return;
        }
        String fqcn = fullyQualifiedName(methodSymbol.owner());
        String member = methodSymbol.name();
        String arguments = argumentsFromMethodSymbol(methodSymbol);
        checkAndReport(tree, fqcn, member, arguments);
    }

    private void visitNewClass(NewClassTree tree) {
        Symbol.MethodSymbol constructorSymbol = tree.methodSymbol();
        if (constructorSymbol == null) {
            return;
        }
        String fqcn = fullyQualifiedName(constructorSymbol.owner());
        String member = "<init>";
        String arguments = argumentsFromConstructorSymbol(constructorSymbol);
        checkAndReport(tree, fqcn, member, arguments);
    }

    private void visitMemberSelect(MemberSelectExpressionTree tree) {
        Symbol symbol = tree.identifier().symbol();
        if (symbol == null || !symbol.isVariableSymbol()) {
            return;
        }
        String fqcn = fullyQualifiedName(symbol.owner());
        String member = symbol.name();
        checkAndReport(tree, fqcn, member, null);
    }

    private void visitIdentifier(IdentifierTree tree) {
        if (isPartOfMethodInvocation(tree) || isPartOfMemberSelect(tree)) {
            return;
        }
        Symbol symbol = tree.symbol();
        if (symbol == null || !symbol.isVariableSymbol() || !symbol.isStatic()) {
            return;
        }
        if (isVariableDeclarationIdentifier(tree)) {
            return;
        }
        if (symbol.owner() == null || !symbol.owner().isTypeSymbol()) {
            return;
        }
        String fqcn = fullyQualifiedName(symbol.owner());
        String member = symbol.name();
        checkAndReport(tree, fqcn, member, null);
    }

    private boolean isPartOfMethodInvocation(IdentifierTree tree) {
        Tree parent = tree.parent();
        if (parent instanceof MethodInvocationTree invocationTree) {
            return invocationTree.methodSelect() == tree;
        }
        return false;
    }

    private boolean isPartOfMemberSelect(IdentifierTree tree) {
        Tree parent = tree.parent();
        return parent instanceof MemberSelectExpressionTree;
    }

    private boolean isVariableDeclarationIdentifier(IdentifierTree tree) {
        Tree parent = tree.parent();
        if (parent instanceof org.sonar.plugins.java.api.tree.VariableTree variableTree) {
            return variableTree.simpleName() == tree;
        }
        return false;
    }

    private void checkAndReport(Tree tree, String fqcn, String member, String arguments) {
        if (configs == null || configs.isEmpty()) {
            return;
        }
        Optional<DeprecatedApiConfig> matched = configs.stream()
            .filter(config -> config.matches(fqcn, member, arguments))
            .findFirst();

        if (matched.isPresent()) {
            DeprecatedApiConfig config = matched.get();
            String message = buildMessage(config);
            reportIssue(tree, message);
        }
    }

    private String buildMessage(DeprecatedApiConfig config) {
        String migration = config.getMigration() == null ? "" : config.getMigration();
        String note = config.getNote();
        String suffix = (note != null && !note.isEmpty()) ? " (" + note + ")" : "";
        return "This API is deprecated for this project. " + migration + suffix;
    }

    private static String fullyQualifiedName(Symbol symbol) {
        if (symbol == null || symbol.type() == null) {
            return null;
        }
        return symbol.type().fullyQualifiedName();
    }

    private static String argumentsFromMethodSymbol(Symbol.MethodSymbol methodSymbol) {
        return argumentsFromTypes(methodSymbol.parameterTypes());
    }

    private static String argumentsFromConstructorSymbol(Symbol.MethodSymbol methodSymbol) {
        return argumentsFromTypes(methodSymbol.parameterTypes());
    }

    private static String argumentsFromTypes(List<Type> types) {
        if (types == null || types.isEmpty()) {
            return "()";
        }
        String params = types.stream()
            .map(CustomDeprecationCheck::argumentTypeName)
            .collect(Collectors.joining(","));
        return "(" + params + ")";
    }

    private static String argumentTypeName(Type type) {
        if (type == null || type.isUnknown()) {
            return "java.lang.Object";
        }
        String fqn = type.fullyQualifiedName();
        if (fqn == null || fqn.isEmpty()) {
            fqn = type.name();
        }
        if (fqn == null || fqn.isEmpty()) {
            return "java.lang.Object";
        }
        return fqn;
    }
}
