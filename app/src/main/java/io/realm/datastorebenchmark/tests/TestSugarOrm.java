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

import com.orm.query.Condition;
import com.orm.query.Select;

import java.util.ArrayList;
import java.util.List;

import io.realm.datastorebenchmark.Benchmark;
import io.realm.datastorebenchmark.TestDataStore;
import io.realm.datastorebenchmark.sugarorm.SugarEmployee;

public class TestSugarOrm extends TestDataStore {

    private long numberOfIterations;

    public TestSugarOrm(Context context, long numberOfObjects, long numberOfIterations) {
        super(context, numberOfObjects);
        this.numberOfIterations = numberOfIterations;
    }


    private void addObjects() {
        for (int i = 0; i < numberOfObjects; i++) {
            SugarEmployee employee = new SugarEmployee(i,
                    dataGenerator.getEmployeeName(i),
                    dataGenerator.getEmployeeAge(i),
                    dataGenerator.getEmployeeHiredStatus(i));
            employee.save();
        }
    }

    private void deleteObjects() {
        SugarEmployee.deleteAll(SugarEmployee.class);
    }

    private void verify() {
        if (SugarEmployee.count(SugarEmployee.class, null, null, null, null, null) != numberOfObjects) {
            throw new RuntimeException(String.format("Number of objects is %d - %d expected.",
                    SugarEmployee.count(SugarEmployee.class, null, null, null, null, null), numberOfObjects));
        }
    }

    @Override
    public void testSimpleWrite() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            @Override
            public void setUp() {
                deleteObjects();
            }

            @Override
            public void tearDown() {
                deleteObjects();
            }

            @Override
            public void run() {
                addObjects();
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
                deleteObjects();
                addObjects();
                verify();
            }

            @Override
            public void tearDown() {
                deleteObjects();
            }

            @Override
            public void run() {
                List<SugarEmployee> list = Select.from(SugarEmployee.class).where(
                        Condition.prop("hired").eq(0),
                        Condition.prop("age").gt(19).lt(51),
                        Condition.prop("name").eq("Foo0")).list();
                for (SugarEmployee e : list) {
                    long tmp = e.getId();
                }
            }
        };
        measurements.put(TEST_SIMPLE_QUERY, benchmark.execute(numberOfIterations));

        tearDown();
    }

    @Override
    public void testBatchWrite() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            private List<SugarEmployee> sugarEmployeeList;

            @Override
            public void setUp() {
                deleteObjects();
                sugarEmployeeList = new ArrayList<SugarEmployee>();
                for (int i = 0; i < numberOfObjects; i++) {
                    SugarEmployee sugarEmployee = new SugarEmployee(i,
                            dataGenerator.getEmployeeName(i),
                            dataGenerator.getEmployeeAge(i),
                            dataGenerator.getEmployeeHiredStatus(i));
                    sugarEmployeeList.add(sugarEmployee);
                }
            }

            @Override
            public void tearDown() {
                deleteObjects();
                sugarEmployeeList.clear();
            }

            @Override
            public void run() {
                // FIXME: This is a deprecated method but we use it anyway
                SugarEmployee.saveInTx(sugarEmployeeList);
            }
        };
        measurements.put(TEST_BATCH_WRITE, benchmark.execute(numberOfIterations));
    }

    @Override
    public void testSum() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            @Override
            public void setUp() {
                deleteObjects();
                addObjects();
                verify();
            }

            @Override
            public void tearDown() {
                deleteObjects();
            }

            @Override
            public void run() {
                // FIXME: No sum function
                List<SugarEmployee> sugarEmployeeList = Select.from(SugarEmployee.class).list();
                long sum = 0;
                for (SugarEmployee e : sugarEmployeeList) {
                    sum += e.getAge();
                }
            }
        };
        measurements.put(TEST_SUM, benchmark.execute(numberOfIterations));
    }

    @Override
    public void testCount() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            @Override
            public void setUp() {
                deleteObjects();
                addObjects();
                verify();
            }

            @Override
            public void tearDown() {
                deleteObjects();
            }

            @Override
            public void run() {
                long count = SugarEmployee.count(SugarEmployee.class, null, null, null, null, null);
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
                deleteObjects();
                addObjects();
                verify();
            }

            @Override
            public void tearDown() {
                deleteObjects();
            }

            @Override
            public void run() {
                Select outcome = Select.from(SugarEmployee.class)
                        .where(Condition.prop("name").eq("Smile1"),
                                Condition.prop("age").gt(-3).lt(0),
                                Condition.prop("hired").eq(1));
                long tmp = outcome.list().size();
            }
        };
        measurements.put(TEST_FULL_SCAN, benchmark.execute(numberOfIterations));

        tearDown();
    }

    @Override
    public void testDelete() {
        // TODO Implement this test
    }
}
