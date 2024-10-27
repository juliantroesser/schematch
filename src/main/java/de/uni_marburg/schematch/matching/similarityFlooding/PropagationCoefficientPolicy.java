package de.uni_marburg.schematch.matching.similarityFlooding;

import org.jgrapht.Graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;

public enum PropagationCoefficientPolicy {

    INVERSE_AVERAGE {
        @Override
        public List<Map<String, Double>> evaluate(Node nodeGraph1, Node nodeGraph2, Graph<Node, LabelEdge> graph1, Graph<Node, LabelEdge> graph2) throws NoSuchMethodException {

            // Count incoming and outgoing labels for nodes in each graph
            Map<String, Double> countInLabelsGraph1 = countLabels(graph1, nodeGraph1, true);
            Map<String, Double> countOutLabelsGraph1 = countLabels(graph1, nodeGraph1, false);
            Map<String, Double> countInLabelsGraph2 = countLabels(graph2, nodeGraph2, true);
            Map<String, Double> countOutLabelsGraph2 = countLabels(graph2, nodeGraph2, false);

            // Combine counts from both graphs
            BinaryOperator<Double> sumMergeFunction = Double::sum;
            Map<String, Double> countInLabelsTotal = mergeMaps(countInLabelsGraph1, countInLabelsGraph2, sumMergeFunction);
            Map<String, Double> countOutLabelsTotal = mergeMaps(countOutLabelsGraph1, countOutLabelsGraph2, sumMergeFunction);

            // Inverse label values
            invertMapValues(countInLabelsTotal, 2.0);
            invertMapValues(countOutLabelsTotal, 2.0);

            return List.of(countInLabelsTotal, countOutLabelsTotal);
        }
    },

    INVERSE_PRODUCT {
        @Override
        public List<Map<String, Double>> evaluate(Node nodeGraph1, Node nodeGraph2, Graph<Node, LabelEdge> graph1, Graph<Node, LabelEdge> graph2) throws NoSuchMethodException {

            // Count incoming and outgoing labels for nodes in each graph
            Map<String, Double> countInLabelsGraph1 = countLabels(graph1, nodeGraph1, true);
            Map<String, Double> countOutLabelsGraph1 = countLabels(graph1, nodeGraph1, false);
            Map<String, Double> countInLabelsGraph2 = countLabels(graph2, nodeGraph2, true);
            Map<String, Double> countOutLabelsGraph2 = countLabels(graph2, nodeGraph2, false);

            // Combine counts from both graphs
            BinaryOperator<Double> productMergeFunction = (a, b) -> a * b;
            Map<String, Double> countInLabelsTotal = mergeMaps(countInLabelsGraph1, countInLabelsGraph2, productMergeFunction);
            Map<String, Double> countOutLabelsTotal = mergeMaps(countOutLabelsGraph1, countOutLabelsGraph2, productMergeFunction);

            // Inverse label values
            invertMapValues(countInLabelsTotal, 1.0);
            invertMapValues(countOutLabelsTotal, 1.0);

            return List.of(countInLabelsTotal, countOutLabelsTotal);
        }
    },

    CONSTANT_ONE {
        @Override
        public List<Map<String, Double>> evaluate(Node nodeGraph1, Node nodeGraph2, Graph<Node, LabelEdge> graph1, Graph<Node, LabelEdge> graph2) throws NoSuchMethodException {
            return new ArrayList<>();
        }
    },

    CONSTANT_POINT_FIVE {
        @Override
        public List<Map<String, Double>> evaluate(Node nodeGraph1, Node nodeGraph2, Graph<Node, LabelEdge> graph1, Graph<Node, LabelEdge> graph2) throws NoSuchMethodException {
            return new ArrayList<>();
        }
    };

    public List<Map<String, Double>> evaluate(Node nodeGraph1, Node nodeGraph2, Graph<Node, LabelEdge> graph1, Graph<Node, LabelEdge> graph2) throws NoSuchMethodException {
        throw new NoSuchMethodException("Not Implemented");
    }

    // Method to count either incoming or outgoing labels
    private static Map<String, Double> countLabels(Graph<Node, LabelEdge> graph, Node node, boolean incoming) {
        Map<String, Double> labelCount = new HashMap<>();
        for (LabelEdge edge : incoming ? graph.incomingEdgesOf(node) : graph.outgoingEdgesOf(node)) {
            String label = edge.getLabel();
            labelCount.put(label, labelCount.getOrDefault(label, 0.0) + 1);
        }
        return labelCount;
    }

    // Method to merge two maps with a specified merge function
    private static Map<String, Double> mergeMaps(Map<String, Double> map1, Map<String, Double> map2, BinaryOperator<Double> mergeFunction) {
        Map<String, Double> mergedMap = new HashMap<>(map1);
        map2.forEach((key, value) -> mergedMap.merge(key, value, mergeFunction));
        return mergedMap;
    }

    // Method to invert values in a map, with error handling for division by zero
    private static void invertMapValues(Map<String, Double> map, double numerator) {
        map.replaceAll((key, value) -> {
            if (value != 0.0) {
                return numerator / value;
            } else {
                System.out.println("Warning: Division by zero for key " + key);
                return 0.0;
            }
        });
    }

}
