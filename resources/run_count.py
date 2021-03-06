import json
import sys
from SPARQLWrapper import SPARQLWrapper, JSON

def run_count(json_query_file, output_file):
    with open(json_query_file, 'r') as f:
        queries = json.load(f)
        results = {}
        for query in queries:
            if not query["query"].replace(" ", "").lower() in results:
                sparql = SPARQLWrapper(query["endpoint"])
                sparql.setQuery(query["query"])
                sparql.setReturnFormat(JSON)
                print("query: {} variables {} endpoint: {}".format(query['query'], query['variable'], query["endpoint"]))
                query_results = sparql.query().convert()
                for query_result in query_results["results"]["bindings"]:
                    if not query['query'].replace(" ", "").lower() in results:
                        results[query["query"].replace(" ", "").lower()] = [query_result[query['variable']]['value'], query['comment'], query['variable'], query['endpoint'], query['query']]
                        results[query["query"].replace(" ", "").lower()].append(query['id'])
                    else:
                        results[query["query"].replace(" ", "").lower()].append(query['id'])
    with open(output_file, 'w') as f:
        json.dump(results, f, indent=4)

if __name__ == '__main__':
    if len(sys.argv) == 1:
        print('Usage: python run_count.py json_query_file ')
    else:
        print(sys.argv[1])
        run_count(sys.argv[1], sys.argv[2])