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

package io.realm.datastorebenchmark.tests;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import io.realm.datastorebenchmark.Benchmark;
import io.realm.datastorebenchmark.DataStoreTest;
import io.realm.datastorebenchmark.entities.SQLiteDatabaseHelper;

public class TestSQLite extends DataStoreTest {
    protected SQLiteDatabase db;
    private SQLiteDatabaseHelper dbHelper;
    private long numberOfIterations;

    public TestSQLite(Context context, long numberOfObjects, long numberOfIterations) {
        super(context, numberOfObjects);
        this.numberOfIterations = numberOfIterations;
    }

    protected void setUp() {
        super.setUp();
        dbHelper = new SQLiteDatabaseHelper(context);
        db = dbHelper.getWritableDatabase();
    }

    protected void tearDown() {
        super.tearDown();
        db.close();
        dbHelper.close();
    }

    private void addSimpleTable() {
        db.execSQL("DROP TABLE IF EXISTS " + dbHelper.TABLE_SIMPLE);
        db.execSQL("CREATE TABLE " + dbHelper.TABLE_SIMPLE + " ("
                + "id INTEGER, "
                + "name TEXT,"
                + "age INTEGER,"
                + "hired INTEGER"
                + ")");
    }

    private void addRows() {
        SQLiteStatement stmt = db.compileStatement("INSERT INTO " + dbHelper.TABLE_SIMPLE+ " VALUES(?1, ?2, ?3, ?4)");
        db.beginTransaction();
        for (int i = 0; i < numberOfObjects; i++) {
            stmt.clearBindings();
            stmt.bindLong(1, i);
            stmt.bindString(2, dataGenerator.getEmployeeName(i));
            stmt.bindLong(3, dataGenerator.getEmployeeAge(i));
            stmt.bindLong(4, dataGenerator.getEmployeeHiredStatusAsInt(i));
            stmt.executeInsert();
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    private void verify() {
        Cursor cursor = db.rawQuery(String.format("SELECT COUNT(*) AS count FROM %s", dbHelper.TABLE_SIMPLE), null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        if (count != numberOfObjects) {
            throw new RuntimeException(String.format("Number of row is %d - %d expected.", count, numberOfObjects));
        }
    }

    private void deleteRows() {
        db.beginTransaction();
        db.execSQL(String.format("DELETE FROM %s", dbHelper.TABLE_SIMPLE));
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    @Override
    public void testSimpleQuery() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            @Override
            public void setUp() {
                addSimpleTable();
                addRows();
                verify();
            }

            @Override
            public void tearDown() {
                deleteRows();
            }

            @Override
            public void run() {
                Cursor cursor = db.rawQuery(
                        String.format("SELECT * FROM %s WHERE hired = 0 AND age BETWEEN 20 AND 50 AND name = 'Foo0'",
                        dbHelper.TABLE_SIMPLE), null);
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    int i = cursor.getInt(0);
                    cursor.moveToNext();
                }
                cursor.close();
            }
        };
        measurements.put(TEST_SIMPLE_QUERY, benchmark.execute(numberOfIterations));

        tearDown();
    }

    @Override
    public void testSimpleWrite() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            private SQLiteStatement stmt;
            private int i;

            @Override
            public void setUp() {
                addSimpleTable();
                stmt = db.compileStatement("INSERT INTO " + dbHelper.TABLE_SIMPLE + " VALUES(?1, ?2, ?3, ?4)");
                i = 0;
            }

            @Override
            public void tearDown() {
                deleteRows();
            }

            @Override
            public void run() {
                stmt.clearBindings();
                stmt.bindLong(1, i);
                stmt.bindString(2, dataGenerator.getEmployeeName(i));
                stmt.bindLong(3, dataGenerator.getEmployeeAge(i));
                db.beginTransaction();
                stmt.executeInsert();
                db.setTransactionSuccessful();
                db.endTransaction();
                i++;
            }
        };
        measurements.put(TEST_SIMPLE_WRITE, benchmark.execute(numberOfIterations));

        tearDown();
    }

    @Override
    public void testBatchWrite() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            @Override
            public void setUp() {
                addSimpleTable();
            }

            @Override
            public void tearDown() {
                deleteRows();
            }

            @Override
            public void run() {
                addRows();
            }
        };
        measurements.put(TEST_BATCH_WRITE, benchmark.execute(numberOfIterations));

        tearDown();
    }

    @Override
    public void testFullScan() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            @Override
            public void setUp() {
                addSimpleTable();
                addRows();
                verify();
            }

            @Override
            public void tearDown() {
                deleteRows();
            }

            @Override
            public void run() {
                Cursor cursor = db.rawQuery("SELECT * FROM " + dbHelper.TABLE_SIMPLE
                        + " WHERE hired = 1 AND age BETWEEN -2 AND -1 AND name = 'Smile1'", null);
                int count = cursor.getCount();
                cursor.close();
            }
        };
        measurements.put(TEST_FULL_SCAN, benchmark.execute(numberOfIterations));

        tearDown();
    }

    @Override
    public void testDelete() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            private SQLiteStatement stmt;
            private int i;

            @Override
            public void setUp() {
                addRows();
            }

            @Override
            public void tearDown() {
                deleteRows();
            }

            @Override
            public void run() {
                db.beginTransaction();
                db.execSQL(String.format("DELETE FROM %s", dbHelper.TABLE_SIMPLE));
                db.setTransactionSuccessful();
                db.endTransaction();
            }
        };
        measurements.put(TEST_DELETE, benchmark.execute(numberOfIterations));

        tearDown();
    }

    @Override
    public void testSum() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            @Override
            public void setUp() {
                addSimpleTable();
                addRows();
                verify();
            }

            @Override
            public void tearDown() {
                deleteRows();
            }

            @Override
            public void run() {
                Cursor cursor = db.rawQuery(String.format("SELECT SUM(age) AS sum FROM %s", dbHelper.TABLE_SIMPLE), null);
                cursor.moveToFirst();
                int sum = cursor.getInt(0);
                cursor.close();
            }
        };
        measurements.put(TEST_SUM, benchmark.execute(numberOfIterations));

        tearDown();
    }

    @Override
    public void testCount() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            @Override
            public void setUp() {
                addSimpleTable();
                addRows();
                verify();
            }

            @Override
            public void tearDown() {
                deleteRows();
            }

            @Override
            public void run() {
                Cursor cursor = db.rawQuery(String.format("SELECT COUNT(*) AS count FROM %s", dbHelper.TABLE_SIMPLE), null);
                cursor.moveToFirst();
                int count = cursor.getInt(0);
                cursor.close();
            }
        };
        measurements.put(TEST_COUNT, benchmark.execute(numberOfIterations));

        tearDown();
    }

    @Override
    protected String getTag() {
        return "sqlite";
    }
}