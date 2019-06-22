#!/bin/bash

./gradlew shadowJar

OUTDIR=./solutions
SOLUTION=$OUTDIR/solution-$(date --iso-8601=seconds).zip

for f in $(find . -name '*.desc' | sort -n); do
    java -jar ./build/libs/icfpc2019.jar --non-interactive $f
done

mkdir -p $OUTDIR
find . -name '*.sol' -exec zip -j $SOLUTION {} \;
