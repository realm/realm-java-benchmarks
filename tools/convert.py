import json
import os
import re
import sys

# Read output from Android benchmarks and convert it to the output we used in our own benchmarks
# This script requires that the benchmark file is placed next to the script
with open('io.realm.benchmark.test-benchmarkData.json', 'r') as benchmarkResults:
    # parse file
    results = json.loads(benchmarkResults.read())

    for test in results['results']:
        testMethod = re.search('(EMULATOR_)?(.*)\\[.*\\]', test['name']).group(2)
        testSize = re.search('.*\\[size=(.*)\\]', test['name']).group(1).replace(',', '').replace('.', '')
        library = re.search('io\\.realm\.benchmark\\.(.*)Benchmarks', test['classname']).group(1).lower()

        if not os.path.exists(testSize):
            os.mkdir(testSize)
        output = open(str(testSize) + '/' + library + '_' + testMethod + ".csv", "w")
        for run in test['runs']:
            output.write(str(run))
            output.write('\n')
        output.close()
