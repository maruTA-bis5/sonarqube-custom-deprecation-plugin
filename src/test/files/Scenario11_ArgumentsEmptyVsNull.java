/* SPDX-License-Identifier: Apache-2.0 */
package com.example;

class Scenario11_ArgumentsEmptyVsNull {
    void test() {
        Overload obj = new Overload(); // Noncompliant {{This API is deprecated for this project. Use Overload(String)}}
        Overload obj2 = new Overload("value");

        obj.noArg(); // Noncompliant {{This API is deprecated for this project. Use noArg(String)}}
        obj.noArg("value");
    }
}

class Overload {
    Overload() {
    }

    Overload(String value) {
    }

    void noArg() {
    }

    void noArg(String value) {
    }
}
