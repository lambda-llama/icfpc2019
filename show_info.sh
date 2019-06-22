#!/bin/bash

./gradlew shadowJar

OUTDIR=./solutions
SOLUTION=$OUTDIR/solution-$(date --iso-8601=seconds).zip

for f in $(find . -name '*.desc' | sort -n); do
    java -jar ./build/libs/icfpc2019.jar --non-interactive $f --info_only --show_bonus_count
done
