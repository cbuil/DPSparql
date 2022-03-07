#!/usr/bin/env python3
import sys
import json

if __name__ == '__main__':
    if len(sys.argv) == 1:
        print('Usage: python parse_privacy_results.py JSON result_file output_file ')
    else:
        # print(sys.argv[1], sys.argv[2])
        with open(sys.argv[1], 'r') as f:
            data = json.load(f)
            for triple in data['results']['bindings']:
                if 'uri' in triple['object']['type']:
                    print("<" + triple['subject']['value'] + "> <" +  triple['predicate']['value'] + "> <" +  triple['object']['value'] + "> .")
                else:
                    print("<" + triple['subject']['value'] + "> <" +  triple['predicate']['value'] + "> \"" +  triple['object']['value'] + "\" .")
