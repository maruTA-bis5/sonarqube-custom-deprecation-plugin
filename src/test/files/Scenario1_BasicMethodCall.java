/* SPDX-License-Identifier: Apache-2.0 */
package com.example;

class Scenario1_BasicMethodCall {
    void test() {
        OldApi api = new OldApi();
        api.oldMethod(); // Noncompliant {{This API is deprecated for this project. Use NewApi.newMethod()}}

        NewApi newApi = new NewApi();
        newApi.newMethod();
    }
}

class OldApi {
    void oldMethod() {
    }
}

class NewApi {
    void newMethod() {
    }
}
