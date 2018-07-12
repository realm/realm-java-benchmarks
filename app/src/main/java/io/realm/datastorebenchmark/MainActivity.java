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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import io.realm.datastorebenchmark.tests.TestGreenDao;
import io.realm.datastorebenchmark.tests.TestOrmLite;
import io.realm.datastorebenchmark.tests.TestRealm;
import io.realm.datastorebenchmark.tests.TestSQLite;
import io.realm.datastorebenchmark.tests.TestCouch;

import static io.realm.datastorebenchmark.Constants.*;


public class MainActivity extends AppCompatActivity {

    final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    private Handler handler = new Handler();
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusView = (TextView) findViewById(R.id.textView);
        requestWritePermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    runBenchmarks();
                } else {
                    statusView.setText("Cannot run the benchmarks without write permission!");
                }
            }
        }
    }

    private void requestWritePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            runBenchmarks();
        }
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

    private void runBenchmarks() {

        // Does our preferred timer work?
        checkCpuTimeNanos();

        // Create folder used to store benchmark results.
        final File outputFolder = createOutputDirectory();

        // resolution/granularity of timer
        measureTimerResolution(outputFolder);

        // Configure which tests to run
        Context context = getApplicationContext();
        final List<DataStoreTest> tests = Arrays.asList(
                new TestSQLite(context, NUMBER_OF_OBJECTS, WARMUP_ITERATIONS, NUMBER_OF_ITERATIONS), // Required for Speedup graphs
                new TestRealm(context, NUMBER_OF_OBJECTS, WARMUP_ITERATIONS, NUMBER_OF_ITERATIONS),
                new TestOrmLite(context, NUMBER_OF_OBJECTS, WARMUP_ITERATIONS, NUMBER_OF_ITERATIONS),
                new TestGreenDao(context, NUMBER_OF_OBJECTS, WARMUP_ITERATIONS, NUMBER_OF_ITERATIONS),
                new TestCouch(context, NUMBER_OF_OBJECTS, WARMUP_ITERATIONS, NUMBER_OF_ITERATIONS)
        );

        // Run tests
        statusView.setText("Running benchmarks...");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean first = true;
                for (DataStoreTest test : tests) {
                    Log.i(test.getClass().getName(), "Starting..."  );
                    test.allTests();
                    if (first) {
                        test.saveHeader(TESTFILE_PREFIX);
                        first = false;
                    }
                    test.saveMeasurements(TESTFILE_PREFIX);
                }
                statusView.setText("All benchmarks done. Results in " + outputFolder.getAbsolutePath());
                Log.i(this.getClass().getName(), "Testing Done.");
            }
        }, 800);
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
