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
import org.json.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class BayesianOptimization {
    private static final Logger log = LogManager.getLogger(BayesianOptimization.class);
    private static final String CLIENT_HOST = "localhost";
    private static final int SEND_PORT = 5004;
    private static final int LISTEN_PORT = 5005;
    private static Process pythonProcess;
    private final Socket sendClient;
    private final ServerSocket listenServer;
    private final SimilarityFlooding sf;
    private final Configuration config;

    public BayesianOptimization() {
        this.listenServer = initListenServer(LISTEN_PORT);
        this.sendClient = initSendClient(CLIENT_HOST, SEND_PORT);
        this.sf = new SimilarityFlooding();
        this.config = Configuration.getInstance();
    }

    public static void main(String[] args) {
        log.info("Starting BayesianOptimization");
        var bo = new BayesianOptimization();

        log.info("Configuring SimilarityFlooding");
        bo.configureSimilarityFlooding();

        log.info("Starting Listener");
        var thread = bo.startListingThread();
        log.info("Sending initial data to client");
        bo.sendInitialData();
        thread.start();
        // Wait for the thread to finish
        try {
            thread.join();
        } catch (InterruptedException e) {
            log.error("Error waiting for thread: ", e);
        }
    }

    private void sendInitialData() {
        log.info("Collecting initial data");
        var score = getAvgPerformance();
        var currentParams = sf.getParameters();
        var possibleParams = sf.getPossibleValues();

        log.info("Converted initial data to JSON");
        var initialData = new JSONObject();
        initialData.put("score", score);
        initialData.put("current_params", currentParams);
        initialData.put("possible_values", possibleParams);
        log.info("JSON DATA: {}", initialData);

        log.info("Starting output stream");
        try (OutputStream output = sendClient.getOutputStream(); PrintWriter writer = new PrintWriter(new OutputStreamWriter(output), true)) {
            log.info("Writing initial data");
            writer.println(initialData);
            log.info("Flushing writer");
            writer.flush();
        } catch (IOException e) {
            log.error("Failed to send initial data: ", e);
            throw new RuntimeException(e);
        }
    }

    private static ServerSocket initListenServer(int serverPort) {
        log.info("Initializing server on port {}", serverPort);
        try {
            return new ServerSocket(serverPort);
        } catch (IOException e) {
            log.error("Failed to start server: ", e);
            throw new RuntimeException(e);
        }
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

    private void configureSimilarityFlooding() {
        this.sf.setWholeSchema("true");
        this.sf.setPropCoeffPolicy("INV_PROD");
        this.sf.setFixpoint("A");
        this.sf.setFDV1("false");
        this.sf.setFDV2("false");
        this.sf.setUCCV1("false");
        this.sf.setUCCV2("false");
        this.sf.setINDV1("false");
        this.sf.setINDV2("false");
    }

//    private static void optimizeParameters(SimilarityFlooding similarityFlooding, ThresholdSelectionBoosting thresholdSelectionBoosting) {
//        try (Socket socket = new Socket(HOST, PORT); BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
//
//            Map<String, String> currentParams = similarityFlooding.getParameters();
//            Map<String, Collection<String>> possibleParams = similarityFlooding.getPossibleValues();
//            double bestScore = 0.0;
//            Map<String, String> bestParams = new HashMap<>();
//
//            double avgPerformance = getAvgPerformance(thresholdSelectionBoosting);
//
//            var jsonString = convertToJsonString(currentParams);
//            var jsonString2 = convertToJsonString2(possibleParams);
//
//            log.info("Aktuelle Parameter: {}, Score: {}", currentParams, avgPerformance);
//            writer.println(avgPerformance + "|" + jsonString + "|" + jsonString2);
//
////            String inputLine;
////            StringBuilder response = new StringBuilder();
////            while ((inputLine = reader.readLine()) != null) {
////                response.append(inputLine);
////            }
////            log.info("Response: {}", response);
//
//
////                log.info("Response: {}", response);
////                if (response.equals("DONE")) {
////                    break;
////                }
////
//////                currentParams = parseParameters(response);
////                similarityFlooding.setParameters(currentParams);
////
////                if (avgPerformance > bestScore) {
////                    bestScore = avgPerformance;
////                    bestParams = new HashMap<>(currentParams);
////                }
//
//            log.info("Optimierung abgeschlossen. Beste Parameter: {}, Score: {}", bestParams, bestScore);
//        } catch (IOException e) {
//            log.error("Exception during optimization: ", e);
//        }
//
//        // new server to recive the parameters
//        try {
//            int port = 5003;
//            try (ServerSocket serverSocket = new ServerSocket(port)) {
//                System.out.println("Server started and listening on port " + port);
//                while (true) {
//                    try (Socket clientSocket = serverSocket.accept(); BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
//
//                        System.out.println("Client connected");
//
//                        // Read the input from the client
//                        String inputLine;
//                        StringBuilder data = new StringBuilder();
//                        while ((inputLine = in.readLine()) != null) {
//                            data.append(inputLine);
//                        }
//
//                        // Parse the JSON data
//                        JSONObject json = new JSONObject(data.toString());
//                        System.out.println("Received JSON: " + json);
//
//                        // Process the JSON data as needed
//                        // For example, extract parameters
//                        String fixpoint = json.getString("fixpoint");
//                        boolean indv2 = json.getBoolean("INDV2");
//                        // ... extract other parameters
//
//                        // Send a response back to the client
//                        out.println("Response from Java server");
//
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//
//
//        /*
//        import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.io.PrintWriter;
//import java.net.ServerSocket;
//import java.net.Socket;
//import org.json.JSONObject;
//
//public class JavaServer {
//    public static void main(String[] args) {
//        int port = 5003;
//        try (ServerSocket serverSocket = new ServerSocket(port)) {
//            System.out.println("Server started and listening on port " + port);
//            while (true) {
//                try (Socket clientSocket = serverSocket.accept();
//                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
//                     PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
//
//                    System.out.println("Client connected");
//
//                    // Read the input from the client
//                    String inputLine;
//                    StringBuilder data = new StringBuilder();
//                    while ((inputLine = in.readLine()) != null) {
//                        data.append(inputLine);
//                    }
//
//                    // Parse the JSON data
//                    JSONObject json = new JSONObject(data.toString());
//                    System.out.println("Received JSON: " + json.toString());
//
//                    // Process the JSON data as needed
//                    // For example, extract parameters
//                    String fixpoint = json.getString("fixpoint");
//                    boolean indv2 = json.getBoolean("INDV2");
//                    // ... extract other parameters
//
//                    // Send a response back to the client
//                    out.println("Response from Java server");
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
//         */
//    }

//    private static String convertToJsonString2(Map<String, Collection<String>> possibleParams) {
//        var jsonString = new StringBuilder("{");
//        for (Iterator<Map.Entry<String, Collection<String>>> iterator = possibleParams.entrySet().iterator(); iterator.hasNext(); ) {
//            Map.Entry<String, Collection<String>> entry = iterator.next();
//            jsonString.append("\"").append(entry.getKey()).append("\": [");
//            for (Iterator<String> iter = entry.getValue().iterator(); iter.hasNext(); ) {
//                String value = iter.next();
//                jsonString.append("\"").append(value);
//                if (iter.hasNext()) {
//                    jsonString.append("\", ");
//                } else {
//                    jsonString.append("\"");
//                }
//            }
//            jsonString.append("]");
//            if (iterator.hasNext()) {
//                jsonString.append(", ");
//            }
//        }
//        jsonString.append("}");
//        return jsonString.toString();
//    }
//
//    private static String convertToJsonString(Map<String, String> currentParams) {
//        var jsonString = new StringBuilder("{");
//        for (Iterator<Map.Entry<String, String>> iterator = currentParams.entrySet().iterator(); iterator.hasNext(); ) {
//            Map.Entry<String, String> entry = iterator.next();
//            jsonString.append("\"").append(entry.getKey()).append("\": \"").append(entry.getValue());
//            if (iterator.hasNext()) {
//                jsonString.append("\", ");
//            } else {
//                jsonString.append("\"");
//            }
//        }
//        jsonString.append("}");
//        return jsonString.toString();
//    }
//
//    private static Map<String, String> parseParameters(String input) {
//        Map<String, String> params = new HashMap<>();
//        input = input.replace("{", "").replace("}", "");
//        for (String pair : input.split(", ")) {
//            String[] kv = pair.split("=");
//            if (kv.length == 2) {
//                params.put(kv[0].trim(), kv[1].trim());
//            }
//        }
//        return params;
//    }

    private Socket initSendClient(String clientHost, int clientPort) {
        log.info("Connecting to client at {}:{}", clientHost, clientPort);
        try {
            return new Socket(clientHost, clientPort);
        } catch (IOException e) {
            log.error("Failed to connect to client: ", e);
            throw new RuntimeException(e);
        }
    }

    private Thread startListingThread() {
        return new Thread(() -> {
            try {
                startListening();
            } catch (Exception e) {
                log.error("Error in listening thread: ", e);
            }
        });
    }

    private void startListening() {
        /*
        1. Start a server socket on a port
        2. Accept incoming connections
        3. Read the input from the client
        4. Parse the JSON data
        5. Calculate new score
        6. Send a response back to the client via the sendClient socket
        7. Repeat steps 3-6 until the client closes the connection
         */


        // Server already started in the constructor

        // Accept incoming connections
        log.info("Starting listening");
        try (var socket = listenServer.accept()) {

            // Repeat steps 3-6 until the client closes the connection
            log.info("Starting loop");
            while (!socket.isClosed()) {

                // Read the input from the client
                log.info("Initializing input stream");
                try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    String inputLine;
                    StringBuilder data = new StringBuilder();
                    log.info("Waiting for input");
                    while ((inputLine = in.readLine()) != null) {
                        data.append(inputLine);
                    }
                    log.info("Input: " + data.toString());

                    // Parse the JSON data
                    JSONObject json = new JSONObject(data.toString());
                    log.info("Received JSON: " + json);

                    log.info("Setting new parameters");
                    // Calculate new score
                    this.sf.setWholeSchema(json.getString("wholeSchema"));
                    this.sf.setPropCoeffPolicy(json.getString("propCoeffPolicy"));
                    this.sf.setFixpoint(json.getString("fixpoint"));
                    this.sf.setFDV1(json.getString("FDV1"));
                    this.sf.setFDV2(json.getString("FDV2"));
                    this.sf.setINDV1(json.getString("INDV1"));
                    this.sf.setINDV2(json.getString("INDV2"));
                    this.sf.setUCCV1(json.getString("UCCV1"));
                    this.sf.setUCCV2(json.getString("UCCV2"));

                    log.info("Calculating new score");
                    var score = getAvgPerformance();
                    log.info("New score: " + score);

                    log.info("Building JSONObject");
                    var response = new JSONObject();
                    response.put("result", score);

                    // Send a response back to the client using the sendClient socket
                    log.info("Initializing output stream");
                    try (OutputStream output = sendClient.getOutputStream(); PrintWriter writer = new PrintWriter(new OutputStreamWriter(output), true)) {
                        log.info("Writing initial data");
                        writer.println(response);
                        log.info("Flushing writer");
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            log.info("Socket closed");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double getAvgPerformance() {
        List<Double> performances = new ArrayList<>();
        List<MatchStep> matchSteps = new ArrayList<>();
        List<Metric> metrics = List.of(new F1Score());
        ThresholdSelectionBoosting thresholdSelectionBoosting = new ThresholdSelectionBoosting(0.95);
        TablePairsGenerator tablePairsGenerator = new NaiveTablePairsGenerator();
        for (Configuration.DatasetConfiguration datasetConfig : config.getDatasetConfigurations()) {
            Dataset dataset = new Dataset(datasetConfig);
            List<Float> scenarioPerformances = new ArrayList<>();

            for (String scenarioName : dataset.getScenarioNames()) {
                Scenario scenario = new Scenario(dataset.getPath() + File.separator + scenarioName);
                MatchTask matchTask = new MatchTask(dataset, scenario, matchSteps, metrics);
                List<TablePair> tablePairs = tablePairsGenerator.generateCandidates(scenario);

                float[][] results = this.sf.match(matchTask, null);
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
}
