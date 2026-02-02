/* SPDX-License-Identifier: Apache-2.0 */
package com.example;

class Scenario10_MigrationTargets {
    void test() {
        TargetObj obj = new TargetObj(new Object(), "value");

        obj.targetMethod("value"); // Noncompliant {{This API is deprecated for this project. Use targetMethod(String, String)}}
        obj.targetMethod("value", "value2");

        new TargetObj(); // Noncompliant {{This API is deprecated for this project. Use TargetObj(Object, String)}}
        new TargetObj(new Object(), "value");
    }
}

class TargetObj {
    TargetObj() {
    }

    TargetObj(Object obj, String value) {
    }

    void targetMethod(String value) {
    }

    void targetMethod(String value, String value2) {
    }
}
