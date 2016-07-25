#!/bin/sh
cwd=$(cd $(dirname $0);pwd)

for test in $(ls $cwd/tests/*Test.sh); do
    echo "### Test $test"
    /bin/sh -c $test
done