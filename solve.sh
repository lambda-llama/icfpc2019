#!/bin/bash

set -xe

./gradlew shadowJar

OUTDIR=./solutions
SOLUTION=$OUTDIR/solution-$(date --iso-8601=seconds).zip

find part-* -name '*.desc' | sort -n | parallel -P 12 --progress java -jar ./build/libs/icfpc2019.jar --non-interactive "$@"

mkdir -p $OUTDIR
find . -regextype sed -regex "./part.*/.*sol" -exec zip -j $SOLUTION {} \;
