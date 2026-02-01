/* SPDX-License-Identifier: Apache-2.0 */
package net.bis5.sonarqube.customdeprecation;

import org.sonar.api.Plugin;

/**
 * SonarQube plugin entry point for the Custom Deprecation rule.
 * Registers the rule definition and check implementation for detecting usage of project-specific deprecated APIs.
 */
public class CustomDeprecationPlugin implements Plugin {

    /**
     * Registers the plugin extensions with SonarQube.
     *
     * @param context the plugin context used to register extensions
     */
    @Override
    public void define(Context context) {
        context.addExtension(CustomDeprecationRulesDefinition.class);
        context.addExtension(CustomDeprecationCheck.class);
    }
}
