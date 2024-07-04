package de.uni_marburg.schematch.matching.matrix_boosting.similarity_flooding;
import org.jgrapht.Graph;
import java.util.Map;
import java.util.Set;

/**
 * Enum Class that holds the FixpointFormulas for the SimilarityFlooding Algorithm
 * Each Object decides how to perform the calculations.
 */

public enum FixpointFormula {

    BASIC {
        @Override
        public double evaluate(NodePair node, Set<NodePair> neighborNodes, Map<NodePair, Double> sigma_0, Map<NodePair, Double> sigma_i, Graph<NodePair, CoefficientEdge> propagationGraph) {

            double phi = 0;

            for (NodePair neighbor : neighborNodes) {
                double neighborValue = sigma_i.get(neighbor);
                double propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();
                phi += neighborValue * propagationCoefficient;
            }

            return sigma_i.get(node) + phi;

        }
    },

    FORMULA_A {
        @Override
        public double evaluate(NodePair node, Set<NodePair> neighborNodes, Map<NodePair, Double> sigma_0, Map<NodePair, Double> sigma_i, Graph<NodePair, CoefficientEdge> propagationGraph) {

            double phi = 0;

            for (NodePair neighbor : neighborNodes) {
                double neighborValue = sigma_i.get(neighbor);
                double propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();
                phi += neighborValue * propagationCoefficient;
            }

            return sigma_0.get(node) + phi;

        }
    },

    FORMULA_B {
        @Override
        public double evaluate(NodePair node, Set<NodePair> neighborNodes, Map<NodePair, Double> sigma_0, Map<NodePair, Double> sigma_i, Graph<NodePair, CoefficientEdge> propagationGraph) {

            double phi = 0;

            for (NodePair neighbor : neighborNodes) {

                double neighborValue_0 = sigma_0.get(neighbor);
                double neighborValue_i = sigma_i.get(neighbor);
                double propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();

                phi += (neighborValue_0 + neighborValue_i) * propagationCoefficient;
            }

            return phi;

        }
    },

    FORMULA_C {
        @Override
        public double evaluate(NodePair node, Set<NodePair> neighborNodes, Map<NodePair, Double> sigma_0, Map<NodePair, Double> sigma_i, Graph<NodePair, CoefficientEdge> propagationGraph) {

            double phi = 0;

            for (NodePair neighbor : neighborNodes) {

                double neighborValue_0 = sigma_0.get(neighbor);
                double neighborValue_i = sigma_i.get(neighbor);
                double propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();

                phi += (neighborValue_0 + neighborValue_i) * propagationCoefficient;
            }

            return sigma_0.get(node) + sigma_i.get(node) + phi;
        }
    };

    public double evaluate(NodePair node, Set<NodePair> neighborNodes, Map<NodePair, Double> sigma_0, Map<NodePair, Double> sigma_i, Graph<NodePair, CoefficientEdge> propagationGraph) throws NoSuchMethodException {
        throw new NoSuchMethodException("Not implemented");
    }

}
