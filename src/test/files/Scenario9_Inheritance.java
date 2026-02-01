/* SPDX-License-Identifier: Apache-2.0 */
package com.example;

class Scenario9_Inheritance {
    void test() {
        ChildClass child = new ChildClass();
        child.oldMethod(); // Noncompliant {{This API is deprecated for this project. Use NewApi.newMethod()}}
    }
}

class OldApi {
    void oldMethod() {
    }
}

class ChildClass extends OldApi {
}

class NewApi {
    void newMethod() {
    }
}
