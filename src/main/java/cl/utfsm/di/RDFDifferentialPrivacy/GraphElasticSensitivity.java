package cl.utfsm.di.RDFDifferentialPrivacy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cl.utfsm.di.RDFDifferentialPrivacy.utils.Polynomial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.interfaces.IExpr;

public class GraphElasticSensitivity {

    private static final Logger logger = LogManager
            .getLogger(GraphElasticSensitivity.class.getName());


    public static StarQuery calculateSensitivity(int k,
            List<StarQuery> listStars, double EPSILON,
            DataSource dataSource) throws ExecutionException, CloneNotSupportedException {

        StarQuery starQueryFirst = Collections.max(listStars);
        listStars.remove(starQueryFirst);

        if (listStars.size() > 1) {
            // S_G(star1, G)
            String elasticStabilityFirstStar = "1";
            starQueryFirst.setElasticStability(elasticStabilityFirstStar);
            StarQuery starQuerySecond = calculateSensitivity(k, listStars, EPSILON, dataSource);
            // S_G(star2, G)
            return calculateJoinSensitivity(starQueryFirst, starQuerySecond, dataSource);
        } else {
            // calculate sensibility for starQuery
            String elasticStabilityFirstStar = "1";
            starQueryFirst.setElasticStability(elasticStabilityFirstStar);

            // second star query in the map
            StarQuery starQuerySecond = Collections.max(listStars);
            listStars.remove(starQuerySecond);
            // String elasticStabilityPrime = "x";
            String elasticStabilityPrime = "1";
            starQuerySecond.setElasticStability(elasticStabilityPrime);
            // now we join
            return calculateJoinSensitivity(starQueryFirst, starQuerySecond, dataSource);

        }
    }

    private static StarQuery calculateJoinSensitivity(StarQuery starQueryLeft, StarQuery starQueryRight,
            DataSource hdtDataSource) throws CloneNotSupportedException, ExecutionException {

        List<String> joinVariables = starQueryLeft.getVariables();
        joinVariables.retainAll(starQueryRight.getVariables());
        String mostPopularValueLeft;
        String mostPopularValueRight;
        if (starQueryLeft.getMostPopularValue() == null) {
            mostPopularValueLeft = mostPopularValue(joinVariables.get(0),
                    starQueryLeft, hdtDataSource);
            logger.info("mostPopularValueLeft: " + mostPopularValueLeft);
            starQueryLeft.setMostPopularValue(mostPopularValueLeft);
        } else {
            mostPopularValueLeft = starQueryLeft.getMostPopularValue();
        }
        if (starQueryRight.getMostPopularValue() == null) {
            mostPopularValueRight = mostPopularValue(joinVariables.get(0),
                    starQueryRight, hdtDataSource);
            logger.info("mostPopularValueRight: " + mostPopularValueRight);
            starQueryRight.setMostPopularValue(mostPopularValueRight);
        } else {
            mostPopularValueRight = starQueryRight.getMostPopularValue();
        }
        String stabilityRight = starQueryRight.getElasticStability();
        String stabilityLeft = starQueryLeft.getElasticStability();

        // new stability
        Polynomial polynomialF1 = new Polynomial("x", mostPopularValueRight);
        Polynomial polynomialF2 = new Polynomial("x", mostPopularValueLeft);

        polynomialF1.addBinomial(stabilityLeft);
        polynomialF2.addBinomial(stabilityRight);

        // new stability
        String f1 = mostPopularValueRight + " * " + stabilityLeft;
        String f2 = mostPopularValueLeft + " * " + stabilityRight;
        // String f2 = new Func("f2", mostPopularValueLeft.multiply(stabilityRight));

        // I generate new starQueryPrime
        StarQuery newStarQueryPrime = new StarQuery(starQueryLeft.getTriples(), starQueryLeft.getStarSchemaName() + starQueryRight.getStarSchemaName());
        newStarQueryPrime.addStarQuery(starQueryRight.getTriples());

        ExprEvaluator exprEvaluator = new ExprEvaluator(false, (short) 100);
        IExpr result;

        result = exprEvaluator.eval("Function({x}, " + polynomialF1 + ")[1]");
        double f1Val = Math.round(exprEvaluator.evalf(result));

        result = exprEvaluator.eval("Function({x}, " + polynomialF2 + ")[1]");
        double f2Val = Math.round(exprEvaluator.evalf(result));

        if (f1Val > f2Val) {
            newStarQueryPrime.setElasticStability(f1);
            newStarQueryPrime.setMostPopularValue(mostPopularValueRight);
        } else {
            newStarQueryPrime.setElasticStability(f2);
            newStarQueryPrime.setMostPopularValue(mostPopularValueLeft);
        }

        return newStarQueryPrime;

    }

    /*
     * mostPopularValue(joinVariable a, StarQuery starQuery, DataSource)
     */
    private static String mostPopularValue(String var, StarQuery starQuery,
            DataSource dataSource) {
        // base case: mp(a,s_1,G)
        return Polynomial.createBinomial("x",
                Integer.toString(dataSource.mostFrequenResult(new MaxFreqQuery(starQuery.toString(), var))));
    }

    public static Sensitivity smoothElasticSensitivity(String elasticSensitivity,
            double prevSensitivity, double beta, int k, long graphSize) {
        Sensitivity sensitivity = new Sensitivity(prevSensitivity,
                elasticSensitivity);

        
        int maxI = 0;
        double ceil = findCeil(beta, elasticSensitivity, graphSize, k);

        short historyCapacity = 100;
        ExprEvaluator exprEvaluator = new ExprEvaluator(false, historyCapacity);
        IExpr result;

        for (int i = 0; i < ceil; i++) {
            
            result = exprEvaluator.eval("Function({x}, " + elasticSensitivity + ")[" + i + "]");
            double kPrime = exprEvaluator.evalf(result);
            double smoothSensitivity = Math.exp(-k * beta) * kPrime;
            if (smoothSensitivity > prevSensitivity) {
                prevSensitivity = smoothSensitivity;
                maxI = i;
            }
            k++;
        }
        sensitivity.setMaxK(maxI);
        sensitivity.setSensitivity(prevSensitivity);
        return sensitivity;
    }

    public static Sensitivity smoothElasticSensitivityStar(
            String elasticSensitivity, Sensitivity prevSensitivity, double beta,
            int k, long graphSize) {
        int maxI = 0;
        for (int i = 0; i < graphSize; i++) {
            Sensitivity smoothSensitivity = new Sensitivity(
                    Math.exp(-k * beta) * 1, elasticSensitivity);
            if (smoothSensitivity.getSensitivity() > prevSensitivity
                    .getSensitivity()) {
                prevSensitivity = smoothSensitivity;
                maxI = i;
            }
            k++;
        }
        Sensitivity sens = new Sensitivity(prevSensitivity.getSensitivity(),
                elasticSensitivity);
        sens.setMaxK(maxI);
        return sens;
    }

    private static double findCeil(double beta, String elasticSensitivity, long graphSize, int k) {
        short historyCapacity = 100;
        ExprEvaluator exprEvaluator = new ExprEvaluator(false, historyCapacity);

        // NSolve function uses Laguerre's method, returning real and complex solutions
        logger.info("E^(-" + beta + "*x)*" + elasticSensitivity);

        IExpr result = exprEvaluator.eval("diff(E^(-" + beta + "*x)*" + elasticSensitivity + ",x)");
        String derivate = result.toString();

        String cleaned = derivate.replaceAll("2.718281828459045", "E");
        cleaned = cleaned.replaceAll("\\*E\\^\\([+-]?\\d*\\.?\\d*\\*x\\)", "");
        cleaned = cleaned.replaceAll("E\\^\\([+-]?\\d*\\.?\\d*\\*x\\)\\*", "");
        cleaned = cleaned.replaceAll("/E\\^\\([+-]?\\d*\\.?\\d*\\*x\\)", "");
        cleaned = cleaned.replaceAll("\\+E\\^\\([+-]?\\d*\\.?\\d*\\*x\\)", "+1");
        cleaned = cleaned.replaceAll("E\\^\\([+-]?\\d*\\.?\\d*\\*x\\)\\+", "1+");

        logger.info("Cleaned function: " + cleaned);

        // result = exprEvaluator.eval("NSolve(0==" + cleaned + ",x)");
        result = exprEvaluator.eval("NRoots(" + cleaned + "==0)");

        String strResult = result.toString();

        double ceilMaxCandidate = k;

        if (strResult.equals("{}")) {
            logger.info("The function has no roots.");
        } else {
            logger.info("Candidates: " + result);
            /*
             * OPTIMIZATION
             * If we maximize E^(-beta*x)*P(x), where P(x) is a polynomial.
             * The maximal value can be determined finding the max maximal
             * of the function, where it is decreasing to infinite.
             */

            strResult = strResult.substring(1, strResult.length() - 1);

            String[] arrayZeros = strResult.split(",");
            List<String> listStrZeros = new ArrayList<>(Arrays.asList(arrayZeros));

            // clean the string returned
            /*
             * listStrZeros.replaceAll(zero -> zero.substring(1, zero.length() - 1));
             * listStrZeros.replaceAll(zero -> zero.substring(3));
             */

            // remove complex solutions
            listStrZeros.removeIf(zero -> zero.contains("I"));
            listStrZeros.removeIf(zero -> zero.contains("i"));
            logger.info("aaa: " + listStrZeros);

            List<Double> listDoubleZeros = listStrZeros.stream().map(exprEvaluator::evalf).collect(Collectors.toList());

            double maxCandidate = Collections.max(listDoubleZeros);

            ceilMaxCandidate = (int) Math.ceil(maxCandidate);

            logger.info("graphSize: " + graphSize);
            logger.info("maxCandidate: " + maxCandidate);

            if (ceilMaxCandidate < 0) {
                ceilMaxCandidate = k;

            } else if (ceilMaxCandidate > graphSize) {
                ceilMaxCandidate = graphSize;
            }
        }
        return ceilMaxCandidate;
    }
}
