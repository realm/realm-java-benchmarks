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

import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;

import io.realm.datastorebenchmark.tests.TestCouch;
import io.realm.datastorebenchmark.tests.TestGreenDAO;
import io.realm.datastorebenchmark.tests.TestLowlevelRealm;
import io.realm.datastorebenchmark.tests.TestOrmLite;
import io.realm.datastorebenchmark.tests.TestRealm;
import io.realm.datastorebenchmark.tests.TestSQLite;
import io.realm.datastorebenchmark.tests.TestSugarOrm;


public class MainActivity extends AppCompatActivity {

    final private long NUMBER_OF_ITERATIONS = 50;
    final private long NUMBER_OF_OBJECTS = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // does CPU timer work?
        if (Debug.threadCpuTimeNanos() == -1) {
            throw new RuntimeException("Debug.threadCpuTimeNanos() doesn't work.");
        }

        // create folder for output
        File directory = new File(Environment.getExternalStorageDirectory() + "/datastorebenchmark");
        directory.mkdirs();

        // resolution/granularity of timer
        // see http://gamasutra.com/view/feature/171774/getting_high_precision_timing_on_.php?print=1
        long diff = 50000; // very large value
        for (int i = 0; i < 100; i++) {
            long end;
            long start = Debug.threadCpuTimeNanos();
            while (true) {
                end = Debug.threadCpuTimeNanos();
                if (end != start) {
                    if (diff > (end - start)) {
                        diff = end - start;
                    }
                    break;
                }
            }
        }
        try {
            File file = new File(Environment.getExternalStorageDirectory() + "/datastorebenchmark", "timer");
            FileOutputStream fileOutputStream = new FileOutputStream(file, false);
            fileOutputStream.write(String.format("%d\n", NUMBER_OF_ITERATIONS).getBytes());
            fileOutputStream.write(String.format("%d\n", NUMBER_OF_OBJECTS).getBytes());
            fileOutputStream.write(String.format("%d\n", diff).getBytes());
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        String FILE_PREFIX = "datastore";

        TestRealm testRealm = new TestRealm(getApplicationContext(), NUMBER_OF_OBJECTS, NUMBER_OF_ITERATIONS);
        testRealm.allTests();
        testRealm.saveHeader(FILE_PREFIX); // first one print header too
        testRealm.saveMeasurements("realm", FILE_PREFIX);

        TestSQLite testSQLite = new TestSQLite(getApplicationContext(), NUMBER_OF_OBJECTS, NUMBER_OF_ITERATIONS);
        testSQLite.allTests();
        testSQLite.saveMeasurements("sqlite", FILE_PREFIX);

        TestLowlevelRealm testLowlevelRealm = new TestLowlevelRealm(getApplicationContext(), NUMBER_OF_OBJECTS, NUMBER_OF_ITERATIONS);
        testLowlevelRealm.allTests();
        testLowlevelRealm.saveMeasurements("realmlowlevel", FILE_PREFIX);

        TestOrmLite testOrmLite = new TestOrmLite(getApplicationContext(), NUMBER_OF_OBJECTS, NUMBER_OF_ITERATIONS);
        testOrmLite.allTests();
        testOrmLite.saveMeasurements("ormlite", FILE_PREFIX);

        TestSugarOrm testSugarOrm = new TestSugarOrm(getApplicationContext(), NUMBER_OF_OBJECTS, NUMBER_OF_ITERATIONS);
        testSugarOrm.allTests();
        testSugarOrm.saveMeasurements("sugarorm", FILE_PREFIX);

        TestCouch testCouch = new TestCouch(getApplicationContext(), NUMBER_OF_OBJECTS, NUMBER_OF_ITERATIONS);
        testCouch.allTests();
        testCouch.saveMeasurements("couchbase", FILE_PREFIX);

        TestGreenDAO testGreenDAO = new TestGreenDAO(getApplicationContext(), NUMBER_OF_OBJECTS, NUMBER_OF_ITERATIONS);
        testGreenDAO.allTests();
        testGreenDAO.saveMeasurements("greendao", FILE_PREFIX);

        setContentView(R.layout.activity_main);
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText("Done");
    }
}
