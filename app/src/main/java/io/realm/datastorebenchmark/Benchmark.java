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
     * Setting up the benchmark
     */
    public abstract void setUp();

    /**
     * Tear down any structures used by the benchmark
     */
    public abstract void tearDown();

    /**
     * Override this method with your test.
     */
    public abstract void run();

    /**
     * Execute the microbenchmark
     */
    public List<Long> execute(long numberOfIterations) {
        setUp();

        for (int i = 0; i < numberOfIterations; i++) {
            System.gc();
            start();
            run();
            stop();
        }

        tearDown();

        return timings;
    }

    /**
     * Starts the timer
     */
    public void start() {
        timeStart = Debug.threadCpuTimeNanos();
    }

    /**
     * Stops the timer
     */
    public void stop() {
        long timeStop = Debug.threadCpuTimeNanos();
        long duration = timeStop - timeStart; // may report 0
        timings.add(duration);
    }
}
