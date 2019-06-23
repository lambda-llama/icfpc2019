#!/usr/bin/python3

from functools import reduce
import json
import os

if __name__ == "__main__":
    all = set()
    winners = {
        "part-1-initial": {},
        "part-2-teleports": {},
        "part-3-clones": {},
    }
    for (d, _, fs) in os.walk("."):
        d = d[2:]
        if "part-" in d:
            for f in fs:
                if ".meta" in f:
                    with open(d + os.sep + f) as fin:
                        x = json.load(fin)
                        for k in x:
                            if k != "bestTime":
                                all.add(k)
                                if x[k] == x["bestTime"]:
                                    winners[d][k] = winners[d].get(k, 0) + 1

    all_winners = set()
    for d in winners:
        print("Winners in %s:" % d)
        for s in sorted(winners[d], key=lambda k: -winners[d][k]):
            print("    %s: %d" % (s, winners[d][s]))
        all_winners = all_winners.union(set(winners[d]))
        print()

    print("Non-winning strategies:")
    for s in sorted(all - all_winners):
        print("    %s" % s)
