#!/bin/bash

set -xe

./gradlew shadowJar

OUTDIR=./solutions
SOLUTION=$OUTDIR/solution-$(date --iso-8601=seconds).zip

find part-* -name '*.desc' | sort -n | xargs -P 8 -n 1 java -jar ./build/libs/icfpc2019.jar --non-interactive --validate

mkdir -p $OUTDIR
find . -name '*.sol' -exec zip -j $SOLUTION {} \;
