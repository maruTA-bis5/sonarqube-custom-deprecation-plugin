/* SPDX-License-Identifier: Apache-2.0 */
package com.example;

class Scenario8_5_DifferentClass {
    void test() {
        OldApi oldApi = new OldApi();
        oldApi.oldMethod(); // Noncompliant {{This API is deprecated for this project. Use NewApi.newMethod()}}

        DifferentApi differentApi = new DifferentApi();
        differentApi.oldMethod();
    }
}

class OldApi {
    void oldMethod() {
    }
}

class DifferentApi {
    void oldMethod() {
    }
}

class NewApi {
    void newMethod() {
    }
}
