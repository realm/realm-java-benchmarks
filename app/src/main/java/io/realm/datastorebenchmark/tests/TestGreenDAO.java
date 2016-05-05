package io.realm.datastorebenchmark.tests;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;

import io.realm.datastorebenchmark.Benchmark;
import io.realm.datastorebenchmark.TestDataStore;
import io.realm.datastorebenchmark.greendao.DaoMaster;
import io.realm.datastorebenchmark.greendao.DaoSession;
import io.realm.datastorebenchmark.greendao.Employee;
import io.realm.datastorebenchmark.greendao.EmployeeDao;

public class TestGreenDAO extends TestDataStore {


    private long numberOfIterations;

    private SQLiteDatabase db;
    private DaoMaster daoMaster;
    private DaoSession daoSession;
    private EmployeeDao employeeDao;


    public TestGreenDAO(Context context, long numberOfObjects, long numberOfIterations) {
        super(context, numberOfObjects);
        this.numberOfIterations = numberOfIterations;
    }

    private void addObjects() {
        try {
            db.beginTransaction();
            for (int i = 0; i < numberOfObjects; i++) {
                Employee employee = new Employee();
                employee.setName(dataGenerator.getEmployeeName(i));
                employee.setAge(dataGenerator.getEmployeeAge(i));
                employee.setHired(dataGenerator.getHiredBool(i));
                employeeDao.insert(employee);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            throw new RuntimeException("Cannot add object.");
        } finally {
            db.endTransaction();
        }
    }

    private void delete() {
        try {
            db.beginTransaction();
            employeeDao.deleteAll();
            db.setTransactionSuccessful();
        }  catch (Exception e) {
            throw new RuntimeException("Cannot delete objects.");
        } finally {
            db.endTransaction();
        }
    }

    private void verify() {
        List<Employee> list = employeeDao.loadAll();

        if (list.size() < numberOfObjects) {
            throw new RuntimeException(String.format("Number of objects is %d - %d expected.",
                    list.size(), numberOfObjects));
        }
    }
    public void setUp() {
        DaoMaster.DevOpenHelper helper
                = new DaoMaster.DevOpenHelper(context, "EmployeeGreenDAO.db", null);
        db = helper.getWritableDatabase();
        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
        employeeDao = daoSession.getEmployeeDao();
        employeeDao.createTable(db, true);
    }

    public void tearDown() {
        employeeDao.dropTable(db, true);
        db.close();
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
                    try {
                        db.beginTransaction();
                        Employee employee = new Employee();
                        employee.setName(dataGenerator.getEmployeeName(i));
                        employee.setAge(dataGenerator.getEmployeeAge(i));
                        employee.setHired(dataGenerator.getHiredBool(i));
                        employeeDao.insert(employee);
                        db.setTransactionSuccessful();
                    } catch (Exception e) {
                        throw new RuntimeException("Cannot add object.");
                    } finally {
                        db.endTransaction();
                    }
                }

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
                de.greenrobot.dao.query.QueryBuilder qb = employeeDao.queryBuilder();
                qb.where(qb.and(EmployeeDao.Properties.Name.eq("Foo1"),
                        EmployeeDao.Properties.Age.between(20, 50),
                        EmployeeDao.Properties.Hired.eq(true)));
                qb.build();
                List<Employee> list = qb.list();
                for (Employee e : list) {
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
                try {
                    db.beginTransaction();
                    for (int i = 0; i < numberOfObjects; i++) {
                        Employee employee = new Employee();
                        employee.setName(dataGenerator.getEmployeeName(i));
                        employee.setAge(dataGenerator.getEmployeeAge(i));
                        employee.setHired(dataGenerator.getHiredBool(i));
                        employeeDao.insert(employee);
                    }
                    db.setTransactionSuccessful();
                } catch (Exception e) {
                    throw new RuntimeException("Cannot add object.");
                } finally {
                    db.endTransaction();
                }
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
                List<Employee> list = employeeDao.loadAll();
                int tmp = 0;
                for (Employee e : list) {
                    tmp += e.getAge();
                }
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
                List<Employee> list = employeeDao.loadAll();
                int tmp = list.size();
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
                de.greenrobot.dao.query.QueryBuilder qb = employeeDao.queryBuilder();
                qb.where(qb.and(EmployeeDao.Properties.Name.eq("Smile1"),
                        EmployeeDao.Properties.Age.between(-2, -1),
                        EmployeeDao.Properties.Hired.eq(false)));
                qb.build();
                List<Employee> list = qb.list();
                int count = list.size();
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
