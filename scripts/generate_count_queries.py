#!/usr/bin/env python3
import sys
import os

def generate_queries(patterns_file, output_file):
    with open(patterns_file, 'r') as f:
        patterns = f.readlines()
        with open(output_file, 'w') as fw:
            for pattern in patterns:
                query = ""
                query = "SELECT COUNT((" + pattern.split(sep = ',', maxsplit=1)[1].split()[0] + ") as ?count) WHERE {" + pattern.split(sep=',', maxsplit=1)[1].rstrip() + "}\n"
                fw.write(query)

if __name__ == '__main__':
    if len(sys.argv) == 1:
        # python get_schemas.py uris_file properties_file output_folder
        print('Usage: python generate_count_queries.py uris_file patterns_file')
    else:
        try:
            generate_queries(sys.argv[1], sys.argv[2])
        except Exception as exc:
            print(exc)
            sys.exit()
        sys.exit()
