# Differential Privacy and SPARQL

This is the companion git repository for the paper [Differential Privacy and SPARQL](http://www.semantic-web-journal.net/content/differential-privacy-and-sparql-0). The repository is organized as follows:

## Requirements

This works uses Java 1.8 and Maven project management software. All libraries needed to run the software are described in the [pom.xml](pom.xml) file.

## Installation

To install the software run `mvn clean install`. 

## Running the software

To run the software execute ```mvn exec:java -Dexec.mainClass=cl.utfsm.di.RDFDifferentialPrivacy.Run.RunSymbolic -Dexec.args="-f path_to_query -d sparql_endpoint_url -o output_file -v true -eps epsilon_value```. The arguments to run the software are the following:

 * -f: path to the COUNT query file. If the parameter points to a folder, the software will scan for all files with extension `.rq` and run the queries it finds.
 * -d: SPARQL endpoint URL, the software runs the previous COUNT query on that endpoint. 
 * -o: output file in which the software stores the query results (original result, private result, noise added, stability polynomial and query sensitivity).
 * -v: boolean to indicate whether we are running a query evaluation or not (runs the percentage error calculation according to the paper's equation).
 * -e: indicates the privacy parameter \epsilon 


### Privacy schema 

The privacy schema we use in this software is in the [schema.inf.json](resources/schema.info.json) file. It is encoded as a JSON file, with the following data:

```json
{
        "mSize": schema_size,
        "mSchemaName":"schema_name",
        "mEndpoint":"endpoint_url"
    }
```
The `mSize` variable stores the size of the schema (i.e. the amount of stars in our schema). The `mSchemaName` describes the privacy schema name. The `mEndpoint` variable stores the endpoint in which the privacy schema's data is stored.


### Queries

The queries used in the evaluation are in the [resources/evaluation/queries](resources/evaluation/queries) folder. The queries are enumerated as in the Appendix Section in the paper.

### Experiment Reproducibility

The experiments from the paper (Differential Privacy and SPARQL)[https://www.semantic-web-journal.net/content/differential-privacy-and-sparql-0] can be reproduced following the next steps:

 1. Download the Wikidata data from (Figshare)[https://figshare.com/s/50b7544ad6b1f51de060]
 2. Extract all URIs representing the RDF resources that will be in your private dataset, storing them into a file `uris_file`. It is possible to do that by using a simple query like `SELECT ?uri WHERE {?uri wdt:P31 wd:Q5}`
 3. To create the privacy schemas for people, organizations and professions run the (get_schemas.py)[scripts/get_schemas.py] Python script using as input parameters:
    * `uris_file`: URIs for the RDF resources from which extract the data (properties) that will be in the private database;
    * `properties_file`: the properties from the defined privacy schema;
    * `output_folder`: the folder in which the data will write.
    * `endpoint_url`: the sparql endpoint storing the data that populates the privacy schema.
 4. Load the data generated by the (get_schemas.py)[scripts/get_schemas.py] script into a SPARQL endpoint (such as Jena Fuseki + TDB)
 5. Execute the command ``mvn exec:java -Dexec.mainClass=cl.utfsm.di.RDFDifferentialPrivacy.Run.RunSymbolic -Dexec.args="-f path_to_query -d sparql_endpoint_url -o output_file -v true -eps epsilon_value``` using the parametes described previously.

 ## Copyright

MIT License

Copyright (c) [2023] [Carlos Buil-Aranda, Jorge Lobo and Federico Olmedo]

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.