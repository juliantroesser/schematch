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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        similarityFlooding.setFixpoint("C");
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

        //Loop over dataset
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
    }
}
