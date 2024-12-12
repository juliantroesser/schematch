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
//                double propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();

                double propagationCoefficient;
                try{
                    propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();
                } catch(Exception e) {
                    propagationCoefficient = 0.0;
                }

                phi += neighborValue * propagationCoefficient; //Anstatt alles zu addieren gibt es phi_constraint und phi_else
            }

            return sigma_i.get(node) + phi;


//            double phi_constraint = 0;
//            double phi_else = 0;
//
//            for (NodePair neighbor : neighborNodes) {
//                double neighborValue = sigma_i.get(neighbor);
//                double propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();
//
//                if(neighbor.getFirstNode().getNodeType().equals(NodeType.CONSTRAINT) && neighbor.getSecondNode().getNodeType().equals(NodeType.CONSTRAINT)) {
//                    phi_constraint += propagationCoefficient * neighborValue;
//                } else {
//                    phi_else += propagationCoefficient * neighborValue;
//                }
//            }
//
//            return sigma_i.get(node) + 0.75 * phi_constraint + 0.25 * phi_else; //TODO: Was tun, falls keine/oder nur sehr wenig Dependency Information, weil dann phi_else nur mit 0.25 Gewichtet wird -> Nochmal gewichten, das heisst
            //TODO: Anzahl der jeweiligen Nachbartypen zählen und damit gewichten
            //TODO: Overall Score scheint besser zu werden!

        }
    },

    FORMULA_A {
        @Override
        public double evaluate(NodePair node, Set<NodePair> neighborNodes, Map<NodePair, Double> sigma_0, Map<NodePair, Double> sigma_i, Graph<NodePair, CoefficientEdge> propagationGraph) {

            double phi = 0;

            for (NodePair neighbor : neighborNodes) {
                double neighborValue = sigma_i.get(neighbor);
//                double propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();
                double propagationCoefficient;

                try{
                    propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();
                } catch(Exception e) {
                    propagationCoefficient = 0.0;
                }

                phi += neighborValue * propagationCoefficient;
            }

            return sigma_0.get(node) + phi;

//            double phi_constraint = 0;
//            double phi_else = 0;
//
//            for (NodePair neighbor : neighborNodes) {
//                double neighborValue = sigma_i.get(neighbor);
//                double propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();
//
//                if(neighbor.getFirstNode().getNodeType().equals(NodeType.CONSTRAINT) && neighbor.getSecondNode().getNodeType().equals(NodeType.CONSTRAINT)) {
//                    phi_constraint += propagationCoefficient * neighborValue;
//                } else {
//                    phi_else += propagationCoefficient * neighborValue;
//                }
//            }
//
//            return sigma_0.get(node) + 0.75 * phi_constraint + 0.25 * phi_else;

        }
    },

    FORMULA_B {
        @Override
        public double evaluate(NodePair node, Set<NodePair> neighborNodes, Map<NodePair, Double> sigma_0, Map<NodePair, Double> sigma_i, Graph<NodePair, CoefficientEdge> propagationGraph) {

            double phi = 0;

            for (NodePair neighbor : neighborNodes) {

                double neighborValue_0 = sigma_0.get(neighbor);
                double neighborValue_i = sigma_i.get(neighbor);
//                double propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();

                double propagationCoefficient;

                try{
                    propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();
                } catch(Exception e) {
                    propagationCoefficient = 0.0;
                }

                phi += (neighborValue_0 + neighborValue_i) * propagationCoefficient;
            }

            return phi;

//            double phi_constraint = 0;
//            double phi_else = 0;
//
//            for (NodePair neighbor : neighborNodes) {
//                double neighborValue_0 = sigma_0.get(neighbor);
//                double neighborValue_i = sigma_i.get(neighbor);
//                double propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();
//
//                if(neighbor.getFirstNode().getNodeType().equals(NodeType.CONSTRAINT) && neighbor.getSecondNode().getNodeType().equals(NodeType.CONSTRAINT)) {
//                    phi_constraint += (neighborValue_0 + neighborValue_i) * propagationCoefficient;
//                } else {
//                    phi_else += (neighborValue_0 + neighborValue_i) * propagationCoefficient;
//                }
//            }
//
//            return 0.75 * phi_constraint + 0.25 * phi_else;

        }
    },

    FORMULA_C {
        @Override
        public double evaluate(NodePair node, Set<NodePair> neighborNodes, Map<NodePair, Double> sigma_0, Map<NodePair, Double> sigma_i, Graph<NodePair, CoefficientEdge> propagationGraph) {

            double phi = 0;

            for (NodePair neighbor : neighborNodes) {

                double neighborValue_0 = sigma_0.get(neighbor);
                double neighborValue_i = sigma_i.get(neighbor);
//                double propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();

                double propagationCoefficient;
                try {
                    propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();
                } catch (Exception e) {
                    propagationCoefficient = 0.0;
                }

                phi += (neighborValue_0 + neighborValue_i) * propagationCoefficient;
            }

            return sigma_0.get(node) + sigma_i.get(node) + phi;

//            double phi_constraint = 0;
//            double phi_else = 0;
//
//            for (NodePair neighbor : neighborNodes) {
//                double neighborValue_0 = sigma_0.get(neighbor);
//                double neighborValue_i = sigma_i.get(neighbor);
//                double propagationCoefficient = propagationGraph.getEdge(neighbor, node).getCoefficient();
//
//                if(neighbor.getFirstNode().getNodeType().equals(NodeType.CONSTRAINT) && neighbor.getSecondNode().getNodeType().equals(NodeType.CONSTRAINT)) {
//                    phi_constraint += (neighborValue_0 + neighborValue_i) * propagationCoefficient;
//                } else {
//                    phi_else += (neighborValue_0 + neighborValue_i) * propagationCoefficient;
//                }
//            }
//
//            return sigma_0.get(node) + sigma_i.get(node) + 0.75 * phi_constraint + 0.25 * phi_else;
        }
    };

    public double evaluate(NodePair node, Set<NodePair> neighborNodes, Map<NodePair, Double> sigma_0, Map<NodePair, Double> sigma_i, Graph<NodePair, CoefficientEdge> propagationGraph) throws NoSuchMethodException {
        throw new NoSuchMethodException("Not implemented");
    }

}
