# Differential Privacy and SPARQL

This is the companion git repository for the paper [Differential Privacy and SPARQL](http://www.semantic-web-journal.net/content/differential-privacy-and-sparql-0). The repository is organized as follows:

## Requirements

This works uses Java 1.8 and Maven project management software. All libraries needed to run the software are described in the [pom.xml](pom.xml) file.

## Installation

To install the software run `mvn clean install`. 

## Running the software

To run the software execute ```mvn exec:java -Dexec.mainClass=cl.utfsm.di.RDFDifferentialPrivacy.Run.RunSymbolic -Dexec.args="-f path_to_query -d sparql_endpoint_url -o output_file -v true -eps epsilon_value"```. The arguments to run the software are the following:

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
The `mSzie` variable stores the size of the schema (i.e. the amount of stars in our schema). The `mSchemaName` describes the privacy schema name. The `mEndpoint` variable stores the endpoint in which the privacy schema's data is stored.


### Queries

The queries used in the evaluation are in the [resources/evaluation/queries](resources/evaluation/queries) folder. The queries are enumerated as in the Appendix Section in the paper.

