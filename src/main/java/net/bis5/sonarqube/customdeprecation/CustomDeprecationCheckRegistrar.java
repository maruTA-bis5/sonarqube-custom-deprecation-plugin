/* SPDX-License-Identifier: Apache-2.0 */
package net.bis5.sonarqube.customdeprecation;

import java.util.Collections;
import java.util.List;
import org.sonar.plugins.java.api.CheckRegistrar;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonarsource.api.sonarlint.SonarLintSide;

/**
 * Provides the "checks" (implementations of rules) classes that are going to be executed during
 * source code analysis.
 *
 * This class is a batch extension by implementing the {@link CheckRegistrar} interface.
 */
@SonarLintSide
public class CustomDeprecationCheckRegistrar implements CheckRegistrar {

    /**
     * Register the classes that will be used to instantiate checks during analysis.
     */
    @Override
    public void register(RegistrarContext registrarContext) {
        // Call to registerClassesForRepository to associate the classes with the correct repository key
        registrarContext.registerClassesForRepository(
            CustomDeprecationRulesDefinition.REPOSITORY_KEY,
            checkClasses(),
            Collections.emptyList());
    }

    /**
     * Lists all the main checks provided by the plugin
     *
     * @return list of check classes registered by this plugin
     */
    public static List<Class<? extends JavaCheck>> checkClasses() {
        return Collections.singletonList(CustomDeprecationCheck.class);
    }
}
