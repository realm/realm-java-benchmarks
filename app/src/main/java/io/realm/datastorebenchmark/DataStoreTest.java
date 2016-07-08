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

package io.realm.datastorebenchmark;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class DataStoreTest {
    protected Context context;
    protected Map<String, List<Long>> measurements;
    protected final long numberOfObjects;
    protected final long warmupIterations;
    protected final long testIterations;

    protected DataGenerator dataGenerator;

    protected final String TEST_SIMPLE_QUERY      = "SimpleQuery"; // Do a simple query and read 1 field
    protected final String TEST_SIMPLE_WRITE      = "SimpleWrite"; // Insert a single object inside a transaction
    protected final String TEST_BATCH_WRITE       = "BatchWrite";  // Insert multiple objects inside a transaction
    protected final String TEST_SUM               = "Sum";         // Calculate the sum of a field on all objects
    protected final String TEST_COUNT             = "Count";       // Count all objects in a table
    protected final String TEST_FULL_SCAN         = "FullScan";    // Do a query that returns no results (= has to scan all elements)
    protected final String TEST_DELETE            = "Delete";      // Delete all object in a table

    private String keys[] = { TEST_BATCH_WRITE, TEST_SIMPLE_WRITE, TEST_SIMPLE_QUERY,
            TEST_FULL_SCAN, TEST_COUNT, TEST_SUM, TEST_DELETE };

    public DataStoreTest(Context context, long numberOfObjects, long warmupIterations, long testIterations) {
        Log.i("DataStoreBenchmark", this.getClass().getName().toString());
        this.context = context;
        this.measurements = new HashMap<>();
        this.numberOfObjects = numberOfObjects;
        this.warmupIterations = warmupIterations;
        this.testIterations = testIterations;
        dataGenerator = new DataGenerator();
        dataGenerator.initNames();
    }

    protected void setUp() {}

    protected void tearDown() {
        System.gc();
    }

    public void allTests() {
        Class clazz = this.getClass();
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            String name = method.getName();
            if (name.startsWith("test")) {
                try {
                    Log.i("DataStoreBenchmark", "invoking " + getTag() + ":" + method.getName());
                    method.invoke(this);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void saveHeader(String filePrefix) {
        try {
            File file = new File(Environment.getExternalStorageDirectory() + "/datastorebenchmark", filePrefix + ".csv");
            FileOutputStream fileOutputStream = new FileOutputStream(file, false);
            fileOutputStream.write("datastore;".getBytes());
            for (String key : keys) {
                fileOutputStream.write(String.format("%s (min);%s (max);%s (median);", key, key, key).getBytes());
            }
            fileOutputStream.write("\n".getBytes());
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveMeasurements(String filePrefix) {
        String tag = getTag();
        try {
            // one file per test with raw data
            for (String key : keys) {
                File file1 = new File(Environment.getExternalStorageDirectory() + "/datastorebenchmark", tag + "_" + key + ".csv");
                FileOutputStream fileOutputStream1 = new FileOutputStream(file1, false);
                List<Long> measurement = measurements.get(key);
                for (int i = 0; i < measurement.size(); i++) {
                    fileOutputStream1.write(String.format("%d\n", measurement.get(i).longValue()).getBytes());
                }
                fileOutputStream1.close();
            }

            // combined CSV file
            File file = new File(Environment.getExternalStorageDirectory() + "/datastorebenchmark", filePrefix + ".csv");
            FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            fileOutputStream.write(String.format("%s;", tag).getBytes());
            for (String key : keys) {
                List<Long> measurement = measurements.get(key);
                double[] doubles = new double[measurement.size()];
                for (int i = 0; i < measurement.size(); i++) {
                    doubles[i] = measurement.get(i).doubleValue();
                }
                Collections.sort(measurement);
                DescriptiveStatistics results = new DescriptiveStatistics(doubles);
                fileOutputStream.write(String.format("%e;%e;%e;", results.getMin(), results.getMax(), results.getMean()).getBytes());
            }
            fileOutputStream.write("\n".getBytes());
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public abstract void testSimpleWrite();
    public abstract void testSimpleQuery();
    public abstract void testBatchWrite();
    public abstract void testFullScan();
    public abstract void testDelete();
    public abstract void testSum();
    public abstract void testCount();
    protected abstract String getTag();
}