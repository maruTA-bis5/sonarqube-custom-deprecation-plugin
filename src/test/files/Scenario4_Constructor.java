/* SPDX-License-Identifier: Apache-2.0 */
package com.example;

class Scenario4_Constructor {
    void test() {
        OldClass obj = new OldClass(); // Noncompliant {{This API is deprecated for this project. Use NewClass instead}}
        NewClass obj2 = new NewClass();
    }
}

class OldClass {
    OldClass() {
    }
}

class NewClass {
    NewClass() {
    }
}
