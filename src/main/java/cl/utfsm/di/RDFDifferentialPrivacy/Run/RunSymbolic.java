package cl.utfsm.di.RDFDifferentialPrivacy.Run;

import cl.utfsm.di.RDFDifferentialPrivacy.*;
import cl.utfsm.di.RDFDifferentialPrivacy.utils.Helper;
import cl.utfsm.di.RDFDifferentialPrivacy.utils.SchemaInfo;

import org.apache.commons.cli.*;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ExecutionException;

import com.google.gson.Gson;

public class RunSymbolic {

    private static final Logger logger = LogManager.getLogger(RunSymbolic.class.getName());

    // privacy budget
    private static double EPSILON = 0.1;

    private static String queryString = "";
    private static String queryFile = "";
    private static String dataFile = "";
    private static String outputFile = "";
    private static String endpoint = "";
    private static boolean evaluation = false;

    public static void main(String[] args) throws IOException, CloneNotSupportedException, ExecutionException {

        parseInput(args);

        try {
            DataSource dataSource;
            dataSource = new EndpointDataSource(dataFile);

            Path queryLocation = Paths.get(queryFile);
            if (Files.isRegularFile(queryLocation)) {
                try (Scanner input = new Scanner(new File(queryFile))) {
                    queryString = input.useDelimiter("\\Z").next();
                    logger.info(queryString);
                    runAnalysis(queryFile, queryString, dataSource, outputFile, evaluation, EPSILON);
               }
            } else if (Files.isDirectory(queryLocation)) {
                Iterator<Path> filesPath = Files.list(Paths.get(queryFile)).filter(p -> p.toString().endsWith(".rq"))
                        .iterator();
                logger.info("Running analysis to DIRECTORY: " + queryLocation);
                while (filesPath.hasNext()) {
                    Path nextQuery = filesPath.next();
                    logger.info("Running analysis to query: " + nextQuery.toString());
                    try (Scanner input = new Scanner(nextQuery)) {
                        queryString = input.useDelimiter("\\Z").next();
                        logger.debug(queryString);
                        runAnalysis(nextQuery.toString(), queryString, dataSource, outputFile, evaluation, EPSILON);
                    } catch (Exception e) {
                        logger.error("query failed!!: " + nextQuery.toString());
                    }
                    // logger.info("Cache stats: "
                    // + dataSource.mostFrequenResultStats());
                }
            } else {
                if (Files.notExists(queryLocation)) {
                    throw new FileNotFoundException("No query file");
                }
            }
        } catch (IOException e1) {
            System.out.println("Exception: " + e1.getMessage());
            System.exit(-1);
        }
    }

    private static void runAnalysis(String queryFile, String queryString, DataSource dataSource, String outpuFile,
            boolean evaluation, double EPSILON) throws IOException, CloneNotSupportedException, ExecutionException {
        int countQueryResult = dataSource.executeCountQuery(queryString);
        Query q = QueryFactory.create(queryString);

        List<List<String>> triplePatterns = new ArrayList<>();

        ElementGroup queryPattern = (ElementGroup) q.getQueryPattern();
        List<Element> elementList = queryPattern.getElements();
        Sensitivity smoothSensitivity;
        Element element = elementList.get(0);
        boolean starQuery = false;
        if (element instanceof ElementPathBlock) {
            String elasticStability = "0";

            int k = 1;

            // Map<String, List<TriplePath>> starQueriesMap = Helper
            // .getStarPatterns(q);

            List<SchemaInfo> schemaInfos = getSchemasInfo();
            Map<String, List<TriplePath>> starQueriesMap = Helper.getStarPatterns(q, schemaInfos);

            dataSource.setMostFreqValueMaps(starQueriesMap, triplePatterns);
            Map<String, String> schemasURL = new HashMap<String, String>();
            long graphSize = 0;
            for (SchemaInfo schemaInfo : schemaInfos) {
                schemasURL.put(schemaInfo.mSchemaName, schemaInfo.mEndpoint);
                graphSize += schemaInfo.mSize;
                // if (starQueriesMap.containsKey(schemaInfo.mSchemaName)) {
                //     graphSize += schemaInfo.mSize;
                // }

            }
            dataSource.setSchemasURL(schemasURL);
            logger.info("graph size " + graphSize);
            // delta parameter: use 1/n^2, with n = size of the data in the
            // query
            double DELTA = 1 / (Math.pow(graphSize, 2));
            double beta = EPSILON / (2 * Math.log(2 / DELTA));
            if (Helper.isStarQuery(q)) {
                starQuery = true;
                elasticStability = "x";
                Sensitivity sensitivity = new Sensitivity(1.0, elasticStability);
                smoothSensitivity = GraphElasticSensitivity.smoothElasticSensitivityStar(elasticStability, sensitivity,
                        beta, k, graphSize);
                logger.info("star query (smooth) sensitivity: " + smoothSensitivity);
            } else {
                // elasticStability = GraphElasticSensitivity
                // .calculateSensitivity(k, starQueriesMap,
                // EPSILON, hdtDataSource);

                List<StarQuery> listStars = new ArrayList<>();
                for(String key : starQueriesMap.keySet()){
                    listStars.add(new StarQuery(starQueriesMap.get(key), key));
                }
                StarQuery sq = GraphElasticSensitivity.calculateSensitivity(k, listStars, EPSILON, dataSource);

                logger.info("Elastic Stability: " + sq.getElasticStability());
                smoothSensitivity = GraphElasticSensitivity.smoothElasticSensitivity(sq.getElasticStability(), 0, beta,
                        k, graphSize);
                elasticStability = sq.getElasticStability();
                logger.info("Path Smooth Sensitivity: " + smoothSensitivity.getSensitivity());
            }

            // add noise using Laplace Probability Density Function
            // 2 * sensitivity / epsilon
            double scale = 2 * smoothSensitivity.getSensitivity() / EPSILON;

            writeAnalysisResult(scale, queryFile, EPSILON, evaluation, countQueryResult, elasticStability, graphSize,
                    starQuery, dataSource, EPSILON, smoothSensitivity, outpuFile);

        }
    }

    private static void parseInput(String[] args) throws IOException {
        // create Options object
        Options options = new Options();

        options.addOption("q", "query", true, "input SPARQL query");
        options.addOption("f", "qFile", true, "input SPARQL query File");
        options.addOption("d", "data", true, "HDT data file");
        options.addOption("e", "dir", true, "query directory");
        options.addOption("o", "dir", true, "output file");
        options.addOption("v", "evaluation", true, "evaluation");
        options.addOption("eps", "epsilon", true, "epsilon");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("q")) {
                queryString = cmd.getOptionValue("q");
            } else {
                logger.info("Missing SPARQL query ");
            }
            if (cmd.hasOption("eps")) {
                EPSILON = Double.parseDouble(cmd.getOptionValue("eps"));
            } else {
                logger.info("Missing SPARQL query ");
            }
            if (cmd.hasOption("f")) {
                queryFile = cmd.getOptionValue("f");
                // queryString = new Scanner(new File(queryFile))
                // .useDelimiter("\\Z").next();
                // transform into Jena Query object
            } else {
                logger.info("Missing SPARQL query file");
            }
            if (cmd.hasOption("d")) {
                dataFile = cmd.getOptionValue("d");
            } else {
                logger.info("Missing data file");
            }
            if (cmd.hasOption("e")) {
                endpoint = cmd.getOptionValue("e");
            } else {
                logger.info("Missing endpoint address");
            }
            if (cmd.hasOption("o")) {
                outputFile = cmd.getOptionValue("o");
                if (!Files.exists(Paths.get(outputFile))) {
                    Files.createFile(Paths.get(outputFile));
                }
            } else {
                logger.info("Missing output file");
            }
            if (cmd.hasOption("v")) {
                evaluation = true;
            }
        } catch (ParseException e1) {
            System.out.println(e1.getMessage());
            System.exit(-1);
        }

    }

    private static void writeAnalysisResult(double scale, String queryFile, double ePSILON, boolean evaluation,
            int countQueryResult, String elasticStability, long graphSize, boolean starQuery, DataSource hdtDataSource,
            double EPSILON, Sensitivity smoothSensitivity, String outpuFile) throws IOException {
        SecureRandom random = new SecureRandom();

        int times = 1;
        if (evaluation) {
            times = 100;
        }

        List<Double> privateResultList = new ArrayList<>();
        List<Integer> resultList = new ArrayList<>();
        for (int i = 0; i < times; i++) {
            double u = 0.5 - random.nextDouble();
            // LaplaceDistribution l = new LaplaceDistribution(u, scale);
            double noise = -Math.signum(u) * scale * Math.log(1 - 2 * Math.abs(u));
            logger.debug("Math.log(1 - 2 * Math.abs(u)) " + Math.log(1 - 2 * Math.abs(u)));

            double finalResult1 = countQueryResult + noise;
            // double finalResult2 = countQueryResult + l.sample();

            logger.debug("Original result: " + countQueryResult);
            logger.debug("Noise added: " + Math.round(noise));
            logger.debug("Private Result: " + Math.round(finalResult1));
            privateResultList.add(finalResult1);
            resultList.add(countQueryResult);
        }
        logger.debug("Original result: " + countQueryResult);
        logger.debug("Private  result: " + Math.round(privateResultList.get(times - 1)));

        Result result = new Result(queryFile, EPSILON, privateResultList, smoothSensitivity.getSensitivity(),
                resultList, smoothSensitivity.getMaxK(), scale, elasticStability, graphSize, starQuery,
                hdtDataSource.getMapMostFreqValue(), hdtDataSource.getMapMostFreqValueStar());

        StringBuffer resultsBuffer = new StringBuffer();
        resultsBuffer.append(result.toString().replace('\n', ' '));
        resultsBuffer.append("\n");

        Files.write(Paths.get(outpuFile), resultsBuffer.toString().getBytes(), StandardOpenOption.APPEND);
    }

    private static List<SchemaInfo> getSchemasInfo() throws IOException {
        List<SchemaInfo> schemasInfo = new ArrayList<SchemaInfo>();
        String schemaInfoFile = readFile("resources/schema.info.json", StandardCharsets.US_ASCII);
        logger.debug(schemaInfoFile);
        Gson gson = new Gson();
        SchemaInfo[] schemas = gson.fromJson(schemaInfoFile, SchemaInfo[].class);
        schemasInfo = Arrays.asList(schemas);
        return schemasInfo;
    }

    static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

}
