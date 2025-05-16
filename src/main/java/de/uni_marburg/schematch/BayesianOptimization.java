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

/**
 * Main class that drives the Bayesian optimization process.
 * It configures the Similarity Flooding algorithm, sends initial data to the client,
 * and listens for parameter updates to re-evaluate performance.
 */
public class BayesianOptimization {
    private static final Logger log = LogManager.getLogger(BayesianOptimization.class);
    private static final String CLIENT_HOST = "localhost";
    private static final int SEND_PORT = 5003;
    private static final int LISTEN_PORT = 5005;

    // Supporting components for communication and evaluation.
    private final CommunicationManager communicationManager;
    private final PerformanceEvaluator performanceEvaluator;
    private final SimilarityFlooding similarityFlooding;
    private final Configuration config;

    /**
     * Constructs the BayesianOptimization instance by initializing the communication manager,
     * the Similarity Flooding algorithm, and the performance evaluator.
     */
    public BayesianOptimization() {
        // Initialize communication channels.
        this.communicationManager = new CommunicationManager(CLIENT_HOST, SEND_PORT, LISTEN_PORT);
        // Initialize Similarity Flooding instance.
        this.similarityFlooding = new SimilarityFlooding();
        // Load configuration.
        this.config = Configuration.getInstance();
        // Initialize performance evaluator with a naive table pairs generator.
        this.performanceEvaluator = new PerformanceEvaluator(config, similarityFlooding, new NaiveTablePairsGenerator());
    }

    /**
     * Main method to run the Bayesian Optimization process.
     *
     * @param args Command-line arguments (unused)
     */
    public static void main(String[] args) {
        log.info("Starting Bayesian Optimization process");
        BayesianOptimization bo = new BayesianOptimization();

        // Configure Similarity Flooding with default parameters.
        bo.configureSimilarityFlooding();

        // Send initial data to the client.
        bo.sendInitialData();

        // Start the communication listener in a separate thread.
        Thread listenerThread = bo.communicationManager.startListening(bo::handleIncomingMessage);
        listenerThread.start();

        // Wait for the listener thread to finish.
        try {
            listenerThread.join();
        } catch (InterruptedException e) {
            log.error("Listener thread interrupted: ", e);
        }
    }

    /**
     * Configures the Similarity Flooding algorithm with default parameter settings.
     */
    private void configureSimilarityFlooding() {
        log.info("Configuring Similarity Flooding parameters");
        similarityFlooding.setPropCoeffPolicy("INV_PROD");
        similarityFlooding.setFixpoint("A");
        similarityFlooding.setFdFilter("all");
    }

    /**
     * Sends the initial data to the client including the performance score,
     * current parameters, and possible parameter values.
     */
    private void sendInitialData() {
        log.info("Sending initial data to client");
        double score = performanceEvaluator.evaluateAveragePerformance();

        JSONObject initialData = new JSONObject();
        initialData.put("score", score);
        initialData.put("current_params", similarityFlooding.getParameters());
        initialData.put("possible_values", similarityFlooding.getPossibleValues());

        log.info("Initial JSON data: {}", initialData);
        communicationManager.sendMessage(initialData);
    }

    /**
     * Callback for handling incoming JSON messages. Updates Similarity Flooding parameters
     * and sends back a recalculated performance score.
     *
     * @param message Incoming JSON message containing new parameter values.
     */
    private void handleIncomingMessage(JSONObject message) {
        log.info("Handling incoming message: {}", message);
        try {
            // Update Similarity Flooding parameters from the received JSON.
            similarityFlooding.setPropCoeffPolicy(message.getString("propCoeffPolicy"));
            similarityFlooding.setFixpoint(message.getString("fixpoint"));
            similarityFlooding.setFdFilter(message.getString("fdFilter"));
        } catch (Exception e) {
            log.error("Error updating parameters from incoming message: ", e);
        }

        // Evaluate new performance after updating parameters.
        double newScore = performanceEvaluator.evaluateAveragePerformance();
        log.info("New performance score calculated: {}", newScore);

        JSONObject response = new JSONObject();
        response.put("score", newScore);
        communicationManager.sendMessage(response);
    }
}

/**
 * Manages the socket-based communication with the client.
 * It encapsulates both the persistent connection for sending messages and the server socket for listening.
 */
class CommunicationManager {
    private static final Logger log = LogManager.getLogger(CommunicationManager.class);
    private final Socket sendSocket;
    private final PrintWriter messageSender;
    private final ServerSocket listenServer;

    /**
     * Constructs the CommunicationManager by initializing both the sending and listening sockets.
     *
     * @param clientHost The host address of the client.
     * @param sendPort   The port used for sending messages.
     * @param listenPort The port used for listening to incoming messages.
     */
    public CommunicationManager(String clientHost, int sendPort, int listenPort) {
        this.listenServer = initListenServer(listenPort);
        this.sendSocket = initSendSocket(clientHost, sendPort);
        this.messageSender = initMessageSender(sendSocket);
    }

    /**
     * Initializes a server socket to listen on the specified port.
     *
     * @param port The port number.
     * @return The initialized ServerSocket.
     */
    private ServerSocket initListenServer(int port) {
        log.info("Initializing listening server on port {}", port);
        try {
            return new ServerSocket(port);
        } catch (IOException e) {
            log.error("Failed to initialize listening server: ", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Connects to the client socket on the specified host and port.
     *
     * @param host The client host.
     * @param port The client port.
     * @return The connected Socket.
     */
    private Socket initSendSocket(String host, int port) {
        log.info("Connecting to client at {}:{}", host, port);
        try {
            return new Socket(host, port);
        } catch (IOException e) {
            log.error("Failed to connect to client: ", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Initializes the PrintWriter for the given socket.
     *
     * @param socket The socket for which the output stream is required.
     * @return A persistent PrintWriter for sending messages.
     */
    private PrintWriter initMessageSender(Socket socket) {
        try {
            return new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        } catch (IOException e) {
            log.error("Failed to get output stream from socket: ", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends a JSON message to the client using the persistent connection.
     *
     * @param message The JSON message to send.
     */
    public void sendMessage(JSONObject message) {
        log.info("Sending message: {}", message);
        messageSender.println(message);
        messageSender.flush();
    }

    /**
     * Starts a listening thread that waits for incoming connections and processes messages.
     *
     * @param messageHandler Callback to process each received JSON message.
     * @return The thread that is handling incoming messages.
     */
    public Thread startListening(java.util.function.Consumer<JSONObject> messageHandler) {
        return new Thread(() -> {
            log.info("Listening for incoming connections");
            try (Socket socket = listenServer.accept(); BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                log.info("Accepted connection from client");
                String line;
                // Process each incoming line as a JSON message.
                while ((line = reader.readLine()) != null) {
                    log.info("Received message: {}", line);
                    try {
                        JSONObject json = new JSONObject(line);
                        messageHandler.accept(json);
                    } catch (Exception e) {
                        log.error("Error processing incoming message: ", e);
                    }
                }
                log.info("Client disconnected from listening socket");
            } catch (IOException e) {
                log.error("Error in listening thread: ", e);
            }
        });
    }
}

/**
 * Evaluates the performance of the Similarity Flooding algorithm across all dataset configurations.
 * It iterates through each dataset and scenario, performs matching and boosting, and computes the average performance score.
 */
class PerformanceEvaluator {
    private static final Logger log = LogManager.getLogger(PerformanceEvaluator.class);
    private final Configuration config;
    private final SimilarityFlooding similarityFlooding;
    private final TablePairsGenerator tablePairsGenerator;
    private final List<Metric> metrics;

    /**
     * Constructs a PerformanceEvaluator.
     *
     * @param config              The configuration instance containing dataset details.
     * @param similarityFlooding  The Similarity Flooding instance to use for matching.
     * @param tablePairsGenerator The generator used to produce candidate table pairs.
     */
    public PerformanceEvaluator(Configuration config, SimilarityFlooding similarityFlooding, TablePairsGenerator tablePairsGenerator) {
        this.config = config;
        this.similarityFlooding = similarityFlooding;
        this.tablePairsGenerator = tablePairsGenerator;
        // Initialize with F1Score metric.
        this.metrics = List.of(new F1Score());
    }

    /**
     * Evaluates and returns the average performance score across all dataset configurations.
     *
     * @return The average performance score.
     */
    public double evaluateAveragePerformance() {
        List<Double> datasetPerformances = new ArrayList<>();
        ThresholdSelectionBoosting thresholdBoosting = new ThresholdSelectionBoosting(SimilarityFlooding.SELECT_THRESHOLD_WEIGHT);

        // Iterate over each dataset configuration.
        for (Configuration.DatasetConfiguration datasetConfig : config.getDatasetConfigurations()) {
            Dataset dataset = new Dataset(datasetConfig);
            List<Float> scenarioPerformances = new ArrayList<>();

            // Process each scenario within the dataset.
            for (String scenarioName : dataset.getScenarioNames()) {
                Scenario scenario = new Scenario(dataset.getPath() + File.separator + scenarioName);
                // Initialize a new MatchTask with an empty list of match steps.
                MatchTask matchTask = new MatchTask(dataset, scenario, new ArrayList<MatchStep>(), metrics);
                List<TablePair> tablePairs = tablePairsGenerator.generateCandidates(scenario);
                matchTask.setTablePairs(tablePairs);
                matchTask.readGroundTruth();

                // Run Similarity Flooding and boosting.
                float[][] results = similarityFlooding.match(matchTask, null);
                results = thresholdBoosting.run(matchTask, null, results);

                // Evaluate performance using the first metric (F1Score).
                Evaluator evaluator = new Evaluator(metrics, scenario, matchTask.getGroundTruthMatrix());
                Performance performance = evaluator.evaluate(results).get(metrics.get(0));
                scenarioPerformances.add(performance.getGlobalScore());
            }
            // Compute average performance for the current dataset.
            double avgScenarioPerformance = scenarioPerformances.stream().mapToDouble(Float::doubleValue).average().orElse(0.0);
            datasetPerformances.add(avgScenarioPerformance);
        }
        // Compute overall average performance.
        double avgPerformance = datasetPerformances.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        log.info("Calculated average performance: {}", avgPerformance);
        return avgPerformance;
    }
}
