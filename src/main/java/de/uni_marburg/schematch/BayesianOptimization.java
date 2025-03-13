package de.uni_marburg.schematch;

import de.uni_marburg.schematch.boosting.ThresholdSelectionBoosting;
import de.uni_marburg.schematch.data.Dataset;
import de.uni_marburg.schematch.data.Scenario;
import de.uni_marburg.schematch.evaluation.Evaluator;
import de.uni_marburg.schematch.evaluation.metric.F1Score;
import de.uni_marburg.schematch.evaluation.metric.Metric;
import de.uni_marburg.schematch.evaluation.performance.Performance;
import de.uni_marburg.schematch.matching.similarityFlooding.SimilarityFlooding;
import de.uni_marburg.schematch.matchtask.MatchTask;
import de.uni_marburg.schematch.matchtask.matchstep.MatchStep;
import de.uni_marburg.schematch.matchtask.tablepair.TablePair;
import de.uni_marburg.schematch.matchtask.tablepair.generators.NaiveTablePairsGenerator;
import de.uni_marburg.schematch.matchtask.tablepair.generators.TablePairsGenerator;
import de.uni_marburg.schematch.utils.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class BayesianOptimization {

    private static final Logger log = LogManager.getLogger(BayesianOptimization.class);

    public static void main(String[] args) {
        Configuration config = Configuration.getInstance();
        List<MatchStep> matchSteps = new ArrayList<>();

        // Initialize similarity flooding matcher and set its parameters
        SimilarityFlooding similarityFlooding = new SimilarityFlooding();
        configureSimilarityFlooding(similarityFlooding);

        // Initialize threshold selection boosting with a fixed threshold
        ThresholdSelectionBoosting thresholdSelectionBoosting = new ThresholdSelectionBoosting(0.95);
        List<Metric> metrics = List.of(new F1Score());
        TablePairsGenerator tablePairsGenerator = new NaiveTablePairsGenerator();

        // Run the optimization loop
        optimizeParameters(config, matchSteps, metrics, tablePairsGenerator, similarityFlooding, thresholdSelectionBoosting);
    }

    private static void configureSimilarityFlooding(SimilarityFlooding similarityFlooding) {
        // Set predefined parameters for the similarity flooding algorithm
        similarityFlooding.setWholeSchema("true");
        similarityFlooding.setPropCoeffPolicy("INV_PROD");
        similarityFlooding.setFixpoint("A");
        similarityFlooding.setFDV1("false");
        similarityFlooding.setFDV2("false");
        similarityFlooding.setUCCV1("false");
        similarityFlooding.setUCCV2("false");
        similarityFlooding.setINDV1("false");
        similarityFlooding.setINDV2("false");
    }

    private static void optimizeParameters(Configuration config, List<MatchStep> matchSteps, List<Metric> metrics, TablePairsGenerator tablePairsGenerator, SimilarityFlooding similarityFlooding, ThresholdSelectionBoosting thresholdSelectionBoosting) {
        List<Double> performances = new ArrayList<>();
        HashMap<String, String> currentParams = similarityFlooding.getParameters();
        List<HashMap<String, String>> parameterHistory = new ArrayList<>();
        List<Double> scoreHistory = new ArrayList<>();

        while (true) {
            // Evaluate current parameter set
            double avgPerformance = getAvgPerformance(config, matchSteps, metrics, tablePairsGenerator, similarityFlooding, thresholdSelectionBoosting, performances);
            parameterHistory.add(new HashMap<>(currentParams));
            scoreHistory.add(avgPerformance);

            // Optimize parameters based on evaluation
            HashMap<String, String> newParams = runOptimization(avgPerformance, currentParams, similarityFlooding.getPossibleValues());
            if (!newParams.equals(currentParams)) {
                currentParams = newParams;
                similarityFlooding.setParameters(currentParams);
            } else {
                break; // Stop if no further improvement is found
            }
        }

        logParameterHistory(parameterHistory, scoreHistory);
    }

    private static double getAvgPerformance(Configuration config, List<MatchStep> matchSteps, List<Metric> metrics, TablePairsGenerator tablePairsGenerator, SimilarityFlooding similarityFlooding, ThresholdSelectionBoosting thresholdSelectionBoosting, List<Double> performances) {
        for (Configuration.DatasetConfiguration datasetConfig : config.getDatasetConfigurations()) {
            Dataset dataset = new Dataset(datasetConfig);
            List<Float> scenarioPerformances = new ArrayList<>();

            for (String scenarioName : dataset.getScenarioNames()) {
                Scenario scenario = new Scenario(dataset.getPath() + File.separator + scenarioName);
                MatchTask matchTask = new MatchTask(dataset, scenario, matchSteps, metrics);
                List<TablePair> tablePairs = tablePairsGenerator.generateCandidates(scenario);

                // Execute similarity flooding and apply threshold boosting
                float[][] results = similarityFlooding.match(matchTask, null);
                results = thresholdSelectionBoosting.run(matchTask, null, results);
                matchTask.setTablePairs(tablePairs);
                matchTask.readGroundTruth();

                // Evaluate performance
                Evaluator evaluator = new Evaluator(metrics, scenario, matchTask.getGroundTruthMatrix());
                Performance performance = evaluator.evaluate(results).get(metrics.get(0));
                scenarioPerformances.add(performance.getGlobalScore());
            }

            double avgPerformance = scenarioPerformances.stream().mapToDouble(Float::doubleValue).average().orElse(0.0);
            performances.add(avgPerformance);
        }
        return performances.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private static HashMap<String, String> runOptimization(double score, HashMap<String, String> currentParams, HashMap<String, Collection<String>> possibleValues) {
        log.info("Running optimization...");
        try {
            // Run Bayesian optimization script in Python
            ProcessBuilder processBuilder = new ProcessBuilder("scripts/Optimizations/.venv/bin/python", "scripts/Optimizations/BayesianOptimization.py", String.valueOf(score), currentParams.toString(), possibleValues.toString());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            HashMap<String, String> params = new HashMap<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] keyValue = line.split("=");
                if (keyValue.length == 2) {
                    params.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }
            process.waitFor();

            log.info("Optimization successful. New parameters: {}", params);
            return params;
        } catch (Exception e) {
            log.error("Exception during optimization: ", e);
            return new HashMap<>();
        }
    }

    private static void logParameterHistory(List<HashMap<String, String>> parameterHistory, List<Double> scoreHistory) {
        log.info("Parameter history:");
        for (int i = 0; i < parameterHistory.size(); i++) {
            log.info("Parameters: {}, Score: {}", parameterHistory.get(i), scoreHistory.get(i));
        }
    }
}
