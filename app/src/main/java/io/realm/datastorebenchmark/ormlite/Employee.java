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

package io.realm.datastorebenchmark.ormlite;

import android.support.annotation.NonNull;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.sql.SQLException;

@DatabaseTable(tableName = Employee.TABLE_NAME)
public class Employee {

    public static final String TABLE_NAME = "employee";

    private static Dao<Employee, Long> sDao;

    @DatabaseField(columnName = "_id", generatedId =  true)
    private long id;

    @DatabaseField(columnName = "name", dataType = DataType.STRING)
    private String name;

    @DatabaseField(columnName = "age", dataType = DataType.LONG)
    private long age;

    @DatabaseField(columnName = "hired", dataType = DataType.BOOLEAN)
    private boolean hired;

    public Employee() {
    }

    public boolean isHired() {
        return hired;
    }

    public void setHired(boolean hired) {
        this.hired = hired;
    }

    public long getAge() {
        return age;
    }

    public void setAge(long age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public static synchronized Dao<Employee, Long> getDao() {
        if(sDao == null) {
            try {
                sDao = DataBaseHelper.getInstance().getDao(Employee.class);
            } catch (SQLException e) {
                return null;
            }
        }
        return sDao;
    }
}
