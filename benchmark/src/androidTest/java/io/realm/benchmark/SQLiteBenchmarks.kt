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

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.benchmark.measureRepeated
import androidx.test.platform.app.InstrumentationRegistry

class SQLiteBenchmarks(size: Long): Benchmarks(size) {

    class SQLiteDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "sqlite.db", null, 1) {
        val TABLE_SIMPLE = "Simple"

        override fun onCreate(db: SQLiteDatabase) {
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            onCreate(db)
        }
    }

    private lateinit var db: SQLiteDatabase
    private lateinit var dbHelper: SQLiteDatabaseHelper

    override fun before() {
        dbHelper = SQLiteDatabaseHelper(InstrumentationRegistry.getInstrumentation().targetContext)
        db = dbHelper.writableDatabase
        addSimpleTable()
    }

    override fun after() {
        db.close()
        dbHelper.close()
    }

    private fun addSimpleTable() {
        db.execSQL("DROP TABLE IF EXISTS " + dbHelper.TABLE_SIMPLE)
        db.execSQL("CREATE TABLE " + dbHelper.TABLE_SIMPLE + " ("
                + "id INTEGER PRIMARY_KEY, "
                + "name TEXT,"
                + "age INTEGER,"
                + "hired INTEGER"
                + ")")
    }

    private fun addRows() {
        val stmt = db.compileStatement("INSERT INTO " + dbHelper.TABLE_SIMPLE + " VALUES(?1, ?2, ?3, ?4)")
        db.beginTransaction()
        for (i in 0 until size) {
            stmt.clearBindings()
            stmt.bindLong(1, i)
            stmt.bindString(2, dataGenerator.getEmployeeName(i))
            stmt.bindLong(3, dataGenerator.getEmployeeAge(i))
            stmt.bindLong(4, dataGenerator.getEmployeeHiredStatusAsInt(i))
            stmt.executeInsert()
        }
        db.setTransactionSuccessful()
        db.endTransaction()
    }

    private fun deleteRows() {
        db.beginTransaction()
        db.execSQL(String.format("DELETE FROM %s", dbHelper.TABLE_SIMPLE))
        db.setTransactionSuccessful()
        db.endTransaction()
    }


    override fun simpleQuery() {
        val query = String.format("SELECT * FROM %s WHERE hired = 0 AND age BETWEEN 20 AND 50 AND name = 'Foo0'", dbHelper.TABLE_SIMPLE)
        benchmarkRule.measureRepeated {
            runWithTimingDisabled { addRows() }

            // Run query
            val cursor = db.rawQuery(query, null)

            // Read a single property to actually exercise the query and try to normalize
            // the workload across libraries (as queries/reading data behaves quite differently)
            cursor.moveToFirst()
            while (!cursor.isAfterLast) {
                @Suppress("UNUSED_VARIABLE")
                val i = cursor.getInt(0)
                cursor.moveToNext()
            }
            cursor.close()

            runWithTimingDisabled { deleteRows() }
        }
    }

    override fun simpleWrite() {
        val stmt = db.compileStatement("INSERT INTO " + dbHelper.TABLE_SIMPLE + " VALUES(?1, ?2, ?3, ?4)")
        var i: Long = 0
        benchmarkRule.measureRepeated {
            runWithTimingDisabled { deleteRows() }
            stmt.clearBindings()
            stmt.bindLong(1, i)
            stmt.bindString(2, dataGenerator.getEmployeeName(i))
            stmt.bindLong(3, dataGenerator.getEmployeeAge(i))
            stmt.bindLong(4, dataGenerator.getEmployeeHiredStatusAsInt(i))

            db.beginTransaction()
            stmt.executeInsert()
            db.setTransactionSuccessful()
            db.endTransaction()
            i++
        }
    }

    override fun batchWrite() {
        benchmarkRule.measureRepeated {
            runWithTimingDisabled { deleteRows() }
            addRows()
        }
    }

    override fun fullScan() {
        addRows()
        benchmarkRule.measureRepeated {
            val cursor = db.rawQuery("SELECT * FROM " + dbHelper.TABLE_SIMPLE
                    + " WHERE hired = 1 AND age BETWEEN -2 AND -1 AND name = 'Smile1'", null)
            @Suppress("UNUSED_VARIABLE")
            val count = cursor.count
            cursor.close()
        }
    }

    override fun delete() {
        benchmarkRule.measureRepeated {
            runWithTimingDisabled { addRows() }
            db.beginTransaction()
            db.execSQL(String.format("DELETE FROM %s", dbHelper.TABLE_SIMPLE))
            db.setTransactionSuccessful()
            db.endTransaction()
        }
    }

    override fun sum() {
        addRows()
        benchmarkRule.measureRepeated {
            val cursor = db.rawQuery(String.format("SELECT SUM(age) AS sum FROM %s", dbHelper.TABLE_SIMPLE), null)
            cursor.moveToFirst()
            @Suppress("UNUSED_VARIABLE")
            val sum = cursor.getInt(0)
            cursor.close()
        }
    }

    override fun count() {
        addRows()
        benchmarkRule.measureRepeated {
            val cursor = db.rawQuery(String.format("SELECT COUNT(*) AS count FROM %s", dbHelper.TABLE_SIMPLE), null)
            cursor.moveToFirst()
            @Suppress("UNUSED_VARIABLE") val count = cursor.getInt(0)
            cursor.close()
        }
    }

}
