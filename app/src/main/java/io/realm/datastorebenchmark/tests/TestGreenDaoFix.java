package io.realm.datastorebenchmark.tests;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.dao.query.QueryBuilder;
import io.realm.datastorebenchmark.Benchmark;
import io.realm.datastorebenchmark.DataStoreTest;
import io.realm.datastorebenchmark.greendao.DaoMaster;
import io.realm.datastorebenchmark.greendao.DaoSession;
import io.realm.datastorebenchmark.greendao.Employee;
import io.realm.datastorebenchmark.greendao.EmployeeDao;

public class TestGreenDaoFix extends DataStoreTest {

    private long numberOfIterations;

    private SQLiteDatabase db;
    private DaoMaster daoMaster;
    private DaoSession daoSession;
    private EmployeeDao employeeDao;


    public TestGreenDaoFix(Context context, long numberOfObjects, long numberOfIterations) {
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
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(context, "EmployeeGreenDAO.db", null);
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
            public int index;

            @Override
            public void setUp() {
                index = 0;
            }

            @Override
            public void tearDown() {
                delete();
            }

            @Override
            public void run() {
                Employee employee = new Employee();
                employee.setName(dataGenerator.getEmployeeName(index));
                employee.setAge(dataGenerator.getEmployeeAge(index));
                employee.setHired(dataGenerator.getHiredBool(index));
                employeeDao.insert(employee);
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
            public void tearDown() {
                delete();
            }

            @Override
            public void run() {
                QueryBuilder qb = employeeDao.queryBuilder();
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
            }

            @Override
            public void tearDown() {
                delete();
            }

            @Override
            public void run() {
                List<Employee> data = new ArrayList<>();
                for (int i = 0; i < numberOfObjects; i++) {
                    Employee employee = new Employee();
                    employee.setName(dataGenerator.getEmployeeName(i));
                    employee.setAge(dataGenerator.getEmployeeAge(i));
                    employee.setHired(dataGenerator.getHiredBool(i));
                    data.add(employee);
                }
                employeeDao.insertInTx(data);
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
                addObjects();
                verify();
            }

            @Override
            public void tearDown() {
                delete();
            }

            @Override
            public void run() {
                QueryBuilder qb = employeeDao.queryBuilder();
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
        setUp();

        Benchmark benchmark = new Benchmark() {
            @Override
            public void setUp() {
                addObjects();
            }

            @Override
            public void tearDown() {
                delete();
            }

            @Override
            public void run() {
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
                addObjects();
                verify();
            }

            @Override
            public void tearDown() {
                delete();
            }

            @Override
            public void run() {
                // GreenDao doesn't support aggregate functions: https://github.com/greenrobot/greenDAO/issues/89
                Cursor cursor = employeeDao.getDatabase().rawQuery(
                        "SELECT SUM("+ EmployeeDao.Properties.Age.columnName +") AS sum FROM " + EmployeeDao.TABLENAME,
                        null
                );
                cursor.moveToFirst();
                long sum = cursor.getLong(0);
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
                addObjects();
                verify();
            }

            @Override
            public void tearDown() {
                delete();
            }

            @Override
            public void run() {
                long count = employeeDao.count();
            }
        };
        measurements.put(TEST_COUNT, benchmark.execute(numberOfIterations));

        tearDown();
    }

    @Override
    protected String getTag() {
        return "greendao";
    }
}
