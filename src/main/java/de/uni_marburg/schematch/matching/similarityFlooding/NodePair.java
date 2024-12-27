package de.uni_marburg.schematch.matching.similarityFlooding;

public class NodePair {

    private final Node node1;
    private final Node node2;

    public NodePair(Node node1, Node node2) {
        this.node1 = node1;
        this.node2 = node2;
    }

    public Node getFirstNode() {
        return node1;
    }

    public Node getSecondNode() {
        return node2;
    }

    //Equal if NodePair is a tuple of the same nodes, order does not matter
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NodePair other) {
            return (other.node1.equals(this.node1) && other.node2.equals(this.node2)) || (other.node1.equals(this.node2) && other.node2.equals(this.node1));
        }
        return false;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + (node1 == null ? 0 : node1.hashCode());
        result = prime * result + (node2 == null ? 0 : node2.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "(" + node1.toString() + ", " + node2.toString() + ")";
    }

}
