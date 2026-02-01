/* SPDX-License-Identifier: Apache-2.0 */
package com.example;

class Scenario2_StaticMethod {
    void test() {
        OldApi.staticMethod(); // Noncompliant {{This API is deprecated for this project. Use NewApi.staticMethod()}}
        NewApi.staticMethod();
    }
}

class OldApi {
    static void staticMethod() {
    }
}

class NewApi {
    static void staticMethod() {
    }
}
