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

import sys, getopt
import numpy as np
import os.path
import matplotlib.pylab as pylab
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches

xkcdStyle = False # plot as Randall Munroe

# List of names of possible tested data stores (see DataStoreTest#getTag())
datastores = ['sqlite', 'realm', 'ormlite', 'greendao', 'realmlowlevel', 'sugarorm', 'couchbase']

# Number of objects tested. It is assumed that all sub folders contain test data and are named after the number of
# objects tested, e.g "./1000" or "./10000".
datasizes = []

# The individual benchmarks (see TestDataStore.java)
tests = ['BatchWrite', 'SimpleWrite', 'SimpleQuery', 'FullScan', 'Sum', 'Count', 'Delete']

# read the timer resolution
def readTimer(datasize):
    inFileName = str(datasize) + '/timer'
    values = [int(line.strip()) for line in open(inFileName)]
    return values

# read the raw data for a given test
def readRawValues(datasize, datastore, test):
    inFileName = str(datasize) + '/' + datastore + '_' + test + '.csv'
    return [int(line.strip()) for line in open(inFileName)]

# returns number of bogus timings and list of real timings
def readValues(datasize, datastore, test):
    values = sorted(readRawValues(datasize, datastore, test))
    timings = [x for x in values if x > 0] # remove bogus values
    return (len(values) - len(timings), timings)

# was a datastore benchmarked?
def datastoreBenchmarked(datasize, datastore):
    return os.path.exists(str(datasize) + '/' + datastore + '_Sum.csv')

def benchmark(datasize):
    x = []
    y = []
    c = []
    patches = []
    colors = ['green', 'blue', 'red']
    if xkcdStyle:
        plt.xkcd()
    plt.figure()
    plt.title('Data stores comparison')
    plt.ylabel('Operations/time')
    plt.tick_params(axis='both', which='both',
        bottom='off', top='off')

    n = 0
    for datastore in datastores:
        if datastoreBenchmarked(datasize, datastore):
            patches.append(mpatches.Patch(color=colors[n % len(tests)], label=datastore))
            n = n + 1

    for test in tests:
        n = 0
        for datastore in datastores:
            if datastoreBenchmarked(datasize, datastore):
                (_, values) = readValues(datasize, datastore, test)
                if len(values) == 0:
                    y.append(0)
                else:
                    median = np.median(values)
                    if test == 'BatchWrite':
                        median = median / float(datasize)
                    y.append(1.0e9/median)
                x.append(test.replace('Simple', ''))
                c.append(colors[n % len(tests)])
                n = n + 1

    plt.legend(bbox_to_anchor=(1, 1), bbox_transform=plt.gcf().transFigure, handles=patches)
    t = [test.replace('Simple', '') for test in tests]
    rects = plt.bar(np.arange(len(y)), y, color=c)
    plt.xticks(np.arange(1, len(datastores)*len(tests), len(datastores)), t)
    outFileName = str(datasize) + '/timings.png'
    plt.savefig(outFileName)
    plt.close()

def validate(datasize):
    for test in tests:
        for datastore in datastores:
            if datastoreBenchmarked(datasize, datastore):
                pylab.figure()
                pylab.title(datastore + ':' + test)
                pylab.xlabel('Value')
                pylab.ylabel('Count')
                (_, values) = readValues(datasize, datastore, test)
                if len(values) > 1:
                    n, bins, patches = pylab.hist(values, 10, histtype='bar', label=datastore)
                    pylab.legend()
                    outFileName = str(datasize) + '/hist_' + datastore + '_' + test + '.png'
                    pylab.savefig(outFileName)
                else:
                    print('Cannot generate histogram for ' + datastore + ':' + test + ' for size ' + str(datasize))
                pylab.close()

def plotraw(datasize):
    timer_res = readTimer(datasize)[2] # 3rd line
    for test in tests:
        for datastore in datastores:
            if datastoreBenchmarked(datasize, datastore):
                if xkcdStyle:
                    plt.xkcd()
                plt.figure()
                plt.title(datastore + ':' + test)
                plt.xlabel('Iteration')
                plt.ylabel('time [ns]')
                values = readRawValues(datasize, datastore, test)
                if len(values) > 0:
                    plt.plot(values)
                    plt.axhline(timer_res, color='red')
                    plt.savefig(str(datasize) + '/raw_' + datastore + '_' + test + '.png')
                plt.close()

def analyze(datasize):
    print('Analyzing size,' + str(datasize))
    timer_res = readTimer(datasize)[2] # 3rd line
    print('Timer resolution,' + str(timer_res))
    print('Data store,Test,minimum,average,maximum,bogus,real')
    for datastore in datastores:
        if datastoreBenchmarked(datasize, datastore):
            for test in tests:
                (bogus, timings) = readValues(datasize, datastore, test)
                ops = []
                if test.endswith('Write'):
                    ops = [10e9*float(datasize)/float(t) for t in timings]
                else:
                    ops = [10e9/float(t) for t in timings]
                minimum = ops[-1]
                maximum = ops[0]
                average = sum(ops)/float(len(ops))
                row = [datastore, test, str(minimum), str(average), str(maximum), str(bogus), str(len(timings))]
                print ','.join(row)

def speedup(datasize):
    dstores = [s for s in datastores if s != 'sqlite']
    print('Data size = ', datasize)

    # Read and categorize all test data into bucketes for each test type
    testdata = {} # Map between datastore and tests
    for test in tests:
        (_, timings) = readValues(datasize, 'sqlite', test)
        sqlite = np.median(timings)
        data = []
        colors = []
        for ds in dstores:
            (_, values) = readValues(datasize, ds, test)
            value = 0
            if len(values) > 0:
                value = np.median(values)
                speedup = 0.0
                if value < sqlite:
                    speedup = sqlite / value
                else:
                    speedup = -value / sqlite
            else:
                speedup = 0

            if not ds in testdata.keys():
                testdata[ds] = { "data": [] }

            testdata[ds]['data'].append(speedup)
            print('  datastore = ', ds, ' test = ', test, ' speedup = ', speedup)


    # Plot all data: Group each datastore for each benchmark
    fig, ax = plt.subplots()
    if xkcdStyle:
        plt.xkcd()

    plt.title(str(datasize) + ' rows/objects')
    plt.ylabel('Speed up')
    plt.xlabel('Test')

    i = 0
    legend_bar = []
    legend_labels = []
    for key in testdata:
        N = len(testdata[key]['data'])
        data = testdata[key]['data']
        ind = np.arange(N)
        width = 0.35
        c = 'yellow'
        if (key == 'realmlowlevel'):
            c = 'blue'
        elif (key == 'realm'):
            c = 'green'

        type = ax.bar(ind + (width * i), data, width, color=c)
        autolabel(type, data, ax)
        legend_bar.append(type[0])
        legend_labels.append(key)
        i = i + 1

    ax.set_xticks(ind + width)
    ax.set_xticklabels(tests)
    plt.legend(legend_bar, legend_labels)

    outFileName = str(datasize) + '/speedup.png'
    plt.savefig(outFileName)
    plt.close()

# attach some text labels
def autolabel(rects, data, ax):
    i = 0
    for rect in rects:
        height = rect.get_height()
        val = data[i]
        mod = 1
        if (val < 0):
            mod = 0
        ax.text(rect.get_x() + rect.get_width()/2., (height + 0.) * mod,
                '%2.2f'%float(val),
                ha='center', va='bottom')
        i = i + 1

def usage():
    print('dsb.py [-h] [-d <dir>] [-b] [-s] [-v] [-a] [-p] [-e <engine>] [-t test]')
    print(' -d <dir>    : only analyze these directories')
    print(' -v          : validate')
    print(' -a          : analyze')
    print(' -s          : speed up graphs compared to raw SQLite (Requires SQLite test).')
    print(' -e <engine> : only these engines')
    print(' -p          : plot raw data')
    print(' -x          : XKCD style graphs')
    print(' -t <test>   : only these tests')
    print(' -h          : this message')

def main(argv):
    global datasizes
    global datastores
    global tests
    global xkcdStyle

    xkcdStyle = False

    # Automatically detect folders that should contain benchmark results
    for dir in os.listdir('.'):
        if os.path.isdir(dir) and unicode(dir, "utf-8").isnumeric():
            datasizes.append(dir)

    # Automatically detect which data stores have been tested
    # We assume that all benchmark folders have the results from the same data stores
    tested_datastores = set()
    if len(datasizes) > 0:
        for file in os.listdir("./" + datasizes[0]):
            fileprefix = file.split('_')[0]
            if fileprefix in datastores and fileprefix not in tested_datastores:
                tested_datastores.add(fileprefix)
    datastores = list(tested_datastores)

    # Configure plots
    font = { 'weight' : 'normal', 'size' : 8 }
    plt.rc('font', **font)

    do_analyze = False
    do_validate = False
    do_speedup = False
    do_plotraw = False
    do_benchmark = False

    try:
        optlist, args = getopt.getopt(argv, 'h:d:bvsape:xt:')
    except getopt.GetoptError:
        usage()
        sys.exit(2)
    for opt, arg in optlist:
        if opt == '-d':
            arr = arg.split(',')
            if len(arr) == 1:
                datasizes = [arg]
            else:
                datasizes = arr
        elif opt == '-x':
            xkcdStyle = True
        elif opt == '-v':
            do_validate = True
        elif opt == '-a':
            do_analyze = True
        elif opt == '-s':
            do_speedup = True
        elif opt == '-p':
            do_plotraw = True
        elif opt == '-e':
            arr = arg.split(',')
            if len(arr) == 1:
                datastores = [arr]
            else:
                datastores = arr
        elif opt == '-t':
            arr = arg.split(',')
            if len(arr) == 1:
                tests = [arr]
            else:
                tests = arr
        elif opt == '-b':
            do_benchmark = True
        elif opt == '-h':
            usage()
            sys.exit(0)
        else:
            usage()
            sys.exit(2)

    if do_analyze:
        for datasize in datasizes:
            analyze(datasize)

    if do_validate:
        for datasize in datasizes:
            validate(datasize)

    if do_speedup:
        for datasize in datasizes:
            speedup(datasize)

    if do_plotraw:
        for datasize in datasizes:
            plotraw(datasize)

    if do_benchmark:
        for datasize in datasizes:
            benchmark(datasize)

if __name__ == '__main__':
    main(sys.argv[1:])
