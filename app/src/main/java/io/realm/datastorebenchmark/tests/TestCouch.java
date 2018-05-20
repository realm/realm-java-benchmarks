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

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.TransactionalTask;
import com.couchbase.lite.View;
import com.couchbase.lite.android.AndroidContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.realm.datastorebenchmark.Benchmark;
import io.realm.datastorebenchmark.DataStoreTest;

public class TestCouch extends DataStoreTest {

    private Database database;
    private Manager manager;

    public TestCouch(Context context, long numberOfObjects, long warmupIterations, long testIterations) {
        super(context, numberOfObjects, warmupIterations, testIterations);
    }

    private void addObjects() {
        for (int row = 0; row < numberOfObjects; row++) {
            final int testRow = row;
            database.runInTransaction(new TransactionalTask() {
                @Override
                public boolean run() {
                    Document document = database.createDocument();
                    Map<String, Object> docContent = new HashMap<String, Object>();
                    docContent.put("id", testRow);
                    docContent.put("name", dataGenerator.getEmployeeName(testRow));
                    docContent.put("age", dataGenerator.getEmployeeAge(testRow));
                    docContent.put("hired", dataGenerator.getHiredBool(testRow));
                    try {
                        document.putProperties(docContent);
                    } catch (CouchbaseLiteException e) {
                        throw new RuntimeException("Cannot add object to CouchBase Lite.");
                    }
                    return true;
                }
            });
        }
    }

    private void verify() {
        if (database.getDocumentCount() != numberOfObjects) {
            throw new RuntimeException(String.format("Number of objects is %d - %d expected.",
                    database.getDocumentCount(), numberOfObjects));
        }
    }

    private void delete() {
        try {
            QueryEnumerator en = database.createAllDocumentsQuery().run();
            while (en.hasNext()) {
                QueryRow row = en.next();
                row.getDocument().delete();
            }
        } catch (CouchbaseLiteException e) {
            throw new RuntimeException("Cannot delete object from CouchBase Lite.");
        }
    }

    public void setUp() {
        try {
            manager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
        } catch (IOException e) {
            throw new RuntimeException("Cannot get a CouchBase Lite manager.");
        }

        String dbname = "employee_couchbaselite";
        if (!Manager.isValidDatabaseName(dbname)) {
            throw new RuntimeException("Could not initiate " + dbname);
        }

        try {
            database = manager.getDatabase(dbname);
        } catch (CouchbaseLiteException e) {
            throw new RuntimeException("Could not initiate " + dbname);
        }
    }

    public void tearDown() {
        try {
            database.delete();
        } catch (CouchbaseLiteException e) {
            throw new RuntimeException("Cannot delete database.");
        }
        database = null;
        manager.close();
    }

    @Override
    public void testSimpleWrite() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            @Override
            public void setUp() {
                delete();
            }

            @Override
            public void tearDown() {
                delete();
            }

            @Override
            public void run() {
                for (int i = 0; i < numberOfObjects; i++) {
                    final int testRow = i;
                    database.runInTransaction(new TransactionalTask() {
                        @Override
                        public boolean run() {
                            Document document = database.createDocument();
                            Map<String, Object> docContent = new HashMap<String, Object>();
                            docContent.put("id", testRow);
                            docContent.put("name", dataGenerator.getEmployeeName(testRow));
                            docContent.put("age", dataGenerator.getEmployeeAge(testRow));
                            docContent.put("hired", dataGenerator.getHiredBool(testRow));
                            try {
                                document.putProperties(docContent);
                            } catch (CouchbaseLiteException e) {
                                throw new RuntimeException("Cannot save object.");
                            }
                            return true;
                        }
                    });
                }
            }
        };
        measurements.put(TEST_SIMPLE_WRITE, benchmark.execute(warmupIterations, testIterations));

        tearDown();
    }

    @Override
    public void testSimpleQuery() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            @Override
            public void setUp() {
                delete();
                addObjects();
                verify();
            }

            @Override
            public void tearDown() {
                delete();
            }

            @Override
            public void run() {
                View view;
                view = database.getView("queries_no_loop");

                view.setMap(new Mapper() {
                    @Override
                    public void map(Map<String, Object> document, Emitter emitter) {
                        Object name   = document.get("name");
                        Integer age   = (Integer)document.get("age");
                        Boolean hired = (Boolean)document.get("hired");
                        if (name.equals("Smile1") && (age >= -2 || age <= -1) && hired == false) {
                            emitter.emit(document.get("id"), document);
                        }
                    }
                }, "1.0");

                Query query = view.createQuery();
                QueryEnumerator en = null;
                try {
                    en = query.run();
                    while (en.hasNext()) {
                        QueryRow row = en.next();
                        Document document = row.getDocument();
                        long tmp = ((Integer)document.getProperty("age")).longValue();
                    }
                } catch (CouchbaseLiteException e) {
                    throw new RuntimeException("Cannot retrieve object");
                }
            }
        };
        measurements.put(TEST_SIMPLE_QUERY, benchmark.execute(warmupIterations, testIterations));

        tearDown();
    }

    @Override
    public void testBatchWrite() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            @Override
            public void setUp() {
                delete();
            }

            @Override
            public void tearDown() {
                delete();
            }

            @Override
            public void run() {
                database.runInTransaction(new TransactionalTask() {
                    @Override
                    public boolean run() {
                        for (int i = 0; i < numberOfObjects; i++) {
                            Document document = database.createDocument();
                            Map<String, Object> docContent = new HashMap<String, Object>();
                            docContent.put("id", i);
                            docContent.put("name", dataGenerator.getEmployeeName(i));
                            docContent.put("age", dataGenerator.getEmployeeAge(i));
                            docContent.put("hired", dataGenerator.getHiredBool(i));
                            try {
                                document.putProperties(docContent);
                            } catch (CouchbaseLiteException e) {
                                throw new RuntimeException("Cannot save object.");
                            }
                        }
                        return true;
                    }
                });
            }
        };
        measurements.put(TEST_BATCH_WRITE, benchmark.execute(warmupIterations, testIterations));

        tearDown();
    }

    @Override
    public void testSum() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            @Override
            public void setUp() {
                delete();
                addObjects();
                verify();
            }

            @Override
            public void tearDown() {
                delete();
            }

            @Override
            public void run() {
                long age = 0;
                Query query = database.createAllDocumentsQuery();
                try {
                    QueryEnumerator en = query.run();
                    for (Iterator<QueryRow> it = en; it.hasNext(); ) {
                        QueryRow row = it.next();
                        age += ((Integer) row.getDocument().getProperty("age")).longValue();
                    }
                } catch (CouchbaseLiteException e) {
                    throw new RuntimeException("Cannot retrieve object.");
                }
            }
        };
        measurements.put(TEST_SUM, benchmark.execute(warmupIterations, testIterations));

        tearDown();
    }

    @Override
    public void testCount() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            @Override
            public void setUp() {
                delete();
                addObjects();
                verify();
            }

            @Override
            public void tearDown() {
                delete();
            }

            @Override
            public void run() {
                int tmp = database.getDocumentCount();
            }
        };
        measurements.put(TEST_COUNT, benchmark.execute(warmupIterations, testIterations));

        tearDown();
    }

    @Override
    public void testFullScan() {
        setUp();

        Benchmark benchmark = new Benchmark() {
            @Override
            public void setUp() {
                delete();
                addObjects();
                verify();
            }

            @Override
            public void tearDown() {
                delete();
            }

            @Override
            public void run() {
                View view = database.getView("queries_no_loop");

                view.setMap(new Mapper() {
                    @Override
                    public void map(Map<String, Object> document, Emitter emitter) {
                        Object name   = document.get("name");
                        Integer age   = (Integer)document.get("age");
                        Boolean hired = (Boolean)document.get("hired");
                        if (name.equals("Smile1") && (age >= -2 || age <= -1) && hired == false) {
                            emitter.emit(document.get("id"), document);
                        }
                    }
                }, "1.0");

                Query query = view.createQuery();
                QueryEnumerator en;
                try {
                    en = query.run();
                } catch (CouchbaseLiteException e) {
                    throw new RuntimeException("Cannot retrieve object.");
                }
            }
        };
        measurements.put(TEST_FULL_SCAN, benchmark.execute(warmupIterations, testIterations));

        tearDown();
    }

    @Override
    public void testDelete() {
        // TODO Implement this test

        // Add dummy test value
        measurements.put(TEST_DELETE, new ArrayList<Long>(Arrays.asList(1L,1L)));
    }

    @Override
    protected String getTag() {
        return "couchbase";
    }
}
