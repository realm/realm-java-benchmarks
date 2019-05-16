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

import androidx.benchmark.measureRepeated
import androidx.test.platform.app.InstrumentationRegistry
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.benchmark.realm.Employee
import io.realm.kotlin.createObject
import io.realm.kotlin.where


class RealmBenchmarks(size: Long): Benchmarks(size) {

    private lateinit var config: RealmConfiguration
    private lateinit var realm: Realm

    override fun before() {
        Realm.init(InstrumentationRegistry.getInstrumentation().targetContext)
        this.config = RealmConfiguration.Builder().name("benchmark.realm").build()
        Realm.deleteRealm(config)
        realm = Realm.getInstance(config)
    }

    override fun after() {
        realm.close()
        Realm.deleteRealm(config)
    }

    private fun addObjects(numberOfObjects: Long) {
        realm.executeTransaction {
            for (i in 0 until numberOfObjects) {
                val employee = realm.createObject<Employee>(i)
                employee.name = dataGenerator.getEmployeeName(i)
                employee.age = dataGenerator.getEmployeeAge(i)
                employee.hired = dataGenerator.getHiredBool(i)
            }
        }
    }

    private fun deleteObjects() {
        realm.executeTransaction {
            it.deleteAll()
        }
    }

    override fun simpleQuery() {
        benchmarkRule.measureRepeated {
            runWithTimingDisabled { addObjects(size) }
            // Run query
            val employees = realm.where<Employee>()
                    .equalTo("hired", false)
                    .between("age", 20, 50)
                    .equalTo("name", "Foo0")
                    .findAll()

            // Read a single property to actually exercise the query and try to normalize
            // the workload across libraries (as queries/reading data behaves quite differently)
            for (employee in employees) {
                @Suppress("UNUSED_VARIABLE")
                val tmp = employee.id
            }
            runWithTimingDisabled { deleteObjects() }
        }
    }

    override fun simpleWrite() {
        var i: Long = 0
        benchmarkRule.measureRepeated {
            runWithTimingDisabled { deleteObjects() }
            realm.executeTransaction {
                val obj = it.createObject<Employee>(i)
                obj.name = dataGenerator.getEmployeeName(i)
                obj.hired = dataGenerator.getHiredBool(i)
                obj.age = dataGenerator.getEmployeeAge(i)
                i++
            }
        }
    }

    override fun batchWrite() {
        val list: MutableList<Employee> = mutableListOf()
        for (i in 0 until size) {
            val obj = Employee()
            obj.id = i
            obj.name = dataGenerator.getEmployeeName(i)
            obj.hired = dataGenerator.getHiredBool(i)
            obj.age = dataGenerator.getEmployeeAge(i)
            list.add(obj)
        }

        benchmarkRule.measureRepeated {
            runWithTimingDisabled { realm.executeTransaction { it.deleteAll() } }
            realm.executeTransaction {
                it.insert(list)
            }
        }
    }

    override fun fullScan() {
        addObjects(size)
        benchmarkRule.measureRepeated {
            val result= realm.where<Employee>()
                    .equalTo("hired", true)
                    .between("age", -2, -1)
                    .equalTo("name", "Smile1").findAll()
            @Suppress("UNUSED_VARIABLE")
            val count = result.size
        }
    }

    override fun delete() {
        deleteObjects()
        benchmarkRule.measureRepeated {
            runWithTimingDisabled { addObjects(size) }
            realm.executeTransaction {
                it.delete(Employee::class.java)
            }
        }
    }

    override fun sum() {
        benchmarkRule.measureRepeated {
            runWithTimingDisabled { addObjects(size) }
            @Suppress("UNUSED_VARIABLE")
            val sum = realm.where<Employee>().sum("age")
            runWithTimingDisabled { deleteObjects() }
        }
    }

    override fun count() {
        benchmarkRule.measureRepeated {
            runWithTimingDisabled { addObjects(size) }
            @Suppress("UNUSED_VARIABLE")
            val count= realm.where<Employee>().count()
            runWithTimingDisabled { deleteObjects() }
        }
    }

}
