/* SPDX-License-Identifier: Apache-2.0 */
package com.example;

import static com.example.OldApi.oldMethod;

class Scenario5_StaticImport {
    void test() {
        oldMethod(); // Noncompliant {{This API is deprecated for this project. Use NewApi.newMethod()}}
    }
}

class OldApi {
    static void oldMethod() {
    }
}

class NewApi {
    static void newMethod() {
    }
}
