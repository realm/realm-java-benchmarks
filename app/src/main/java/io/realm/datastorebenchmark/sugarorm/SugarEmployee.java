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

package io.realm.datastorebenchmark.sugarorm;

import com.orm.SugarRecord;

public class SugarEmployee extends SugarRecord<SugarEmployee> {

    private long eid;  // id is used internally by Sugar ORM
    private String name;
    private long age;
    private boolean hired;

    public SugarEmployee() {}

    public SugarEmployee(long eid, String name, long age, boolean hired) {
        this.eid = eid;
        this.name = name;
        this.age = age;
        this.hired = hired;
    }

    public void setId(long id) {
        this.eid = eid;
    }

    public long getEid() {
        return eid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getAge() {
        return age;
    }

    public void setAge(long age) {
        this.age = age;
    }

    public boolean getHired() {
        return hired;
    }

    public void setHired(boolean hired) {
        this.hired = hired;
    }
}
