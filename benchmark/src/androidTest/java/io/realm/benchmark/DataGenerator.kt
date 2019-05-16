/*
 * Copyright 2019 Realm Inc.
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
package io.realm.benchmark

import java.util.ArrayList

class DataGenerator {

    val MAX_AGE: Long = 50
    val MIN_AGE: Long = 20
    val NUM_TEST_NAMES = 1000
    private var employeeNames: MutableList<String> = mutableListOf()

    init {
        employeeNames = ArrayList()
        for (i in 0 until NUM_TEST_NAMES) {
            employeeNames.add("Foo$i")
        }
    }

    fun getEmployeeName(row: Long): String {
        return employeeNames[(row % NUM_TEST_NAMES).toInt()]
    }

    fun getEmployeeAge(row: Long): Long{
        return row % MAX_AGE + MIN_AGE
    }

    fun getEmployeeHiredStatus(row: Long): Boolean {
        return row % 2 == 1L
    }

    fun getEmployeeHiredStatusAsInt(row: Long): Long {
        return row % 2
    }

    fun getHiredBool(row: Long): Boolean {
        return row % 2 != 0L
    }
}
