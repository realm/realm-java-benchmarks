#!/bin/bash
# Bash helper script for copying all test data from a device to the proper dir and run the analyzer script

if [ "$#" -ne 1 ]; then
    echo 'Usage: copy_and_run.sh <number_of_objects>'
    exit 1
fi

rm -R -f $1
adb pull /sdcard/datastorebenchmark "./$1"
python dsb.py -p -v -s -b