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
import java.util.ArrayList;
import java.util.List;

public class BayesianOptimization {
    private static final Logger log = LogManager.getLogger(BayesianOptimization.class);
    private static final String CLIENT_HOST = "localhost";
    private static final int SEND_PORT = 5003;
    private static final int LISTEN_PORT = 5005;
    private final Socket sendClient;
    private final PrintWriter sendWriter;  // Persistent writer for sending data
    private final ServerSocket listenServer;
    private final SimilarityFlooding sf;
    private final Configuration config;

    public BayesianOptimization() {
        this.listenServer = initListenServer(LISTEN_PORT);
        this.sendClient = initSendClient(CLIENT_HOST, SEND_PORT);
        // Create a persistent PrintWriter without auto-closing the stream
        try {
            this.sendWriter = new PrintWriter(new OutputStreamWriter(sendClient.getOutputStream()), true);
        } catch (IOException e) {
            log.error("Failed to get output stream from sendClient: ", e);
            throw new RuntimeException(e);
        }
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

    private static ServerSocket initListenServer(int serverPort) {
        log.info("Initializing server on port {}", serverPort);
        try {
            return new ServerSocket(serverPort);
        } catch (IOException e) {
            log.error("Failed to start server: ", e);
            throw new RuntimeException(e);
        }
    }

    private Socket initSendClient(String clientHost, int clientPort) {
        log.info("Connecting to client at {}:{}", clientHost, clientPort);
        try {
            return new Socket(clientHost, clientPort);
        } catch (IOException e) {
            log.error("Failed to connect to client: ", e);
            throw new RuntimeException(e);
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

        // Use the persistent sendWriter without closing it.
        log.info("Sending initial data using persistent connection");
        sendWriter.println(initialData);
        sendWriter.flush();
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
        log.info("Starting listening");
        try (Socket socket = listenServer.accept(); BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            log.info("Accepted connection, entering message loop");
            while (true) {
                log.info("Waiting for input");
                String inputLine = in.readLine();
                if (inputLine == null) {
                    log.info("Connection closed by client");
                    break;
                }
                log.info("Input: " + inputLine);

                JSONObject json = new JSONObject(inputLine);
                log.info("Received JSON: " + json);

                // Update parameters based on received JSON
                this.sf.setFixpoint(json.getString("fixpoint"));
                this.sf.setFDV1(json.getString("FDV1"));
                this.sf.setFDV2(json.getString("FDV2"));
                this.sf.setINDV1(json.getString("INDV1"));
                this.sf.setINDV2(json.getString("INDV2"));
                this.sf.setUCCV1(json.getString("UCCV1"));
                this.sf.setUCCV2(json.getString("UCCV2"));

                log.info("Calculating new score");
                double score = getAvgPerformance();
                log.info("New score: " + score);

                JSONObject response = new JSONObject();
                response.put("score", score);

                // Use the persistent sendWriter to send the response.
                log.info("Sending response: " + response);
                sendWriter.println(response);
                sendWriter.flush();
            }
            log.info("Socket closed");
        } catch (Exception e) {
            log.error("Error in listening: ", e);
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
