/*
 * Copyright 2016 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm.datastorebenchmark;

import java.util.ArrayList;
import java.util.List;

public class DataGenerator {
    public static final int MAX_AGE = 50;
    public static final int MIN_AGE = 20;
    public static final int NUM_TEST_NAMES = 1000;

    private List<String> employeeNames = null;

    public void initNames() {
        employeeNames = new ArrayList<String>();
        for (int i = 0; i < NUM_TEST_NAMES; i++) {
            employeeNames.add("Foo" + i);
        }
    }

    public String getEmployeeName(int row) {
        return employeeNames.get(row % NUM_TEST_NAMES);
    }

    public int getEmployeeAge(int row) {
        return row % MAX_AGE + MIN_AGE;
    }

    public boolean getEmployeeHiredStatus(int row) {
        return (row % 2) == 1 ? true : false;
    }

    public int getEmployeeHiredStatusAsInt(int row) {
        return (row % 2);
    }

    public boolean getHiredBool(int row) {
        if (row % 2 == 0) return false;
        return true;
    }

}
