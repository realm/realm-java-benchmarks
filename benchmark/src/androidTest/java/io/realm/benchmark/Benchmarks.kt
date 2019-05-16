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
package io.realm.benchmark

import androidx.benchmark.BenchmarkRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Base class for all benchmarks to ensure that we are running all tests for all libraries
 * being benchmarked.
 *
 * It is still up to each implementation to use `size` correctly.
 */
@RunWith(Parameterized::class)
abstract class Benchmarks(val size: Long) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "size={0}")
        fun data(): Array<Long> {
            return arrayOf(10, 100, 1000);
        }
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    val dataGenerator = DataGenerator()

    @Before abstract fun before()
    @After abstract fun after()
    @Test abstract fun simpleQuery()
    @Test abstract fun simpleWrite()
    @Test abstract fun batchWrite()
    @Test abstract fun fullScan()
    @Test abstract fun delete()
    @Test abstract fun sum()
    @Test abstract fun count()
}