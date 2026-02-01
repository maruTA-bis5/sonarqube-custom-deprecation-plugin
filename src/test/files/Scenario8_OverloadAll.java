/* SPDX-License-Identifier: Apache-2.0 */
package com.example;

class Scenario8_OverloadAll {
    void test() {
        Api api = new Api();
        api.process("text"); // Noncompliant {{This API is deprecated for this project. Use processNew()}}
        api.process(123); // Noncompliant {{This API is deprecated for this project. Use processNew()}}
        api.process("a", "b"); // Noncompliant {{This API is deprecated for this project. Use processNew()}}
    }
}

class Api {
    void process(String value) {
    }

    void process(int value) {
    }

    void process(String a, String b) {
    }
}
