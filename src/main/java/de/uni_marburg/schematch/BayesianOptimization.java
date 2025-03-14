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
    private static final int PORT = 5003;
    private static Process pythonProcess;


    public static void main(String[] args) {
//        startPythonScript();

//        Runtime.getRuntime().addShutdownHook(new Thread(BayesianOptimization::stopPythonScript));

        Configuration config = Configuration.getInstance();
        List<MatchStep> matchSteps = new ArrayList<>();

        SimilarityFlooding similarityFlooding = new SimilarityFlooding();
        configureSimilarityFlooding(similarityFlooding);
        ThresholdSelectionBoosting thresholdSelectionBoosting = new ThresholdSelectionBoosting(0.95);
        List<Metric> metrics = List.of(new F1Score());
        TablePairsGenerator tablePairsGenerator = new NaiveTablePairsGenerator();

        optimizeParameters(config, matchSteps, metrics, tablePairsGenerator, similarityFlooding, thresholdSelectionBoosting);
//        stopPythonScript();
    }

    private static void startPythonScript() {
        log.info("Starting python script");
        try {
            ProcessBuilder pb = new ProcessBuilder("scripts/Optimizations/.venv/bin/python", "scripts/Optimizations/BayesianOptimization.py");
            pb.redirectErrorStream(true);
            pythonProcess = pb.start();

            // Chill for a bit to let the Python script start up
            Thread.sleep(2000);

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("Python: " + line);
                    }
                } catch (IOException e) {
                    log.error("Error reading Python output: ", e);
                }
            }).start();

            log.info("Python optimization script started.");
        } catch (IOException e) {
            log.error("Failed to start Python script: ", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void stopPythonScript() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            pythonProcess.destroy();
            log.info("Python script terminated.");
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
        try (Socket socket = new Socket(HOST, PORT); BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            Map<String, String> currentParams = similarityFlooding.getParameters();
            Map<String, Collection<String>> possibleParams = similarityFlooding.getPossibleValues();
            double bestScore = 0.0;
            Map<String, String> bestParams = new HashMap<>();

            while (true) {
                double avgPerformance = getAvgPerformance(config, matchSteps, metrics, tablePairsGenerator, similarityFlooding, thresholdSelectionBoosting);

                var jsonString = convertToJsonString(currentParams);
                var jsonString2 = convertToJsonString2(possibleParams);

                log.info("Aktuelle Parameter: {}, Score: {}", currentParams, avgPerformance);
                writer.println(avgPerformance + "|" + jsonString + "|" + jsonString2);

                var response = reader.lines();
                // log all responses
                response.forEach(log::info);


                log.info("Response: {}", response);
                if (response.equals("DONE")) {
                    break;
                }

//                currentParams = parseParameters(response);
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

        /*
        import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import org.json.JSONObject;

public class JavaServer {
    public static void main(String[] args) {
        int port = 5003;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started and listening on port " + port);
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                    System.out.println("Client connected");

                    // Read the input from the client
                    String inputLine;
                    StringBuilder data = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        data.append(inputLine);
                    }

                    // Parse the JSON data
                    JSONObject json = new JSONObject(data.toString());
                    System.out.println("Received JSON: " + json.toString());

                    // Process the JSON data as needed
                    // For example, extract parameters
                    String fixpoint = json.getString("fixpoint");
                    boolean indv2 = json.getBoolean("INDV2");
                    // ... extract other parameters

                    // Send a response back to the client
                    out.println("Response from Java server");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
         */
    }

    private static String convertToJsonString2(Map<String, Collection<String>> possibleParams) {
        var jsonString = new StringBuilder("{");
        for (Iterator<Map.Entry<String, Collection<String>>> iterator = possibleParams.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, Collection<String>> entry = iterator.next();
            jsonString.append("\"").append(entry.getKey()).append("\": [");
            for (Iterator<String> iter = entry.getValue().iterator(); iter.hasNext(); ) {
                String value = iter.next();
                jsonString.append("\"").append(value);
                if (iter.hasNext()) {
                    jsonString.append("\", ");
                } else {
                    jsonString.append("\"");
                }
            }
            jsonString.append("]");
            if (iterator.hasNext()) {
                jsonString.append(", ");
            }
        }
        jsonString.append("}");
        return jsonString.toString();
    }

    private static String convertToJsonString(Map<String, String> currentParams) {
        var jsonString = new StringBuilder("{");
        for (Iterator<Map.Entry<String, String>> iterator = currentParams.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, String> entry = iterator.next();
            jsonString.append("\"").append(entry.getKey()).append("\": \"").append(entry.getValue());
            if (iterator.hasNext()) {
                jsonString.append("\", ");
            } else {
                jsonString.append("\"");
            }
        }
        jsonString.append("}");
        return jsonString.toString();
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
