/*
 * Copyright 2019 Realm Inc.
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
package io.realm.benchmark.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface EmployeeDao {

    @Insert
    fun write(obj: Employee)

    @Insert
    fun bulkWrite(list: List<Employee>)

    @Query("SELECT * FROM Employee WHERE hired = 0 AND age BETWEEN 20 AND 50 AND name = 'Foo0'")
    fun simpleQuery(): List<Employee>

    @Query("SELECT * FROM Employee WHERE hired = 1 AND age BETWEEN -2 AND -1 AND name == 'Smile1'")
    fun fullScan(): List<Employee>

    @Query("DELETE FROM Employee")
    fun deleteAll()

    @Query("SELECT SUM(age) AS sum FROM Employee")
    fun sum(): Long

    @Query("SELECT COUNT(*) AS count FROM Employee")
    fun count(): Long

}
