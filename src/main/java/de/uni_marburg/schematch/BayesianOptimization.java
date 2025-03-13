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
import java.util.*;

public class BayesianOptimization {

    final static Logger log = LogManager.getLogger(BayesianOptimization.class);

    public static void main(String[] args) {

        //TODO: Turn of Profiling

        Configuration config = Configuration.getInstance();
        //config.setEvaluateAttributes(true);
        //config.setSaveOutputTablePairGeneration(false);

        List<MatchStep> matchSteps = new ArrayList<>();

        //Matcher erstellen und Parameter setzen
        SimilarityFlooding similarityFlooding = new SimilarityFlooding();
        similarityFlooding.setWholeSchema("true");
        similarityFlooding.setPropCoeffPolicy("INV_PROD");
        similarityFlooding.setFixpoint("A");
        similarityFlooding.setFDV1("false");
        similarityFlooding.setFDV2("false");
        similarityFlooding.setUCCV1("false");
        similarityFlooding.setUCCV2("false");
        similarityFlooding.setINDV1("false");
        similarityFlooding.setINDV2("false");

        //FirstLine Matcher Booster
        ThresholdSelectionBoosting thresholdSelectionBoosting = new ThresholdSelectionBoosting(0.95);

        //Metriken festlegen
        List<Metric> metrics = new ArrayList<>();
        metrics.add(new F1Score());

        //TablePairs generieren damit Evaluation möglich
        TablePairsGenerator tablePairsGenerator = new NaiveTablePairsGenerator();
        //matchSteps.add(new TablePairGenerationStep(config.isSaveOutputTablePairGeneration(), config.isEvaluateTablePairGeneration(), tablePairsGenerator));

        List<Double> performances = new ArrayList<>();

        // Loop until no change in parameters is detected
        double avgPerformanceOverAllDatasets;
        HashMap<String, String> currentParams = similarityFlooding.getParameters();
        HashMap<String, String> newParams;

        List<HashMap<String, String>> parameterHistory = new ArrayList<>();
        List<Double> scoreHistory = new ArrayList<>();
        do {
            avgPerformanceOverAllDatasets = getAvgPerformanceOverAllDatasets(config, matchSteps, metrics, tablePairsGenerator, similarityFlooding, thresholdSelectionBoosting, performances);
            newParams = runOptimization(avgPerformanceOverAllDatasets, currentParams, similarityFlooding.getPossibleValues());
            parameterHistory.add(new HashMap<>(currentParams)); // Store the current parameters
            scoreHistory.add(avgPerformanceOverAllDatasets); // Store the score
            if (!newParams.equals(currentParams)) {
                currentParams = newParams;
                similarityFlooding.setParameters(currentParams);
            } else {
                break;
            }
        } while (true);

        // Log all parameter sets and their scores
        log.info("Parameter history:");
        for (int i = 0; i < parameterHistory.size(); i++) {
            log.info("Parameters: " + parameterHistory.get(i).toString() + ", Score: " + scoreHistory.get(i));
        }
    }

    private static double getAvgPerformanceOverAllDatasets(Configuration config, List<MatchStep> matchSteps, List<Metric> metrics, TablePairsGenerator tablePairsGenerator, SimilarityFlooding similarityFlooding, ThresholdSelectionBoosting thresholdSelectionBoosting, List<Double> performances) {
        for (Configuration.DatasetConfiguration datasetConfiguration : config.getDatasetConfigurations()) {
            Dataset dataset = new Dataset(datasetConfiguration);

            log.info("Evaluating dataset " + dataset.getName() + " with the Parameters: ");

            List<Float> performancesPerScenarioInDataset = new ArrayList<>();

            //Loop over scenrio
            for (String scenarioName : dataset.getScenarioNames()) {
                Scenario scenario = new Scenario(dataset.getPath() + File.separator + scenarioName);
                log.debug("Starting experiments for dataset " + dataset.getName() + ", scenario: " + scenario.getPath());

                MatchTask matchTask = new MatchTask(dataset, scenario, matchSteps, metrics);
                List<TablePair> tablePairs = tablePairsGenerator.generateCandidates(scenario);

                float[][] simFloodingResult = similarityFlooding.match(matchTask, null);
                //System.out.println(Arrays.deepToString(simFloodingResult));

                simFloodingResult = thresholdSelectionBoosting.run(matchTask, null, simFloodingResult);
                //System.out.println(Arrays.deepToString(simFloodingResult));

                matchTask.setTablePairs(tablePairs);

                matchTask.readGroundTruth();
                System.out.println(Arrays.deepToString(matchTask.getGroundTruthMatrix()));

                Evaluator evaluator = new Evaluator(metrics, scenario, matchTask.getGroundTruthMatrix());
                Performance performance = evaluator.evaluate(simFloodingResult).get(metrics.get(0));
                float F1Score = performance.getGlobalScore();

                performancesPerScenarioInDataset.add(F1Score);
                //System.out.println("F1 Score: " + F1Score);
            }

            double avgPerformanceForDataset = performancesPerScenarioInDataset.stream().mapToDouble(Float::doubleValue).average().orElse(0.0);
            performances.add(avgPerformanceForDataset);
        }
        double avgPerformanceOverAllDatasets = performances.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        System.out.println(avgPerformanceOverAllDatasets);
        return avgPerformanceOverAllDatasets;
    }

    /**
     * This method runs the optimization algorithm and returns the next set of parameters to be used.
     *
     * @param score          Score of the current set of parameters
     * @param currentParams  Current set of parameters
     * @param possibleValues Possible values for the parameters
     * @return HashMap containing the next set of parameters
     */
    private static HashMap<String, String> runOptimization(double score, HashMap<String, String> currentParams, HashMap<String, Collection<String>> possibleValues) {
        log.info("Running optimization...");
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("scripts/Optimizations/.venv/bin/python", "scripts/Optimizations/BayesianOptimization.py", String.valueOf(score), currentParams.toString(), possibleValues.toString());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
//                log.info("Python script output:\n" + output.toString());
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {

                // Parse the output to create the HashMap
                HashMap<String, String> params = new HashMap<>();
                // Assuming the output is in key=value format
                for (String param : output.toString().split("\n")) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2) {
                        params.put(keyValue[0], keyValue[1]);
                    }
                }
                log.info("Optimization successful. New parameters: " + params);
                return params;
            } else {
                log.error("Python script execution failed with exit code " + exitCode);
                // Log the output
                log.error(output.toString());
            }
        } catch (Exception e) {
            log.error("Exception while running optimization: ", e);
        }
        log.error("Returning empty HashMap");
        return new HashMap<>();
    }
}
