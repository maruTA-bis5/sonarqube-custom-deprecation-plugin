/* SPDX-License-Identifier: Apache-2.0 */
package com.example;

class Scenario_CompliantOnly {
    void test() {
        NewApi api = new NewApi();
        api.newMethod();
    }
}

class NewApi {
    void newMethod() {
    }
}
