/* SPDX-License-Identifier: Apache-2.0 */
package net.bis5.sonarqube.customdeprecation;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

public class CustomDeprecationCheckTest {

    @Test
    public void test_method_invocation_deprecated() {
        CustomDeprecationCheck check = new CustomDeprecationCheck();
        check.deprecatedApis = "[{\"fqcn\":\"com.example.OldApi\",\"member\":\"oldMethod\","
            + "\"arguments\":null,\"migration\":\"Use NewApi.newMethod()\",\"note\":\"\"}]";

        CheckVerifier.newVerifier()
            .onFile("src/test/files/Scenario1_BasicMethodCall.java")
            .withCheck(check)
            .verifyIssues();
    }

    @Test
    public void test_static_method_invocation_deprecated() {
        CustomDeprecationCheck check = new CustomDeprecationCheck();
        check.deprecatedApis = "[{\"fqcn\":\"com.example.OldApi\",\"member\":\"staticMethod\","
            + "\"arguments\":null,\"migration\":\"Use NewApi.staticMethod()\",\"note\":\"\"}]";

        CheckVerifier.newVerifier()
            .onFile("src/test/files/Scenario2_StaticMethod.java")
            .withCheck(check)
            .verifyIssues();
    }

    @Test
    public void test_field_access_deprecated() {
        CustomDeprecationCheck check = new CustomDeprecationCheck();
        check.deprecatedApis = "[{\"fqcn\":\"com.example.Constants\",\"member\":\"OLD_VALUE\","
            + "\"arguments\":null,\"migration\":\"Use Constants.NEW_VALUE\",\"note\":\"\"}]";

        CheckVerifier.newVerifier()
            .onFile("src/test/files/Scenario3_FieldAccess.java")
            .withCheck(check)
            .verifyIssues();
    }

    @Test
    public void test_constructor_call_deprecated() {
        CustomDeprecationCheck check = new CustomDeprecationCheck();
        check.deprecatedApis = "[{\"fqcn\":\"com.example.OldClass\",\"member\":\"<init>\","
            + "\"arguments\":null,\"migration\":\"Use NewClass instead\",\"note\":\"\"}]";

        CheckVerifier.newVerifier()
            .onFile("src/test/files/Scenario4_Constructor.java")
            .withCheck(check)
            .verifyIssues();
    }

    @Test
    public void test_static_import_deprecated() {
        CustomDeprecationCheck check = new CustomDeprecationCheck();
        check.deprecatedApis = "[{\"fqcn\":\"com.example.OldApi\",\"member\":\"oldMethod\","
            + "\"arguments\":null,\"migration\":\"Use NewApi.newMethod()\",\"note\":\"\"}]";

        CheckVerifier.newVerifier()
            .onFile("src/test/files/Scenario5_StaticImport.java")
            .withCheck(check)
            .verifyIssues();
    }

    @Test
    public void test_arguments_matching_specific() {
        CustomDeprecationCheck check = new CustomDeprecationCheck();
        check.deprecatedApis = "[{\"fqcn\":\"com.example.Api\",\"member\":\"process\","
            + "\"arguments\":\"(java.lang.String)\",\"migration\":\"Use processNew(String)\","
            + "\"note\":\"\"}]";

        CheckVerifier.newVerifier()
            .onFile("src/test/files/Scenario7_OverloadSignature.java")
            .withCheck(check)
            .verifyIssues();
    }

    @Test
    public void test_arguments_unspecified_all_overloads() {
        CustomDeprecationCheck check = new CustomDeprecationCheck();
        check.deprecatedApis = "[{\"fqcn\":\"com.example.Api\",\"member\":\"process\","
            + "\"arguments\":null,\"migration\":\"Use processNew()\",\"note\":\"\"}]";

        CheckVerifier.newVerifier()
            .onFile("src/test/files/Scenario8_OverloadAll.java")
            .withCheck(check)
            .verifyIssues();
    }

    @Test
    public void test_multiple_deprecated_apis() {
        CustomDeprecationCheck check = new CustomDeprecationCheck();
        check.deprecatedApis = "["
            + "{\"fqcn\":\"com.example.OldApi\",\"member\":\"method1\",\"arguments\":null,"
            + "\"migration\":\"Use NewApi.method1()\",\"note\":\"\"},"
            + "{\"fqcn\":\"com.example.OldApi\",\"member\":\"method2\",\"arguments\":null,"
            + "\"migration\":\"Use NewApi.method2()\",\"note\":\"Will be removed soon\"},"
            + "{\"fqcn\":\"com.example.OtherOldApi\",\"member\":\"OLD_CONST\",\"arguments\":null,"
            + "\"migration\":\"Use NEW_CONST\",\"note\":\"\"}"
            + "]";

        CheckVerifier.newVerifier()
            .onFile("src/test/files/Scenario6_MultipleApis.java")
            .withCheck(check)
            .verifyIssues();
    }

    @Test
    public void test_same_name_different_class_not_reported() {
        CustomDeprecationCheck check = new CustomDeprecationCheck();
        check.deprecatedApis = "[{\"fqcn\":\"com.example.OldApi\",\"member\":\"oldMethod\","
            + "\"arguments\":null,\"migration\":\"Use NewApi.newMethod()\",\"note\":\"\"}]";

        CheckVerifier.newVerifier()
            .onFile("src/test/files/Scenario8_5_DifferentClass.java")
            .withCheck(check)
            .verifyIssues();
    }

    @Test
    public void test_inherited_method_deprecated() {
        CustomDeprecationCheck check = new CustomDeprecationCheck();
        check.deprecatedApis = "[{\"fqcn\":\"com.example.OldApi\",\"member\":\"oldMethod\","
            + "\"arguments\":null,\"migration\":\"Use NewApi.newMethod()\",\"note\":\"\"}]";

        CheckVerifier.newVerifier()
            .onFile("src/test/files/Scenario9_Inheritance.java")
            .withCheck(check)
            .verifyIssues();
    }

    @Test
    public void test_migration_targets_not_flagged() {
        CustomDeprecationCheck check = new CustomDeprecationCheck();
        check.deprecatedApis = "["
            + "{\"fqcn\":\"com.example.TargetObj\",\"member\":\"<init>\","
            + "\"arguments\":\"()\",\"migration\":\"Use TargetObj(Object, String)\",\"note\":\"\"},"
            + "{\"fqcn\":\"com.example.TargetObj\",\"member\":\"targetMethod\","
            + "\"arguments\":\"(java.lang.String)\","
            + "\"migration\":\"Use targetMethod(String, String)\",\"note\":\"\"}"
            + "]";

        CheckVerifier.newVerifier()
            .onFile("src/test/files/Scenario10_MigrationTargets.java")
            .withCheck(check)
            .verifyIssues();
    }

    @Test
    public void test_arguments_empty_only_matches_no_arg_overload() {
        CustomDeprecationCheck check = new CustomDeprecationCheck();
        check.deprecatedApis = "["
            + "{\"fqcn\":\"com.example.Overload\",\"member\":\"<init>\","
            + "\"arguments\":\"()\",\"migration\":\"Use Overload(String)\",\"note\":\"\"},"
            + "{\"fqcn\":\"com.example.Overload\",\"member\":\"noArg\","
            + "\"arguments\":\"()\",\"migration\":\"Use noArg(String)\",\"note\":\"\"}"
            + "]";

        CheckVerifier.newVerifier()
            .onFile("src/test/files/Scenario11_ArgumentsEmptyVsNull.java")
            .withCheck(check)
            .verifyIssues();
    }

    @Test
    public void test_empty_config_no_errors() {
        CustomDeprecationCheck check = new CustomDeprecationCheck();
        check.deprecatedApis = "[]";

        CheckVerifier.newVerifier()
            .onFile("src/test/files/Scenario_CompliantOnly.java")
            .withCheck(check)
            .verifyNoIssues();
    }

    @Test
    public void test_invalid_json_config_handled() {
        CustomDeprecationCheck check = new CustomDeprecationCheck();
        check.deprecatedApis = "{ invalid json";

        CheckVerifier.newVerifier()
            .onFile("src/test/files/Scenario_CompliantOnly.java")
            .withCheck(check)
            .verifyNoIssues();
    }
}
