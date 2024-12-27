package de.uni_marburg.schematch.matching.similarityFlooding;
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

                double propagationCoefficient;
                try{
                    propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();
                } catch(Exception e) {
                    propagationCoefficient = 0.0;
                }

                phi += neighborValue * propagationCoefficient;
            }

            return sigma_i.get(node) + phi;
        }
    },

    BASIC_Lambda {
        @Override
        public double evaluate(NodePair node, Set<NodePair> neighborNodes, Map<NodePair, Double> sigma_0, Map<NodePair, Double> sigma_i, Graph<NodePair, CoefficientEdge> propagationGraph) {

            double phi = 0;

            for (NodePair neighbor : neighborNodes) {
                double neighborValue = sigma_i.get(neighbor);
                double propagationCoefficient;

                try{
                    propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();
                } catch(Exception e) {
                    propagationCoefficient = 0.0;
                }

                if(neighbor.getFirstNode().getNodeType().equals(NodeType.CONSTRAINT) && neighbor.getSecondNode().getNodeType().equals(NodeType.CONSTRAINT)) {
                    phi += LAMBDA_D * propagationCoefficient * neighborValue;
                } else {
                    phi += LAMBDA_E * propagationCoefficient * neighborValue;
                }
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
                double propagationCoefficient;

                try{
                    propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();
                } catch(Exception e) {
                    propagationCoefficient = 0.0;
                }

                phi += neighborValue * propagationCoefficient;
            }

            return sigma_0.get(node) + phi;
        }
    },

    FORMULA_A_Lambda {
        @Override
        public double evaluate(NodePair node, Set<NodePair> neighborNodes, Map<NodePair, Double> sigma_0, Map<NodePair, Double> sigma_i, Graph<NodePair, CoefficientEdge> propagationGraph) {

            double phi = 0;

            for (NodePair neighbor : neighborNodes) {
                double neighborValue = sigma_i.get(neighbor);
                double propagationCoefficient;

                try{
                    propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();
                } catch(Exception e) {
                    propagationCoefficient = 0.0;
                }

                if(neighbor.getFirstNode().getNodeType().equals(NodeType.CONSTRAINT) && neighbor.getSecondNode().getNodeType().equals(NodeType.CONSTRAINT)) {
                    phi += LAMBDA_D * propagationCoefficient * neighborValue;
                } else {
                    phi += LAMBDA_E * propagationCoefficient * neighborValue;
                }
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
                double propagationCoefficient;

                try{
                    propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();
                } catch(Exception e) {
                    propagationCoefficient = 0.0;
                }

                phi += (neighborValue_0 + neighborValue_i) * propagationCoefficient;
            }

            return phi;
        }
    },

    FORMULA_B_Lambda {
        @Override
        public double evaluate(NodePair node, Set<NodePair> neighborNodes, Map<NodePair, Double> sigma_0, Map<NodePair, Double> sigma_i, Graph<NodePair, CoefficientEdge> propagationGraph) {

            double phi = 0;

            for (NodePair neighbor : neighborNodes) {
                double neighborValue_0 = sigma_0.get(neighbor);
                double neighborValue_i = sigma_i.get(neighbor);
                double propagationCoefficient;

                try{
                    propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();
                } catch(Exception e) {
                    propagationCoefficient = 0.0;
                }

                if(neighbor.getFirstNode().getNodeType().equals(NodeType.CONSTRAINT) && neighbor.getSecondNode().getNodeType().equals(NodeType.CONSTRAINT)) {
                    phi += LAMBDA_D * (neighborValue_0 + neighborValue_i) * propagationCoefficient;
                } else {
                    phi += LAMBDA_E * (neighborValue_0 + neighborValue_i) * propagationCoefficient;
                }
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

                double propagationCoefficient;
                try {
                    propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();
                } catch (Exception e) {
                    propagationCoefficient = 0.0;
                }

                phi += (neighborValue_0 + neighborValue_i) * propagationCoefficient;
            }

            return sigma_0.get(node) + sigma_i.get(node) + phi;
        }
    },

    FORMULA_C_Lambda {

        @Override
        public double evaluate(NodePair node, Set<NodePair> neighborNodes, Map<NodePair, Double> sigma_0, Map<NodePair, Double> sigma_i, Graph<NodePair, CoefficientEdge> propagationGraph) {

            double phi = 0;

            for (NodePair neighbor : neighborNodes) {
                double neighborValue_0 = sigma_0.get(neighbor);
                double neighborValue_i = sigma_i.get(neighbor);
                double propagationCoefficient;

                try {
                    propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();
                } catch (Exception e) {
                    propagationCoefficient = 0.0;
                }

                if(neighbor.getFirstNode().getNodeType().equals(NodeType.CONSTRAINT) && neighbor.getSecondNode().getNodeType().equals(NodeType.CONSTRAINT)) {
                    phi += LAMBDA_D * (neighborValue_0 + neighborValue_i) * propagationCoefficient;
                } else {
                    phi += LAMBDA_E * (neighborValue_0 + neighborValue_i) * propagationCoefficient;
                }
            }

            return sigma_0.get(node) + sigma_i.get(node) + phi;
        }
    };

    protected static final double LAMBDA_D = 0.0;
    protected static final double LAMBDA_E = 1.0; //Keep at 1.0

    public double evaluate(NodePair node, Set<NodePair> neighborNodes, Map<NodePair, Double> sigma_0, Map<NodePair, Double> sigma_i, Graph<NodePair, CoefficientEdge> propagationGraph) throws NoSuchMethodException {
        throw new NoSuchMethodException("Not implemented");
    }

}
