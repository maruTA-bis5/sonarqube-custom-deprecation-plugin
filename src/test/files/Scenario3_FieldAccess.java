/* SPDX-License-Identifier: Apache-2.0 */
package com.example;

class Scenario3_FieldAccess {
    void test() {
        int value = Constants.OLD_VALUE; // Noncompliant {{This API is deprecated for this project. Use Constants.NEW_VALUE}}
        int newValue = Constants.NEW_VALUE;
    }
}

class Constants {
    static final int OLD_VALUE = 1;
    static final int NEW_VALUE = 2;
}
