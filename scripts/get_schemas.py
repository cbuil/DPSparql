#!/usr/bin/env python3

import traceback
import sys
import os
import glob
import subprocess
from multiprocessing import Pool
from SPARQLWrapper import SPARQLWrapper, JSON

cores = 4


def get_rdf_resources(uri_list, properties_list, output_folder, endpoint_url):
    for uri in uri_list:
        for uri_property in properties_list:
            sparql = SPARQLWrapper(endpoint_url)
            sparql.setQuery("""
            SELECT ?x
            WHERE { <""" + uri.replace('\n', '') + """> <""" + uri_property.replace('\n', '') + """> ?x }
            """)
            sparql.setReturnFormat(JSON)
            results = sparql.query().convert()

            for result in results["results"]["bindings"]:
                if "http://www.wikidata.org/" in result["x"]["value"]:
                    print("<" + uri.replace("\n", "") + "> <" + uri_property.replace("\n", "") + "> <" + result["x"]["value"].replace("\n", "") + "> . ")
                else:
                    print("<" + uri.replace("\n", "") + "> <" + uri_property.replace("\n", "") + "> \"" + result["x"]["value"].replace("\n", "") + "\" . ")


def main_parallel(uris_file, properties_file, output_folder, endpoit_url):
    arglist = []
    AVAILABLE_CORES = int(cores)
    folder_number = 0
    with open(uris_file, 'r') as f_uris:
        with open(properties_file, 'r') as f_properties:
            properties_list = f_properties.readlines()
            uris_list = f_uris.readlines()
            # get_rdf_resources(uris_list, properties_list, output_folder, endpoit_url)
            uris_size = len(uris_list)
            uris_segments = uris_size // cores
            start_uri = 0
            params = []
            while start_uri < uris_size:
                params.append([uris_list[start_uri:start_uri + uris_segments-1], properties_list, output_folder, endpoit_url])
                start_uri = start_uri + uris_segments

            with Pool() as pool:
                pool.starmap(get_rdf_resources, params)
                folder_number += 1

#
    # print('Processing ' + str(folder_number) + ' files with ' +
          # str(AVAILABLE_CORES) + ' cores, this may take a while...')
    # pool = Pool(processes=AVAILABLE_CORES)
    # pool.map_async(main, arglist).get()


if __name__ == '__main__':
    if len(sys.argv) == 1:
        # python get_schemas.py uris_file properties_file output_folder
        print('Usage: python get_schemas.py uris_file properties_file output_folder endpoint_url')
    else:
        try:
            main_parallel(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4])
            # main_parallel(sys.argv[1], sys.argv[2], sys.argv[3])
        except Exception as exc:
            print(exc)
            sys.exit()
        sys.exit()
