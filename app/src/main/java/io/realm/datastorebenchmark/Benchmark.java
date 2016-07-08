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

import android.os.Debug;

import java.util.ArrayList;
import java.util.List;

public abstract class Benchmark {

    private List<Long> timings;
    private long timeStart;

    public Benchmark() {
        timings = new ArrayList<>();
    }

    /**
     * Setting up the benchmark. This method will only be called once before all the warmup/test iterations.
     */
    public abstract void setUp();

    /**
     * Tear down any structures used by the benchmark. This method will only be called once all warmup/test iterations
     * are completed.
     */
    public abstract void tearDown();

    /**
     * Override this method with your test.
     */
    public abstract void run();

    /**
     * Initialize any data used by a single benchmark run.
     */
    protected void prepareRun() {

    }

    /**
     * Cleanup any temporary data created during a single run of the benchmark, e.g inserted objects can be removed.
     */
    protected void cleanupRun() {

    }

    public List<Long> execute(long warmupIterations, long numberOfIterations) {
        setUp();

        for (int i = 0; i < warmupIterations; i++) {
            prepareRun();
            System.gc();
            run();
            cleanupRun();
        }

        for (int i = 0; i < numberOfIterations; i++) {
            prepareRun();
            System.gc();
            startTimer();
            run();
            stopTimer();
            cleanupRun();
        }

        tearDown();
        return timings;
    }

    /**
     * Starts the timer
     */
    public void startTimer() {
        timeStart = Debug.threadCpuTimeNanos();
    }

    /**
     * Stops the timer
     */
    public void stopTimer() {
        long timeStop = Debug.threadCpuTimeNanos();
        long duration = timeStop - timeStart; // may report 0
        timings.add(duration);
    }
}
