package io.realm.datastorebenchmark.greendao;

import android.database.sqlite.SQLiteDatabase;

import java.util.Map;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.AbstractDaoSession;
import de.greenrobot.dao.identityscope.IdentityScopeType;
import de.greenrobot.dao.internal.DaoConfig;

import io.realm.datastorebenchmark.greendao.Employee;

import io.realm.datastorebenchmark.greendao.EmployeeDao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see de.greenrobot.dao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig employeeDaoConfig;

    private final EmployeeDao employeeDao;

    public DaoSession(SQLiteDatabase db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        employeeDaoConfig = daoConfigMap.get(EmployeeDao.class).clone();
        employeeDaoConfig.initIdentityScope(type);

        employeeDao = new EmployeeDao(employeeDaoConfig, this);

        registerDao(Employee.class, employeeDao);
    }
    
    public void clear() {
        employeeDaoConfig.getIdentityScope().clear();
    }

    public EmployeeDao getEmployeeDao() {
        return employeeDao;
    }

}
