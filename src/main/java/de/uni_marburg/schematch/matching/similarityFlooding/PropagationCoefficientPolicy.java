package de.uni_marburg.schematch.matching.similarityFlooding;

import org.jgrapht.Graph;

public enum PropagationCoefficientPolicy {

    INVERSE_AVERAGE {
        @Override
        public double evaluate(LabelEdge label, Node node1, Node node2, Graph<Node, LabelEdge> graph1, Graph<Node, LabelEdge> graph2) throws NoSuchMethodException {

            long countOutgoingEdgesOfNode1 = graph1.outgoingEdgesOf(node1).stream().filter(outgoingEdgesInGraph1 -> outgoingEdgesInGraph1.getValue().equals(label.getValue())).count();
            long countIncomingEdgesOfNode1 = graph1.incomingEdgesOf(node1).stream().filter(incomingEdgesInGraph1 -> incomingEdgesInGraph1.getValue().equals(label.getValue())).count();

            long countOutgoingEdgesOfNode2 = graph2.outgoingEdgesOf(node2).stream().filter(outgoingEdgesInGraph2 -> outgoingEdgesInGraph2.getValue().equals(label.getValue())).count();
            long countIncomingEdgesOfNode2 = graph2.incomingEdgesOf(node2).stream().filter(outgoingEdgesInGraph2 -> outgoingEdgesInGraph2.getValue().equals(label.getValue())).count();

            return 2.0 / ((countOutgoingEdgesOfNode1 + countIncomingEdgesOfNode1) + (countOutgoingEdgesOfNode2 + countIncomingEdgesOfNode2));
        }
    },

    INVERSE_PRODUCT {
        @Override
        public double evaluate(LabelEdge label, Node node1, Node node2, Graph<Node, LabelEdge> graph1, Graph<Node, LabelEdge> graph2) throws NoSuchMethodException {

            long countOutgoingEdgesOfNode1 = graph1.outgoingEdgesOf(node1).stream().filter(outgoingEdgesInGraph1 -> outgoingEdgesInGraph1.getValue().equals(label.getValue())).count();
            long countIncomingEdgesOfNode1 = graph1.incomingEdgesOf(node1).stream().filter(incomingEdgesInGraph1 -> incomingEdgesInGraph1.getValue().equals(label.getValue())).count();

            long countOutgoingEdgesOfNode2 = graph2.outgoingEdgesOf(node2).stream().filter(outgoingEdgesInGraph2 -> outgoingEdgesInGraph2.getValue().equals(label.getValue())).count();
            long countIncomingEdgesOfNode2 = graph2.incomingEdgesOf(node2).stream().filter(outgoingEdgesInGraph2 -> outgoingEdgesInGraph2.getValue().equals(label.getValue())).count();

            return 1.0 / ((countOutgoingEdgesOfNode1 + countIncomingEdgesOfNode1) * (countOutgoingEdgesOfNode2 + countIncomingEdgesOfNode2));
        }
    },

    CONSTANT_ONE {
        @Override
        public double evaluate(LabelEdge label, Node node1, Node node2, Graph<Node, LabelEdge> graph1, Graph<Node, LabelEdge> graph2) throws NoSuchMethodException {
            return 1;
        }
    },

    CONSTANT_POINT_FIVE {
        @Override
        public double evaluate(LabelEdge label, Node node1, Node node2, Graph<Node, LabelEdge> graph1, Graph<Node, LabelEdge> graph2) throws NoSuchMethodException {
            return 0.5;
        }
    };

    public double evaluate(LabelEdge label, Node node1, Node node2, Graph<Node, LabelEdge> graph1, Graph<Node, LabelEdge> graph2) throws NoSuchMethodException {
        throw new NoSuchMethodException("Not Implemented");
    }

}
