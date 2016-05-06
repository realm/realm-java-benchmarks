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

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

import io.realm.datastorebenchmark.Benchmark;
import io.realm.datastorebenchmark.DataStoreTest;
import io.realm.datastorebenchmark.ormlite.DataBaseHelper;
import io.realm.datastorebenchmark.ormlite.Employee;

public class TestOrmLite extends DataStoreTest {

    private long numberOfIterations;
    private DataBaseHelper dbHelper;

    public TestOrmLite(Context context, long numberOfObjects, long numberOfIterations) {
        super(context, numberOfObjects);
        this.numberOfIterations = numberOfIterations;
    }

    @Override
    protected void setUp() {
        super.setUp();
        DataBaseHelper.init(context, false);
        dbHelper = DataBaseHelper.getInstance();
        ConnectionSource connectionSource = dbHelper.getConnectionSource();
        try {
            TableUtils.dropTable(connectionSource, Employee.class, true);
            TableUtils.createTable(connectionSource, Employee.class);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void tearDown() {
        ConnectionSource connectionSource = dbHelper.getConnectionSource();
        try {
            TableUtils.dropTable(connectionSource, Employee.class, true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        super.tearDown();
    }

    @Override
    public void testSimpleWrite() {
        setUp();
        Benchmark benchmark = new Benchmark() {
            private int i;

            @Override
            public void setUp() {
                i = 0;
            }

            @Override
            public void run() {
                try {
                    Dao<Employee, Long> dao = Employee.getDao();
                    Employee employee = new Employee();
                    employee.setName(dataGenerator.getEmployeeName(i));
                    employee.setHired(dataGenerator.getHiredBool(i));
                    employee.setAge(dataGenerator.getEmployeeAge(i));
                    dao.create(employee);
                    i++;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void tearDown() {
                deleteObjects();
            }
        };
        measurements.put(TEST_SIMPLE_WRITE, benchmark.execute(numberOfIterations));
        tearDown();
    }

    @Override
    public void testSimpleQuery() {
        setUp();
        Benchmark benchmark = new Benchmark() {

            @Override
            public void setUp() {
                addObjects();
                verify();
            }

            @Override
            public void run() {
                try {
                    Dao<Employee, Long> dao = Employee.getDao();
                    List<Employee> employees = dao.queryBuilder().where()
                            .eq("hired", false)
                            .and()
                            .between("age", 20, 50)
                            .and()
                            .eq("name", "Foo0")
                            .query();

                    for (Employee employee : employees) {
                        long id = employee.getId();
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void tearDown() {
                deleteObjects();
            }
        };
        measurements.put(TEST_SIMPLE_QUERY, benchmark.execute(numberOfIterations));
        tearDown();
    }

    @Override
    public void testBatchWrite() {
        setUp();
        Benchmark benchmark = new Benchmark() {
            public ConnectionSource connectionSource;

            @Override
            public void setUp() {
                connectionSource = dbHelper.getConnectionSource();
            }

            @Override
            public void run() {
                try {
                    TransactionManager.callInTransaction(connectionSource, new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            Dao<Employee, Long> dao = Employee.getDao();
                            for (int i = 0; i < numberOfObjects; i++) {
                                Employee employee = new Employee();
                                employee.setName(dataGenerator.getEmployeeName(i));
                                employee.setHired(dataGenerator.getHiredBool(i));
                                employee.setAge(dataGenerator.getEmployeeAge(i));
                                dao.create(employee);
                            }
                            return null;
                        }
                    });
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void tearDown() {
                deleteObjects();
            }
        };
        measurements.put(TEST_BATCH_WRITE, benchmark.execute(numberOfIterations));
        tearDown();
    }

    @Override
    public void testSum() {
        setUp();
        Benchmark benchmark = new Benchmark() {
            @Override
            public void setUp() {
                addObjects();
                verify();
            }

            @Override
            public void run() {
                try {
                    Dao<Employee, Long> dao = Employee.getDao();
                    GenericRawResults<String[]> queryResult = dao.queryBuilder().selectRaw("SUM(age)").queryRaw();
                    String sum = queryResult.getFirstResult()[0];
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void tearDown() {
                deleteObjects();
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
                addObjects();
                verify();
            }

            @Override
            public void run() {
                try {
                    long count = Employee.getDao().countOf();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void tearDown() {
                deleteObjects();
            }
        };
        measurements.put(TEST_COUNT, benchmark.execute(numberOfIterations));
        tearDown();
    }

    @Override
    public void testFullScan() {
        setUp();
        Benchmark benchmark = new Benchmark() {

            @Override
            public void setUp() {
                addObjects();
                verify();
            }

            @Override
            public void run() {
                try {
                    Dao<Employee, Long> dao = Employee.getDao();
                    List<Employee> employees = dao.queryBuilder().where()
                            .eq("hired", true)
                            .and()
                            .between("age", -2, -1)
                            .and()
                            .eq("name", "Smile1")
                            .query();
                    int size = employees.size();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void tearDown() {
                deleteObjects();
            }
        };
        measurements.put(TEST_FULL_SCAN, benchmark.execute(numberOfIterations));
        tearDown();
    }

    @Override
    public void testDelete() {
        // TODO Implement this test
    }

    @Override
    protected String getTag() {
        return "ormlite";
    }

    // Helper method for filling the DB
    private void addObjects() {
        try {
            for (int i = 0; i < numberOfObjects; i++) {
                Employee employee = new Employee();
                employee.setName(dataGenerator.getEmployeeName(i));
                employee.setAge(dataGenerator.getEmployeeAge(i));
                employee.setHired(dataGenerator.getHiredBool(i));
                Employee.getDao().create(employee);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Check that addObjects() did it job
    private void verify() {
        List<Employee> employees;
        try {
            employees = Employee.getDao().queryForAll();
            if (employees.size() != numberOfObjects) {
                throw new RuntimeException(String.format("Number of objects is %d - %d expected.",
                        employees.size(), numberOfObjects));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // helper method for clearing the database
    private void deleteObjects() {
        ConnectionSource connectionSource = dbHelper.getConnectionSource();
        try {
            TableUtils.dropTable(connectionSource, Employee.class, true);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
