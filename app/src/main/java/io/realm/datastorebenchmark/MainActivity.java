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
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import io.realm.datastorebenchmark.tests.TestGreenDAO;
import io.realm.datastorebenchmark.tests.TestLowlevelRealm;
import io.realm.datastorebenchmark.tests.TestOrmLite;
import io.realm.datastorebenchmark.tests.TestRealm;
import io.realm.datastorebenchmark.tests.TestSQLite;


public class MainActivity extends AppCompatActivity {

    // Benchmark configuration
    public static final String OUTPUT_FOLDER = "datastorebenchmark";
    private static final String TESTFILE_PREFIX = "datastore";
    private static final long NUMBER_OF_ITERATIONS = 50;
    private static final long NUMBER_OF_OBJECTS = 1000;

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final TextView statusView = (TextView) findViewById(R.id.textView);

        // Configure which tests to run
        Context context = getApplicationContext();
        final List<DataStoreTest> tests = Arrays.asList(
            new TestRealm(context, NUMBER_OF_OBJECTS, NUMBER_OF_ITERATIONS),
            new TestSQLite(context, NUMBER_OF_OBJECTS, NUMBER_OF_ITERATIONS),
            new TestLowlevelRealm(context, NUMBER_OF_OBJECTS, NUMBER_OF_ITERATIONS),
            new TestOrmLite(context, NUMBER_OF_OBJECTS, NUMBER_OF_ITERATIONS),
            new TestGreenDAO(context, NUMBER_OF_OBJECTS, NUMBER_OF_ITERATIONS)
//            new TestCouch(context, NUMBER_OF_OBJECTS, NUMBER_OF_ITERATIONS),
//            new TestSugarOrm(context, NUMBER_OF_OBJECTS, NUMBER_OF_ITERATIONS);
        );

        // Does our preferred timer work?
        checkCpuTimeNanos();

        // Create folder used to store benchmark results.
        File outputFolder = createOutputDirectory();

        // resolution/granularity of timer
        measureTimerResolution(outputFolder);

        // Run tests
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                for (DataStoreTest test : tests) {
                    test.allTests();
                    test.saveHeader(TESTFILE_PREFIX);
                    test.saveMeasurements(TESTFILE_PREFIX);
                }
                statusView.setText("Done");
            }
        }, 800);
    }

    /**
     * Verify that the timer mechanism is available.
     */
    private void checkCpuTimeNanos() {
        if (Debug.threadCpuTimeNanos() == -1) {
            throw new RuntimeException("Debug.threadCpuTimeNanos() doesn't work.");
        }
    }

    /**
     * Measure the resolution of the timer.
     * Any measurements close to this value is unreliable.
     */
    private void measureTimerResolution(File outputFolder) {
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
            File file = new File(outputFolder, "timer");
            FileOutputStream fileOutputStream = new FileOutputStream(file, false);
            fileOutputStream.write(String.format(Locale.US, "%d\n", NUMBER_OF_ITERATIONS).getBytes());
            fileOutputStream.write(String.format(Locale.US, "%d\n", NUMBER_OF_OBJECTS).getBytes());
            fileOutputStream.write(String.format(Locale.US, "%d\n", diff).getBytes());
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create the output directory that should hold all the test results. All previous results will be deleted.
     *
     * @return a reference to the created (and empty) directory.
     */
    private File createOutputDirectory() {
        File directory = new File(Environment.getExternalStorageDirectory() + "/" + OUTPUT_FOLDER);
        if (!directory.mkdirs() && !directory.exists()) {
            throw new IllegalStateException("Could not create output folder: " + directory);
        }
        try {
            FileUtils.cleanDirectory(directory);
        } catch (IOException e) {
            throw new IllegalStateException("Could not clear the output folder", e);
        }
        return directory;
    }
}
