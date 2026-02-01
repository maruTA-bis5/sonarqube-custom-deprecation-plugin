/* SPDX-License-Identifier: Apache-2.0 */
package com.example;

class Scenario7_OverloadSignature {
    void test() {
        Api api = new Api();
        api.process("text"); // Noncompliant {{This API is deprecated for this project. Use processNew(String)}}
        api.process(123);
        api.process("a", "b");
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
