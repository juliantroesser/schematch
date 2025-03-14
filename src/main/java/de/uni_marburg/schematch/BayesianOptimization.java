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

import java.io.*;
import java.net.Socket;
import java.util.*;

public class BayesianOptimization {
    private static final Logger log = LogManager.getLogger(BayesianOptimization.class);
    private static final String HOST = "localhost";
    private static final int PORT = 5000;

    public static void main(String[] args) {
        startPythonScript();

        Configuration config = Configuration.getInstance();
        List<MatchStep> matchSteps = new ArrayList<>();

        SimilarityFlooding similarityFlooding = new SimilarityFlooding();
        configureSimilarityFlooding(similarityFlooding);
        ThresholdSelectionBoosting thresholdSelectionBoosting = new ThresholdSelectionBoosting(0.95);
        List<Metric> metrics = List.of(new F1Score());
        TablePairsGenerator tablePairsGenerator = new NaiveTablePairsGenerator();

        optimizeParameters(config, matchSteps, metrics, tablePairsGenerator, similarityFlooding, thresholdSelectionBoosting);
    }

    private static void startPythonScript() {
        try {
            ProcessBuilder pb = new ProcessBuilder("python", "scripts/Optimizations/BayesianOptimization.py");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            log.info("Python optimization script started.");
        } catch (IOException e) {
            log.error("Failed to start Python script: ", e);
        }
    }

    private static void configureSimilarityFlooding(SimilarityFlooding similarityFlooding) {
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
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            HashMap<String, String> currentParams = similarityFlooding.getParameters();
            double bestScore = 0.0;
            HashMap<String, String> bestParams = new HashMap<>();

            while (true) {
                double avgPerformance = getAvgPerformance(config, matchSteps, metrics, tablePairsGenerator, similarityFlooding, thresholdSelectionBoosting);
                writer.println(avgPerformance + " " + currentParams.toString());

                String response = reader.readLine();
                if (response.equals("DONE")) {
                    break;
                }

                currentParams = parseParameters(response);
                similarityFlooding.setParameters(currentParams);

                if (avgPerformance > bestScore) {
                    bestScore = avgPerformance;
                    bestParams = new HashMap<>(currentParams);
                }
            }

            log.info("Optimierung abgeschlossen. Beste Parameter: {}, Score: {}", bestParams, bestScore);
        } catch (IOException e) {
            log.error("Exception during optimization: ", e);
        }
    }

    private static double getAvgPerformance(Configuration config, List<MatchStep> matchSteps, List<Metric> metrics, TablePairsGenerator tablePairsGenerator, SimilarityFlooding similarityFlooding, ThresholdSelectionBoosting thresholdSelectionBoosting) {
        List<Double> performances = new ArrayList<>();
        for (Configuration.DatasetConfiguration datasetConfig : config.getDatasetConfigurations()) {
            Dataset dataset = new Dataset(datasetConfig);
            List<Float> scenarioPerformances = new ArrayList<>();

            for (String scenarioName : dataset.getScenarioNames()) {
                Scenario scenario = new Scenario(dataset.getPath() + File.separator + scenarioName);
                MatchTask matchTask = new MatchTask(dataset, scenario, matchSteps, metrics);
                List<TablePair> tablePairs = tablePairsGenerator.generateCandidates(scenario);

                float[][] results = similarityFlooding.match(matchTask, null);
                results = thresholdSelectionBoosting.run(matchTask, null, results);
                matchTask.setTablePairs(tablePairs);
                matchTask.readGroundTruth();

                Evaluator evaluator = new Evaluator(metrics, scenario, matchTask.getGroundTruthMatrix());
                Performance performance = evaluator.evaluate(results).get(metrics.get(0));
                scenarioPerformances.add(performance.getGlobalScore());
            }

            double avgPerformance = scenarioPerformances.stream().mapToDouble(Float::doubleValue).average().orElse(0.0);
            performances.add(avgPerformance);
        }
        return performances.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private static Map<String, String> parseParameters(String input) {
        Map<String, String> params = new HashMap<>();
        input = input.replace("{", "").replace("}", "");
        for (String pair : input.split(", ")) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                params.put(kv[0].trim(), kv[1].trim());
            }
        }
        return params;
    }
}
