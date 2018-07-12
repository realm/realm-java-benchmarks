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

import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;
import io.realm.datastorebenchmark.Benchmark;
import io.realm.datastorebenchmark.DataStoreTest;
import io.realm.datastorebenchmark.realm.Employee;

public class TestRealm extends DataStoreTest {

    private RealmConfiguration realmConfiguration;

    public TestRealm(Context context, long numberOfObjects, long warmupIterations, long testIterations) {
        super(context, numberOfObjects, warmupIterations, testIterations);
        Realm.init(context);
        this.realmConfiguration = new RealmConfiguration.Builder().build();
    }

    public void setUp() {
        super.setUp();
        Realm.deleteRealm(realmConfiguration);
    }

    public void tearDown() {
        super.tearDown();
    }

    private void addObjects(Realm realm) {
        realm.beginTransaction();
        for (int i = 0; i < numberOfObjects; i++) {
            Employee employee = realm.createObject(Employee.class);
            employee.setName(dataGenerator.getEmployeeName(i));
            employee.setAge(dataGenerator.getEmployeeAge(i));
            employee.setHired(dataGenerator.getHiredBool(i));
            employee.setId(i);
        }
        realm.commitTransaction();
    }

    private void verify(Realm realm) {
        long objectCount = realm.where(Employee.class).count();
        if (objectCount != numberOfObjects) {
            throw new RuntimeException(String.format("Number of objects is %d - %d expected.",
                    objectCount, numberOfObjects));
        }
    }

    private void deleteObjects(Realm realm) {
        realm.beginTransaction();
        realm.deleteAll();;
        realm.commitTransaction();
    }

    @Override
    public void testSimpleQuery() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            private Realm realm;

            @Override
            public void setUp() {
                realm = Realm.getInstance(realmConfiguration);
                addObjects(realm);
                verify(realm);
            }

            @Override
            public void tearDown() {
                deleteObjects(realm);
                realm.close();
            }

            @Override
            public void run() {
                RealmResults<Employee> employees = realm.where(Employee.class)
                        .equalTo("hired", false)
                        .between("age", 20, 50)
                        .equalTo("name", "Foo0").findAll();
                for (Employee employee : employees) {
                    long tmp = employee.getId();
                }
            }
        };
        measurements.put(TEST_SIMPLE_QUERY, benchmark.execute(TEST_SIMPLE_QUERY, warmupIterations, testIterations));

        tearDown();
    }

    @Override
    public void testSimpleWrite() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            private Realm realm;
            private int i;

            @Override
            public void setUp() {
                realm = Realm.getInstance(realmConfiguration);
                i = 0;
            }

            @Override
            public void tearDown() {
                realm.close();
            }

            @Override
            public void run() {
                realm.beginTransaction();
                Employee employee = realm.createObject(Employee.class);
                employee.setId(i);
                employee.setName(dataGenerator.getEmployeeName(i));
                employee.setHired(dataGenerator.getHiredBool(i));
                employee.setAge(dataGenerator.getEmployeeAge(i));
                realm.commitTransaction();
                i++;
            }

            @Override
            protected void cleanupRun() {
                deleteObjects(realm);
            }
        };
        measurements.put(TEST_SIMPLE_WRITE, benchmark.execute(TEST_SIMPLE_WRITE, warmupIterations, testIterations));

        tearDown();
    }

    @Override
    public void testBatchWrite() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            private Realm realm;
            private ArrayList<Employee> employees;

            @Override
            public void setUp() {
                realm = Realm.getInstance(realmConfiguration);
                employees = new ArrayList<Employee>((int)numberOfObjects);
                Employee employee;
                for (int i = 0; i < numberOfObjects; i++) {
                    employee = new Employee();
                    employee.setId(i);
                    employee.setName(dataGenerator.getEmployeeName(i));
                    employee.setHired(dataGenerator.getHiredBool(i));
                    employee.setAge(dataGenerator.getEmployeeAge(i));

                    employees.add(employee);
                }
            }

            @Override
            public void run() {
                realm.beginTransaction();
                realm.insert(employees);
                realm.commitTransaction();
            }

            @Override
            protected void cleanupRun() {
                deleteObjects(realm);
            }

            @Override
            public void tearDown() {
                realm.close();
            }
        };
        measurements.put(TEST_BATCH_WRITE, benchmark.execute(TEST_BATCH_WRITE, warmupIterations, testIterations));

        tearDown();
    }

    @Override
    public void testFullScan() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            private Realm realm;

            @Override
            public void setUp() {
                realm = Realm.getInstance(realmConfiguration);
                addObjects(realm);
                verify(realm);
            }

            @Override
            public void tearDown() {
                deleteObjects(realm);
                realm.close();
            }

            @Override
            public void run() {
                RealmResults<Employee> realmResults = realm.where(Employee.class)
                        .equalTo("hired", true)
                        .between("age", -2, -1)
                        .equalTo("name", "Smile1").findAll();
                long count = realmResults.size();
            }
        };
        measurements.put(TEST_FULL_SCAN, benchmark.execute(TEST_FULL_SCAN, warmupIterations, testIterations));

        tearDown();
    }

    @Override
    public void testDelete() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            private Realm realm;

            @Override
            public void setUp() {
                realm = Realm.getInstance(realmConfiguration);
            }

            @Override
            public void tearDown() {
                realm.close();
            }

            @Override
            protected void prepareRun() {
                addObjects(realm);
            }

            @Override
            public void run() {
                realm.beginTransaction();
                realm.delete(Employee.class);
                realm.commitTransaction();
            }

        };
        measurements.put(TEST_DELETE, benchmark.execute(TEST_DELETE, warmupIterations, testIterations));

        tearDown();
    }

    @Override
    public void testSum() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            private Realm realm;

            @Override
            public void setUp() {
                realm = Realm.getInstance(realmConfiguration);
                addObjects(realm);
                verify(realm);
            }

            @Override
            public void tearDown() {
                deleteObjects(realm);
                realm.close();
            }

            @Override
            public void run() {
                long sum = realm.where(Employee.class).sum("age").longValue();
            }
        };
        measurements.put(TEST_SUM, benchmark.execute(TEST_SUM, warmupIterations, testIterations));

        tearDown();
    }

    @Override
    public void testCount() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            private Realm realm;

            @Override
            public void setUp() {
                realm = Realm.getInstance(realmConfiguration);
                addObjects(realm);
                verify(realm);
            }

            @Override
            public void tearDown() {
                deleteObjects(realm);
                realm.close();
            }

            @Override
            public void run() {
                long count = realm.where(Employee.class).count();
            }
        };
        measurements.put(TEST_COUNT, benchmark.execute(TEST_COUNT, warmupIterations, testIterations));

        tearDown();
    }

    @Override
    protected String getTag() {
        return "realm";
    }
}
