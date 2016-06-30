package io.realm.datastorebenchmark;

/**
 * Benchmark configuration.
 */
public class Constants {
    public static final String OUTPUT_FOLDER = "datastorebenchmark";
    public static final String TESTFILE_PREFIX = "datastore";

    // Number of iterations should be high enough to include any warmup period. The worst outliers
    // will be filtered out by only plotting the inter quartile range and the median will not be
    // effected either way.

    // Light test (2+ minutes) - default
    public static final long NUMBER_OF_ITERATIONS = 100;
    public static final long NUMBER_OF_OBJECTS = 1000;

    // Medium test (7+ minutes)
//    public static final long NUMBER_OF_ITERATIONS = 25;
//    public static final long NUMBER_OF_OBJECTS = 10000;

    // Long test (15+ minutes)
//    public static final long NUMBER_OF_ITERATIONS = 5;
//    public static final long NUMBER_OF_OBJECTS = 100000;
}
