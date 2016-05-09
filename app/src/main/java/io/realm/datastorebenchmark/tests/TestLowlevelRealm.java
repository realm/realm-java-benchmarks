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

import java.io.File;

import io.realm.RealmFieldType;
import io.realm.datastorebenchmark.Benchmark;
import io.realm.datastorebenchmark.DataStoreTest;
import io.realm.internal.ReadTransaction;
import io.realm.internal.SharedGroup;
import io.realm.internal.Table;
import io.realm.internal.TableView;
import io.realm.internal.WriteTransaction;

public class TestLowlevelRealm extends DataStoreTest {
    private String TABLE_NAME = "dummy";
    private long numberOfIterations;

    public TestLowlevelRealm(Context context, long numberOfObjects, long numberOfIterations) {
        super(context, numberOfObjects);
        this.numberOfIterations = numberOfIterations;
    }

    private SharedGroup addTable() {
        String fileName = context.getFilesDir() + "/default.realm";
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }
        SharedGroup sharedGroup = new SharedGroup(fileName);
        WriteTransaction writeTransaction = sharedGroup.beginWrite();
        Table table = writeTransaction.getTable(TABLE_NAME);
        table.addColumn(RealmFieldType.INTEGER, "id");
        table.addColumn(RealmFieldType.STRING, "name");
        table.addColumn(RealmFieldType.INTEGER, "age");
        table.addColumn(RealmFieldType.BOOLEAN, "hired");
        writeTransaction.commit();
        return sharedGroup;
    }

    private void addRows(SharedGroup sharedGroup) {
        WriteTransaction writeTransaction = sharedGroup.beginWrite();
        Table table = writeTransaction.getTable(TABLE_NAME);
        for (int i = 0; i < numberOfObjects; i++) {
            long rowIndex = table.addEmptyRow();
            table.setLong(0, rowIndex, i);
            table.setString(1, rowIndex, dataGenerator.getEmployeeName(i));
            table.setLong(2, rowIndex, dataGenerator.getEmployeeAge(i));
            table.setBoolean(3, rowIndex, dataGenerator.getEmployeeHiredStatus(i));
        }
        writeTransaction.commit();
    }

    private void verify(SharedGroup sharedGroup) {
        ReadTransaction readTransaction = sharedGroup.beginRead();
        Table table = readTransaction.getTable(TABLE_NAME);
        if (table.size() != numberOfObjects) {
            throw new RuntimeException(String.format("Number of row is %d - %d expected.",
                    table.size(), numberOfObjects));
        }
        readTransaction.endRead();
    }

    @Override
    public void testSimpleWrite() {
        Benchmark benchmark = new Benchmark() {
            private SharedGroup sharedGroup;
            private int i;

            @Override
            public void setUp() {
                sharedGroup = addTable();
                i = 0;
            }

            @Override
            public void tearDown() {
                sharedGroup.close();
            }

            @Override
            public void run() {
                WriteTransaction writeTransaction = sharedGroup.beginWrite();
                Table table = writeTransaction.getTable(TABLE_NAME);
                long rowIndex = table.addEmptyRow();
                table.setLong(0, rowIndex, i);
                table.setString(1, rowIndex, dataGenerator.getEmployeeName(i));
                table.setLong(2, rowIndex, dataGenerator.getEmployeeAge(i));
                table.setBoolean(3, rowIndex, dataGenerator.getHiredBool(i));
                writeTransaction.commit();
                i++;
            }
        };
        measurements.put(TEST_SIMPLE_WRITE, benchmark.execute(numberOfIterations));

    }

    @Override
    public void testSimpleQuery() {
        Benchmark benchmark = new Benchmark() {
            private SharedGroup sharedGroup;

            @Override
            public void setUp() {
                sharedGroup = addTable();
                addRows(sharedGroup);
                verify(sharedGroup);
            }

            @Override
            public void tearDown() {
                sharedGroup.close();
            }

            @Override
            public void run() {
                ReadTransaction readTransaction = sharedGroup.beginRead();
                TableView tableView = readTransaction.getTable(TABLE_NAME).where()
                        .equalTo(new long[]{3}, false)
                        .between(new long[]{2}, 20, 50)
                        .equalTo(new long[]{1}, "Foo0")
                        .findAll();
                long size = tableView.size();
                for (int i = 0; i < size; i++) {
                    long id = tableView.getLong(0, i);
                }
                readTransaction.endRead();
            }
        };
        measurements.put(TEST_SIMPLE_QUERY, benchmark.execute(numberOfIterations));
    }

    @Override
    public void testBatchWrite() {
        Benchmark benchmark = new Benchmark() {
            private SharedGroup sharedGroup;

            @Override
            public void setUp() {
                sharedGroup = addTable();
            }

            @Override
            public void tearDown() {
                sharedGroup.close();
            }

            @Override
            public void run() {
                WriteTransaction writeTransaction = sharedGroup.beginWrite();
                Table table = writeTransaction.getTable(TABLE_NAME);
                table.addEmptyRows(numberOfObjects);
                for (int i = 0; i < numberOfObjects; i++) {
                    table.setLong(0, i, i);
                    table.setString(1, i, dataGenerator.getEmployeeName(i));
                    table.setLong(2, i, dataGenerator.getEmployeeAge(i));
                    table.setBoolean(3, i, dataGenerator.getEmployeeHiredStatus(i));
                }
                writeTransaction.commit();
            }
        };
        measurements.put(TEST_BATCH_WRITE, benchmark.execute(numberOfIterations));
    }

    @Override
    public void testFullScan() {
        Benchmark benchmark = new Benchmark() {
            private SharedGroup sharedGroup;

            @Override
            public void setUp() {
                sharedGroup = addTable();
                addRows(sharedGroup);
                verify(sharedGroup);
            }

            @Override
            public void tearDown() {
                sharedGroup.close();
            }

            @Override
            public void run() {
                ReadTransaction readTransaction = sharedGroup.beginRead();
                Table table = readTransaction.getTable(TABLE_NAME);
                TableView tableView = table.where()
                        .equalTo(new long[]{3}, true)
                        .between(new long[]{2}, -2, -1)
                        .equalTo(new long[]{1}, "Smile1")
                        .findAll();
                long count = tableView.size();
                readTransaction.endRead();
            }
        };
        measurements.put(TEST_FULL_SCAN, benchmark.execute(numberOfIterations));
    }

    @Override
    public void testDelete() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            private SharedGroup sharedGroup;

            @Override
            public void setUp() {
                sharedGroup = addTable();
                addRows(sharedGroup);
                verify(sharedGroup);
            }

            @Override
            public void tearDown() {
                sharedGroup.close();
            }

            @Override
            public void run() {
                WriteTransaction writeTransaction = sharedGroup.beginWrite();
                writeTransaction.getTable(TABLE_NAME).clear();
                writeTransaction.commit();
            }
        };
        measurements.put(TEST_DELETE, benchmark.execute(numberOfIterations));

        tearDown();
    }

    @Override
    public void testSum() {
        Benchmark benchmark = new Benchmark() {
            private SharedGroup sharedGroup;

            @Override
            public void setUp() {
                sharedGroup = addTable();
                addRows(sharedGroup);
                verify(sharedGroup);
            }

            @Override
            public void tearDown() {
                sharedGroup.close();
            }

            @Override
            public void run() {
                ReadTransaction readTransaction = sharedGroup.beginRead();
                Table table = readTransaction.getTable(TABLE_NAME);
                long sum = table.sumLong(2);
                readTransaction.endRead();
            }
        };
        measurements.put(TEST_SUM, benchmark.execute(numberOfIterations));
    }

    @Override
    public void testCount() {
        Benchmark benchmark = new Benchmark() {
            private SharedGroup sharedGroup;

            @Override
            public void setUp() {
                sharedGroup = addTable();
                addRows(sharedGroup);
                verify(sharedGroup);
            }

            @Override
            public void tearDown() {
                sharedGroup.close();
            }

            @Override
            public void run() {
                ReadTransaction readTransaction = sharedGroup.beginRead();
                Table table = readTransaction.getTable(TABLE_NAME);
                long count = table.size();
                readTransaction.endRead();
            }
        };
        measurements.put(TEST_COUNT, benchmark.execute(numberOfIterations));
    }

    @Override
    protected String getTag() {
        return "realmlowlevel";
    }
}
