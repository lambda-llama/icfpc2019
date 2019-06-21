#!/bin/bash

OUTDIR=./solutions
SOLUTION=$OUTDIR/solution-$(date +%s).zip

for f in $(find . -name '*.desc' | sort -n); do
    java -jar ./build/libs/icfpc2019.jar --non-interactive $f
done

mkdir -p $OUTDIR
find . -name '*.sol' -exec zip -j $SOLUTION {} \;
