package de.uni_marburg.schematch.matching.similarityFlooding;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.data.Database;
import de.uni_marburg.schematch.data.Table;
import de.uni_marburg.schematch.matching.Matcher;
import de.uni_marburg.schematch.matchtask.MatchTask;
import de.uni_marburg.schematch.matchtask.matchstep.MatchStep;
import de.uni_marburg.schematch.matchtask.matchstep.MatchingStep;
import de.uni_marburg.schematch.similarity.string.Levenshtein;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;

import java.lang.reflect.Field;
import java.util.*;

import static de.uni_marburg.schematch.matching.similarityFlooding.SchemaGraphBuilder.createConnectivityGraph;
import static de.uni_marburg.schematch.matching.similarityFlooding.SchemaGraphBuilder.inducePropagationGraph;
import static de.uni_marburg.schematch.matching.similarityFlooding.SimilarityFloodingUtils.*;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class SimilarityFlooding extends Matcher {

    private static final Logger log = LogManager.getLogger(SimilarityFlooding.class);

    //Fixed parameters
    private static final double LABEL_SCORE_WEIGHT = 0.5;
    public static final double SELECT_THRESHOLD_WEIGHT = 0.95;
    private static final double PRECISION_OF_CONVERGENCE = 0.0001;
    private static final int MAX_ITERATIONS = 200;

    //Free parameters
    private String propCoeffPolicy;
    private String fixpoint;
    private String uccFilterThreshold;
    private String indFilterThreshold;
    private String fdFilterThreshold;

    private static Map<NodePair, Double> similarityFlooding
            (Graph<NodePair, CoefficientEdge> propagationGraph, Map<NodePair, Double> initialMapping, FixpointFormula formula) {

        //Termination conditions
        boolean convergence = false;
        int iterationCount = 0;

        //First, current and next similarity maps
        Map<NodePair, Double> sigma_0 = new HashMap<>();
        for (NodePair node : propagationGraph.vertexSet()) {
            sigma_0.put(node, initialMapping.getOrDefault(node, 0.0));
        }

        Map<NodePair, Double> sigma_i_plus_1 = new HashMap<>();
        Map<NodePair, Double> sigma_i = new HashMap<>(sigma_0);

        while (!convergence && iterationCount <= MAX_ITERATIONS) {

            double maxValueCurrentIteration = Double.MIN_VALUE;

            for (NodePair node : sigma_0.keySet()) {

                //Alle Nachbarn bekommen
                Set<CoefficientEdge> incomingEdges = propagationGraph.incomingEdgesOf(node); //Only neighbors from incoming edges
//                Set<NodePair> neighborNodes = incomingEdges.stream().map(propagationGraph::getEdgeSource).collect(Collectors.toSet());
                Set<NodePair> neighborNodes = new HashSet<>();
                for (CoefficientEdge edge : incomingEdges) {
                    NodePair source = propagationGraph.getEdgeSource(edge);
                    neighborNodes.add(source);
                }

                //Neuen Wert für Node auf Basis der Nachbarn berechnen
                double newValue;
                try {
                    newValue = formula.evaluate(node, neighborNodes, sigma_0, sigma_i, propagationGraph);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }

                //MaxWert der aktuellen Iteration speichern, damit später normalisieren möglich
                if (newValue > maxValueCurrentIteration) {
                    maxValueCurrentIteration = newValue;
                }

                sigma_i_plus_1.put(node, newValue);
            }

            //Normalisieren für die aktuelle Iteration
            for (NodePair node : sigma_0.keySet()) {
                sigma_i_plus_1.put(node, (sigma_i_plus_1.get(node) / maxValueCurrentIteration));
            }

            //Prüfen ob Residuum(sigma_i, sigma_i+1) konvergiert
            convergence = hasConverged(sigma_i, sigma_i_plus_1, PRECISION_OF_CONVERGENCE);
            sigma_i.putAll(sigma_i_plus_1);
            iterationCount++;
        }

        log.info("Ran {} Iterations for {} Nodes", iterationCount - 1, initialMapping.size());
        return sigma_i_plus_1;
    }

    public static Map<String, Collection<String>> getPossibleValues() {
        Map<String, Collection<String>> possibleValues = new HashMap<>();
        for (Param param : Param.values()) {
            possibleValues.put(param.key, param.possibleValues);
        }
        return possibleValues;
    }

    @Override
    public float[][] match(MatchTask matchTask, MatchingStep matchStep) {

        PropagationCoefficientPolicy policy;
        FixpointFormula formula;

        policy = switch (propCoeffPolicy) {
            case "INV_AVG" -> PropagationCoefficientPolicy.INVERSE_AVERAGE;
            case "INV_PROD" -> PropagationCoefficientPolicy.INVERSE_PRODUCT;
            case "CONSTANT_ONE" -> PropagationCoefficientPolicy.CONSTANT_ONE;
            default -> throw new RuntimeException("No such propagation coefficient policy: " + propCoeffPolicy);
        };

        formula = switch (fixpoint) {
            case "BASIC" -> FixpointFormula.BASIC;
            case "A" -> FixpointFormula.FORMULA_A;
            case "B" -> FixpointFormula.FORMULA_B;
            case "C" -> FixpointFormula.FORMULA_C;
            case "BASIC_Lambda" -> FixpointFormula.BASIC_Lambda;
            case "A_Lambda" -> FixpointFormula.FORMULA_A_Lambda;
            case "B_Lambda" -> FixpointFormula.FORMULA_B_Lambda;
            case "C_Lambda" -> FixpointFormula.FORMULA_C_Lambda;
            default -> throw new RuntimeException("No such fixpoint formula: " + fixpoint);
        };

        float[][] simMatrix = matchTask.getEmptySimMatrix();

        Database sourceDb = matchTask.getScenario().getSourceDatabase();
        Database targetDb = matchTask.getScenario().getTargetDatabase();

//        if (this.indFilterThreshold == null) {
//            log.warn("No IND filter threshold specified. Using default value: {}", IND_FILTER_THRESHOLD_DEFAULT);
//            this.indFilterThreshold = IND_FILTER_THRESHOLD_DEFAULT;
//        }
//
//        if (this.fdFilterThreshold == null) {
//            log.warn("No FD filter threshold specified. Using default value: {}", FD_FILTER_THRESHOLD_DEFAULT);
//            this.fdFilterThreshold = FD_FILTER_THRESHOLD_DEFAULT;
//        }
//
//        if (this.uccFilterThreshold == null) {
//            log.warn("No UCC filter threshold specified. Using default value: {}", UCC_FILTER_THRESHOLD_DEFAULT);
//            this.uccFilterThreshold = UCC_FILTER_THRESHOLD_DEFAULT;
//        }

        List<Matcher> matcher = matchTask.getFirstLineMatchers();
        Matcher firstLineMatcher = matcher.get(0);
        float [][] prevMatrix = matchTask.getSimMatrixFromPreviousMatchStep(matchStep, firstLineMatcher);

        SchemaGraphBuilder schemaGraphBuilder = new SchemaGraphBuilder(this.uccFilterThreshold, this.indFilterThreshold, this.fdFilterThreshold);

        Graph<Node, LabelEdge> sourceGraph = schemaGraphBuilder.transformIntoGraphRepresentationSchema(sourceDb);
        Graph<Node, LabelEdge> targetGraph = schemaGraphBuilder.transformIntoGraphRepresentationSchema(targetDb);

        //Combine both Graphs into a connectivity-graph
        Graph<NodePair, LabelEdge> connectivityGraph = createConnectivityGraph(sourceGraph, targetGraph);

        //Transform the connectivity-graph into the propagation-graph on which the algorithm executes
        Graph<NodePair, CoefficientEdge> propagationGraph = inducePropagationGraph(connectivityGraph, sourceGraph, targetGraph, policy);

        //Calculate the initial mapping (similarity) values
        Map<NodePair, Double> initialMapping = buildInitialMappingFromMatrix(prevMatrix,sourceDb, targetDb);

        //Run the similarity-flooding algorithm
        Map<NodePair, Double> floodingResults = similarityFlooding(propagationGraph, initialMapping, formula);

        //Apply constraints/filters to the result
        Map<NodePair, Double> filteredFloodingResults = filterMapping(floodingResults);

        for (Table sourceTable : matchTask.getScenario().getSourceDatabase().getTables()) {
            for (Table targetTable : matchTask.getScenario().getTargetDatabase().getTables()) {
                populateSimMatrix(simMatrix, filteredFloodingResults, sourceTable, targetTable);
            }
        }

        return simMatrix;
    }

    //TODO: Make function a firstLine matcher for better comparison
    private Map<NodePair, Double> calculateInitialMapping(Graph<NodePair, CoefficientEdge> propagationGraph) {

        Map<NodePair, Double> initialMapping = new HashMap<>();

        for (NodePair mappingPair : propagationGraph.vertexSet()) {
            Node node1 = mappingPair.getFirstNode();
            Node node2 = mappingPair.getSecondNode();
            Levenshtein l = new Levenshtein();
            double similarity;

            //All Pairs with ID Nodes get assigned a initial similarity of 0.0
            if (node1.getValue().startsWith("FD") && node2.getValue().startsWith("FD")) {
                similarity = 0.0;
            } else if (node1.getValue().startsWith("UCC") && node2.getValue().startsWith("UCC")) {
                similarity = 0.0;
            } else if (node1.getValue().startsWith("IND") && node2.getValue().startsWith("IND")) {
                similarity = 0.0;
            } else if (node1.isIDNode() || node2.isIDNode()) {
                similarity = 0.0;

            //Otherwise if value Nodes assign combination of LabelScore and ValueScore (if applicable)
            } else {
                Column column1 = node1.getRepresentedColumn();
                Column column2 = node2.getRepresentedColumn();
                double labelSimilarity = l.compare(node1.getValue(), node2.getValue());

                if (column1 == null || column2 == null) {
                    similarity = labelSimilarity;
                } else {
                    //First Line Matcher
                    double labelWeight = LABEL_SCORE_WEIGHT;
                    double valueSimilarity = getValueSimilarityBetweenColumns(column1, column2);
                    similarity = labelWeight * labelSimilarity + (1 - labelWeight) * valueSimilarity;
                }
            }

            initialMapping.put(mappingPair, similarity);

        }
        return initialMapping;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(getClass().getSimpleName());
        result.append("(");

        Field[] fields = getClass().getDeclaredFields();

        for (int i = 1; i < fields.length; i++) {
            try {
                fields[i].setAccessible(true);
                result.append(fields[i].getName()).append("=").append(fields[i].get(this)).append(" &  ");
                fields[i].setAccessible(false);
            } catch (IllegalAccessException ignored) {
            } // Cannot happen, we have set the field to be accessible
        }
        String res = result.toString();
        if (getClass().getDeclaredFields().length > 0) {
            res = res.substring(0, res.length() - 4);
        }
        return res + ")";
    }

    public Map<String, String> getParameters() {
        Map<String, String> parameters = new HashMap<>();
        for (Param param : Param.values()) {
            try {
                Field field = this.getClass().getDeclaredField(param.key);
                field.setAccessible(true);
                parameters.put(param.key, (String) field.get(this));
            } catch (Exception ignored) {
            }
        }
        return parameters;
    }

    public void setParameters(Map<String, String> currentParams) {
        for (Param param : Param.values()) {
            try {
                Field field = this.getClass().getDeclaredField(param.key);
                field.setAccessible(true);
                field.set(this, currentParams.get(param.key));
            } catch (Exception ignored) {
            }
        }
    }


    private Map<NodePair, Double> buildInitialMappingFromMatrix(float[][] simMatrix, Database sourceDb, Database targetDb) {
        Map<NodePair, Double> initialMapping = new HashMap<>();

        for (Table sourceTable : sourceDb.getTables()) {
            for (Table targetTable : targetDb.getTables()) {

                List<Column> sourceColumns = sourceTable.getColumns();
                List<Column> targetColumns = targetTable.getColumns();

                for (int i = 0; i < sourceColumns.size(); i++) {

                    String sourceLabel = sourceColumns.get(i).getLabel();
                    Node sourceNode = new Node(sourceLabel, NodeType.COLUMN, null, false, null, sourceTable, null);

                    for (int j = 0; j < targetColumns.size(); j++) {

                        String targetLabel = targetColumns.get(j).getLabel();
                        Node targetNode = new Node(targetLabel, NodeType.COLUMN, null, false, null, targetTable, null);

                        float value = simMatrix[sourceTable.getOffset() + i] [targetTable.getOffset() + j];
                        initialMapping.put(new NodePair(sourceNode,targetNode), Double.valueOf(value));
                    }
                }
            }
        }

        return initialMapping;
    }



    public enum Param {
        PROP_COEFF_POLICY("propCoeffPolicy", List.of("INV_AVG", "INV_PROD")),
        FIXPOINT("fixpoint", List.of("A", "B", "C")),
//        IND_FILTER_THRESHOLD("indFilterThreshold", List.of("normalizedValue")),
        FD_FILTER("fdFilter", List.of("ngpdep", "alt_ngpdep_sum", "alt_ngpdep_max"));
//        FD_FILTER_THRESHOLD("fdFilterThreshold", List.of("normalizedValue"));
//        UCC_FILTER_THRESHOLD("uccFilterThreshold", List.of("normalizedValue"));

        public final String key;
        public final Collection<String> possibleValues;

        Param(String key, Collection<String> possibleValues) {
            this.key = key;
            this.possibleValues = possibleValues;
        }
    }

}
