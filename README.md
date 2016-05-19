# Purpose

There are lies, damn lies and benchmarks. This is an attempt of the latter.

Benchmarks are just inherently difficult to make generic and really useful. We
would always recommend users to do their own benchmarks for the particular
use-cases that's important to them. So go do that - after reading below!

Done properly, it can still be relatively useful as a rough indicator of areas
to investigate further. For developers it's also needed to be aware of
performance regressions.

This is a benchmarking suite to compare Realm to SQLite (and possibly its
derived ORMs) in a series of common and simple operations:

 * Batch Writes : write a bunch of objects in a single transaction.
 * Simple Writes : write an object in a single transaction.
 * Simple Queries : perform a simple query filter on:
     - int equality
     - string equality
     - int range
 * Full Scan : perform a simple query that doesn't match any results (= a full
   table scan).
 * Sum : sum the values of an int field
 * Count : count the number of entries in a table.
 * Delete: delete all entries in a table.

# The Difficulties of Benchmarking

Benchmarking is hard. Micro-benchmarking is even harder. Micro-benchmarking Java
even more so.

Here are some of the common problems that have to be taken into consideration:


## System Induced Variance

### Garbage Collection

Java is executed in a virtual machine, which is in charge of all the resources,
including memory and garbage collection. This means that there will be several
fluctuations in the measured timings, and even outliers in case the garbage
collection is executed during the test.

To alleviate such issue there are a couple of options.

 * Using a robust algorithm to eliminate outliers, such as [RANSAC](https://en.wikipedia.org/wiki/RANSAC).
 * Measure multiple times **using the median value** among the measured ones.

The second option is much easier to implement and takes care of any fluctuation
and outlying measurements, so it's the one being used in this suite.

It is important to notice that the average is not a good option since it is
influenced in a linear way from the outliers and median, while being better than
average, is still influenced in a linear way.


### JIT

If tests are being performed in a virtual machine that relies on just in time
compilation (such as Dalvik or Art on Android N) the first many runs of the tests
will be influenced by the JIT execution. A way to alleviate this problem is to
**perform some warm-up runs before starting to measure**.

### Issues with Measuring Time

Some care has to be taken while measuring time with system utilities:

 * `System.currentTimeMillis()` is not guaranteed to be monotonic. This makes it
    useless for any benchmarking use.
 * `System.nanoTime()` is indeed guaranteed to be monotonic (at least on
    Android) but it comes with a caveat: two reads can provide the exact same
    number. This is due to the fact that such method provides nanosecond
    precision, but not nanosecond resolution. In order to alleviate this issue
    it is recommended to **invalidate any run where the start and end time are
    the same and measure code that takes a reasonable time above the time
    resolution**.

### CPU speed
  
Many modern devices has CPU's that saves power by reducing the speed of the CPU
and only increasing the speed when there is a need. For that reason it's also a
good idea to warm-up the CPU.


## User Induced Variance

It is extremely easy to influence the measurement of a benchmark if not enough
attention is paid to some details:

 * Always **run the tests sequentially** and never in parallel: this will lower
   the variance induced by the OS scheduler, the JVM scheduler and inter-process
   and inter-thread interactions.
 * Be aware of the cost of method calls, since they might add a constant
   overhead that needs to be taken into consideration.
 * And of course always try to be fair in the comparison and actually compare
   apples to apples.


## Implicit or explicit transactions

SQLite and Realm's object store are both using implicit read transaction. That
is, you don't have to embed read operations and queries in transactions. Of
course, when inserting, updating or removing rows or objects from either
SQLite or Realm, explicit write transactions are used. The implicit read
transactions are not directly measured.

The `in.realm.internal` is also included in the benchmark under the name Realm
Low Level. This benchmark is used internally by Realm to measure the overhead
of the object store. As `io.realm.internal` is using explicit read
transactions, in some cases `io.realm.internal` will appear slower.


## How to Run

The benchmark is mostly self-contained. Only the number of objects/rows can be
varied. In file `MainActivity.java`, the constant `NUMBER_OF_OBJECTS` is exactly
that parameter. The default is 1,000.


## How to Analyze

The Python script `tools/dsb.py` can be used to analyze and visualize the
benchmark data. The script assumes that the raw data for runs are saved in a
subfolder named after the number of objects in the test, e.g "1000".

You can validate the results by running the scripts as `tools/dsb.py -p -v`.
Validation generates two different types of files:

1. All measurement as function of time/iteration. Android/Java can have strange
   spikes (due to JIT, GC, and implementation of `System.nanoTime()`), and this
   type of graph gives you an idea if your raw data fluctuate much. You find the
   graphs as `<NUMBER_OF_OBJECTS>/raw_<DATASTORE>_<TEST>.png`.
2. Histogram (10 bins) is calculated so you can see if the raw data is grouped.
   You find the histograms as `<NUMBER_OF_OBJECTS>/hist_<DATASTORE>_<TEST>.png`.

You can get a plain text report by running `tools/dsb.py -a`. This will
calculate mimimum, average, maximum, and other values.

By running the script as `tools/dsb.py -s` relative speed up compared to SQLite
is reported. If SQLite is faster, a negative number is reported. You find the
graphs as `<NUMBER_OF_OBJECTS>/speedup.png`. The median for each datastore is
used to determine the speedup.

Running the script with `tools/dsb.py -b` will generate benchmark plots for the
different benchmark tasks. You find the graphs as `benchmark_<test>.png`. The
output is a box-and-whisker plot using an IQR of 1.5 and with hidden outliers.
For more information about how to interpret a box plot, read
http://www.purplemath.com/modules/boxwhisk.htm.


## How to analyse - TLDR version

This describes how to benchmark the different datastores supported by this test
suite.

1. Set NUMBER_OF_OBJECTS in `app/src/main/java/io.realm.datastorebenchmark.MainActivity.java`.
   The rest of this guide assumes `1000` which is the default.

2. Deploy the app to the phone or emulator. It will auto-start and report *Done*
   in the UI when complete. Don't touch the phone while it is running.

       > ./gradlew installDebug
      
       > adb shell am start -a android.intent.action.MAIN -n io.realm.datastorebenchmark/io.realm.datastorebenchmark.MainActivity

3. Goto the`./tools` folder.

       > cd tools

4. Copy all results from the phone/emulator using ADB to folder named after
   NUMBER_OF_OBJECTS:

       > adb pull /sdcard/datastorebenchmark/ ./1000

5. Run python script:

       > python dsb.py -p -v -s -b

6. The script will generate a number of plots:

    * The benchmarks plots can be found in `./1000/benchmark_<test>.png`.
    * The speedup plot can be found in `./1000/speedup.png`.
    * The histograms can be found in `./1000/hist_<datastore>_<test>.png`
    * The raw plots can be found in `./1000/raw_<datastore>_<test>.png`.
