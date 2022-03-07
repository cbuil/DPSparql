#!/usr/bin/env python3

import sys
import os
import glob


if __name__ == '__main__':
    uris_file = sys.argv[1]
    with open(uris_file, 'r') as f:
        lines = f.readlines()
        for line in lines:
            print("<" + line.rstrip() + ">" +  " <http://www.wikidata.org/prop/direct/P31> <http://www.wikidata.org/entity/Q28640> .")
