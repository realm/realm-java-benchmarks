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
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import io.realm.benchmark.room.AppDatabase
import io.realm.benchmark.room.Employee
import io.realm.benchmark.room.EmployeeDao
import java.io.File


class RoomBenchmarks(size: Long): Benchmarks(size) {

    private lateinit var db: AppDatabase
    private lateinit var dao: EmployeeDao

    override fun before() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbName = "benchmark.room"
        File(context.applicationInfo.dataDir + "/databases/$dbName").delete()
        db = Room.databaseBuilder(context, AppDatabase::class.java,"benchmark.room").build()
        dao = db.employeeDao()
    }

    override fun after() {
        db.close()
    }

    private fun addObjects() {
        val list: MutableList<Employee> = mutableListOf()
        for (i in 0 until size) {
            list.add(Employee(dataGenerator.getEmployeeName(i),
                    dataGenerator.getEmployeeAge(i),
                    dataGenerator.getHiredBool(i)))
        }
        dao.bulkWrite(list)
    }

    private fun deleteObjects() {
        db.clearAllTables()
    }

    override fun simpleQuery() {
        benchmarkRule.measureRepeated {
            runWithTimingDisabled { addObjects() }
            dao.simpleQuery().forEach {
                // Read a single property to actually exercise the query and try to normalize
                // the workload across libraries (as queries/reading data behaves quite differently)
                @Suppress("UNUSED_VARIABLE")
                val tmp = it.id
            }
            runWithTimingDisabled { deleteObjects() }
        }
    }

    override fun simpleWrite() {
        var i: Long = 0
        benchmarkRule.measureRepeated {
            runWithTimingDisabled { deleteObjects() }
            val obj = Employee(dataGenerator.getEmployeeName(i),
                dataGenerator.getEmployeeAge(i),
                dataGenerator.getHiredBool(i)
            )
            dao.write(obj)
            i++
        }
    }

    override fun batchWrite() {
        val list: MutableList<Employee> = mutableListOf()
        for (i in 0 until size) {
            val obj = Employee(dataGenerator.getEmployeeName(i),
                    dataGenerator.getEmployeeAge(i),
                    dataGenerator.getHiredBool(i))
            list.add(obj)
        }
        benchmarkRule.measureRepeated {
            runWithTimingDisabled { deleteObjects() }
            dao.bulkWrite(list)
        }
    }

    override fun fullScan() {
        benchmarkRule.measureRepeated {
            runWithTimingDisabled { addObjects() }
            @Suppress("UNUSED_VARIABLE")
            val list = dao.fullScan()
            runWithTimingDisabled { deleteObjects() }
        }
    }

    override fun delete() {
        benchmarkRule.measureRepeated {
            runWithTimingDisabled { addObjects() }
            dao.deleteAll()
        }
    }

    override fun sum() {
        benchmarkRule.measureRepeated {
            runWithTimingDisabled { addObjects() }
            @Suppress("UNUSED_VARIABLE")
            val sum = dao.sum()
            runWithTimingDisabled { deleteObjects() }
        }
    }

    override fun count() {
        benchmarkRule.measureRepeated {
            runWithTimingDisabled { addObjects() }
            @Suppress("UNUSED_VARIABLE")
            val count= dao.count()
            runWithTimingDisabled { deleteObjects() }
        }
    }

}