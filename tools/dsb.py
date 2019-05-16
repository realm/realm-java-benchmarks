#!/usr/local/bin/python
#
# Copyright 2016 Realm Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""This script is a helper script for analyzing benchmark data. See README.md for more info."""

import sys
import getopt
import os.path
import numpy as np
import matplotlib.pylab as pylab
import matplotlib.pyplot as plt

# plot as Randall Munroe
XKCD_STYLE = False

# List of names of possible tested data stores (see DataStoreTest#getTag())
DATASTORES = ['sqlite', 'realm', 'room']
# Number of objects tested. It is assumed that all sub folders contain test data and are named after
# the number of objects tested, e.g "./1000" or "./10000".
DATASIZES = ['10', '100', '1000']

# The individual benchmarks (see TestDataStore.java)
TESTS = ['batchWrite', 'simpleWrite', 'simpleQuery', 'fullScan', 'sum', 'count', 'delete']

def read_raw_values(datasize, datastore, test):
    """read the raw data for a given test"""
    in_file_name = str(datasize) + '/' + datastore + '_' + test + '.csv'
    return [int(line.strip()) for line in open(in_file_name)]

def read_values(datasize, datastore, test):
    """returns number of bogus timings and list of real timings"""
    values = sorted(read_raw_values(datasize, datastore, test))
    timings = [x for x in values if x > 0] # remove bogus values
    return (len(values) - len(timings), timings)

def datastore_benchmarked(datasize, datastore):
    """was a datastore benchmarked?"""
    return os.path.exists(str(datasize) + '/' + datastore + '_Sum.csv')

def benchmark(datasize):
    """Plot the output of each benchmark to its own file"""
    for test in TESTS:
        data = []
        labels = []
        DATASTORES.sort()
        for datastore in DATASTORES:
            labels.append(datastore)
            if datastore_benchmarked(datasize, datastore):
                (_, values) = read_values(datasize, datastore, test)
                if test == 'BatchWrite':
                    # Normalize batch measurement to 1 row
                    values = [val/float(datasize) for val in values]

                values = [1.0e9/val for val in values] # Convert to ops/sec
                data.append(values)

        if XKCD_STYLE:
            plt.xkcd()

        # Plot API: http://matplotlib.org/1.3.1/api/pyplot_api.html
        # Box-and-Whisker plot with an IQR of 1.5 and hidden outliers.
        plt.title('Data stores comparison - ' + test)
        plt.ylabel('Ops/sec')
        plt.boxplot(data, whis=1.5, sym='')
        plt.xticks(np.arange(1, len(labels) + 1), labels)
        plt.ylim(ymin=0)
        out_file_name = str(datasize) + '/benchmark_' + test +'.png'
        plt.savefig(out_file_name)
        plt.close()

def plot_histogram(datasize):
    """Plot histograms for the benchmark data"""
    for test in TESTS:
        for datastore in DATASTORES:
            if datastore_benchmarked(datasize, datastore):
                pylab.figure()
                pylab.title(datastore + ':' + test)
                pylab.xlabel('Value')
                pylab.ylabel('Count')
                (_, values) = read_values(datasize, datastore, test)
                if len(values) > 1:
                    pylab.hist(values, 10, histtype='bar', label=datastore)
                    pylab.legend()
                    out_file_name = str(datasize) + '/hist_' + datastore + '_' + test + '.png'
                    pylab.savefig(out_file_name)
                else:
                    print 'Can\'t generate histogram for {0}: {1} for size {2}'.format(datastore,
                                                                                       test,
                                                                                       datasize)
                pylab.close()

def plot_raw(datasize):
    """Plot the raw benchmark data"""
    for test in TESTS:
        for datastore in DATASTORES:
            if datastore_benchmarked(datasize, datastore):
                if XKCD_STYLE:
                    plt.xkcd()
                plt.figure()
                plt.title(datastore + ':' + test)
                plt.xlabel('Iteration')
                plt.ylabel('time [ns]')
                values = read_raw_values(datasize, datastore, test)
                if len(values) > 0:
                    plt.plot(values)
                    plt.savefig(str(datasize) + '/raw_' + datastore + '_' + test + '.png')
                plt.close()

def analyze(datasize):
    """Analyze the raw benchmark data"""
    print 'Analyzing size,' + str(datasize)
    print 'Data store,Test,minimum,average,maximum,bogus,real'
    for datastore in DATASTORES:
        if datastore_benchmarked(datasize, datastore):
            for test in TESTS:
                (bogus, timings) = read_values(datasize, datastore, test)
                ops = []
                if test.endswith('Write'):
                    ops = [10e9*float(datasize)/float(t) for t in timings]
                else:
                    ops = [10e9/float(t) for t in timings]
                minimum = ops[-1]
                maximum = ops[0]
                average = sum(ops)/float(len(ops))
                row = [datastore, test, str(minimum), str(average), str(maximum), str(bogus),
                       str(len(timings))]
                print ','.join(row)

def plot_speedup(datasize):
    """Plot the speedup graph"""
    dstores = [s for s in DATASTORES if s != 'sqlite']
    print('Data size = ', datasize)

    # Read and categorize all test data into bucketes for each test type
    testdata = {} # Map between datastore and TESTS
    for test in TESTS:
        (_, timings) = read_values(datasize, 'sqlite', test)
        sqlite = np.median(timings)
        for store in dstores:
            (_, values) = read_values(datasize, store, test)
            speedup = 0
            if len(values) > 0:
                value = np.median(values)
                if value < sqlite:
                    speedup = sqlite / value
                else:
                    speedup = -value / sqlite

            if not store in testdata.keys():
                testdata[store] = {"data": []}

            testdata[store]['data'].append(speedup)
            print 'datastore = ', store, ' test = ', test, ' speedup = ', speedup


    # Plot all data: Group each datastore for each benchmark
    (_, axis) = plt.subplots()
    if XKCD_STYLE:
        plt.xkcd()

    plt.title(str(datasize) + ' rows/objects')
    plt.ylabel('Speed up')
    plt.xlabel('Test')

    i = 0
    orm_colors = ['yellow', 'orange']
    orm_index = 0
    legend_bar = []
    legend_labels = []
    for key in testdata:
        data_count = len(testdata[key]['data'])
        data = testdata[key]['data']
        ind = np.arange(data_count)
        width = 0.90 / len(dstores)
        if key == 'realmlowlevel':
            color = 'blue'
        elif key == 'realm':
            color = 'green'
        else:
            color = orm_colors[orm_index]
            orm_index = orm_index + 1

        barplot = axis.bar(ind + (width * i), data, width, color=color)
        autolabel(barplot, data, axis)
        legend_bar.append(barplot[0])
        legend_labels.append(key)
        i = i + 1

    axis.set_xticks(ind + width)
    axis.set_xticklabels(TESTS)
    plt.legend(legend_bar, legend_labels)

    out_file_name = str(datasize) + '/speedup.png'
    plt.savefig(out_file_name)
    plt.close()

def autolabel(rects, data, axis):
    """Format and add labels to the benchmark plots"""
    i = 0
    for rect in rects:
        height = rect.get_height()
        val = data[i]
        mod = 1
        if val < 0:
            mod = 0
        axis.text(rect.get_x() + rect.get_width()/2., (height + 0.1) * mod, '%2.2f'%float(val),
                  ha='center', va='bottom')
        i = i + 1

def usage():
    """Prints help and usage information for this script"""
    print 'dsb.py [-h] [-d <dir>] [-b] [-s] [-v] [-a] [-p] [-e <engine>] [-t test]'
    print ' -d <dir>    : only analyze these directories'
    print ' -v          : validate'
    print ' -a          : analyze'
    print ' -b          : plot benchmarks as ops/sec'
    print ' -s          : speed up graphs compared to raw SQLite (Requires SQLite test.'
    print ' -e <engine> : only these engines'
    print ' -p          : plot raw data'
    print ' -x          : XKCD style graphs'
    print ' -t <test>   : only these TESTS'
    print ' -h          : this message'

def main(argv):
    """Runs the script. Use `python dsb.py -h` for more information."""
    global DATASIZES
    global DATASTORES
    global TESTS
    global XKCD_STYLE

    # Automatically detect folders that should contain benchmark results
    for folder in os.listdir('.'):
        if os.path.isdir(folder) and unicode(folder, "utf-8").isnumeric():
            DATASIZES.append(folder)

    # Automatically detect which data stores have been tested
    # We assume that all benchmark folders have the results from the same data stores
    tested_datastores = set()
    if len(DATASIZES) > 0:
        for filename in os.listdir("./" + DATASIZES[0]):
            fileprefix = filename.split('_')[0]
            if fileprefix in DATASTORES and fileprefix not in tested_datastores:
                tested_datastores.add(fileprefix)
    DATASTORES = list(tested_datastores)

    # Configure plots
    font = {'weight' : 'normal', 'size' : 8}
    plt.rc('font', **font)

    do_analyze = False
    do_validate = False
    do_speedup = False
    do_plot_raw = False
    do_benchmark = False

    try:
        (optlist, _) = getopt.getopt(argv, 'h:d:bvsape:xt:')
    except getopt.GetoptError:
        usage()
        sys.exit(2)
    for opt, arg in optlist:
        if opt == '-d':
            arr = arg.split(',')
            if len(arr) == 1:
                DATASIZES = [arg]
            else:
                DATASIZES = arr
        elif opt == '-x':
            XKCD_STYLE = True
        elif opt == '-v':
            do_validate = True
        elif opt == '-a':
            do_analyze = True
        elif opt == '-s':
            do_speedup = True
        elif opt == '-p':
            do_plot_raw = True
        elif opt == '-e':
            arr = arg.split(',')
            if len(arr) == 1:
                DATASTORES = [arr]
            else:
                DATASTORES = arr
        elif opt == '-t':
            arr = arg.split(',')
            if len(arr) == 1:
                TESTS = [arr]
            else:
                TESTS = arr
        elif opt == '-b':
            do_benchmark = True
        elif opt == '-h':
            usage()
            sys.exit(0)
        else:
            usage()
            sys.exit(2)

    if do_analyze:
        for datasize in DATASIZES:
            analyze(datasize)

    if do_validate:
        for datasize in DATASIZES:
            plot_histogram(datasize)

    if do_speedup:
        for datasize in DATASIZES:
            plot_speedup(datasize)

    if do_plot_raw:
        for datasize in DATASIZES:
            plot_raw(datasize)

    if do_benchmark:
        for datasize in DATASIZES:
            benchmark(datasize)

if __name__ == '__main__':
    main(sys.argv[1:])
