/* SPDX-License-Identifier: Apache-2.0 */
package net.bis5.sonarqube.customdeprecation;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;

/**
 * Defines the "Custom Deprecation" rule for detecting usage of project-specific deprecated APIs.
 * Provides the rule metadata, name, description, parameters, and severity.
 */
public class CustomDeprecationRulesDefinition implements RulesDefinition {

    /** The repository key for the custom deprecation rule. */
    public static final String REPOSITORY_KEY = "custodeprecation";

    /** The rule key for custom deprecation checks. */
    public static final String RULE_KEY = "CustomDeprecation";

    /** The language key for Java. */
    public static final String LANGUAGE_KEY = "java";

    /**
     * Defines the rule within the provided context.
     *
     * @param context the rule definition context
     */
    @Override
    public void define(Context context) {
        NewRepository repository = context.createRepository(REPOSITORY_KEY, LANGUAGE_KEY)
            .setName("Custom Deprecation");

        NewRule rule = repository.createRule(RULE_KEY)
            .setName("Custom deprecated API should not be used")
            .setHtmlDescription(getClass().getResource("/org/sonar/l10n/java/rules/custodeprecation/CustomDeprecation.html"))
            .setSeverity("MINOR");

        rule.createParam("deprecatedApis")
            .setName("Deprecated APIs Configuration")
            .setType(RuleParamType.STRING)
            .setDefaultValue("[]")
            .setDescription("JSON array of deprecated API configurations. "
                + "Each entry must contain fqcn, member, optional arguments, migration, and optional note.");

        repository.done();
    }

    /**
     * Gets the rule key for the custom deprecation rule.
     *
     * @return the rule key combining the repository key and rule key
     */
    public static RuleKey ruleKey() {
        return RuleKey.of(REPOSITORY_KEY, RULE_KEY);
    }
}
