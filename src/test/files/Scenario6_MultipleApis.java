/* SPDX-License-Identifier: Apache-2.0 */
package com.example;

class Scenario6_MultipleApis {
    void test() {
        OldApi api = new OldApi();
        api.method1(); // Noncompliant {{This API is deprecated for this project. Use NewApi.method1()}}
        api.method2(); // Noncompliant {{This API is deprecated for this project. Use NewApi.method2() (Will be removed soon)}}

        int value = OtherOldApi.OLD_CONST; // Noncompliant {{This API is deprecated for this project. Use NEW_CONST}}
    }
}

class OldApi {
    void method1() {
    }

    void method2() {
    }
}

class OtherOldApi {
    static final int OLD_CONST = 1;
}

class NewApi {
    void method1() {
    }

    void method2() {
    }
}
