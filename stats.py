#!/usr/bin/python3

import json
import os

if __name__ == "__main__":
    all = set()
    best = set()
    for (d, _, fs) in os.walk("."):
        if "part-" in d:
            for f in fs:
                if ".meta" in f:
                    with open(d + os.sep + f) as fin:
                        x = json.load(fin)
                        for k in x:
                            if k != "bestTime":
                                all.add(k)
                                if x[k] == x["bestTime"]:
                                    best.add(k)
    print("Non-winning strategies:")
    for s in sorted(all - best):
        print("  " + s)
