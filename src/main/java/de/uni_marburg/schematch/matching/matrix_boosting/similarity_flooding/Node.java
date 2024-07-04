package de.uni_marburg.schematch.matching.matrix_boosting.similarity_flooding;

import de.uni_marburg.schematch.data.metadata.Datatype;
import lombok.Getter;

import java.util.Objects;

public class Node {

    @Getter
    private final String value;
    @Getter
    private final NodeType nodeType;
    @Getter
    private final Datatype datatype;
    private final boolean isHelperNode;
    private final boolean isIDNode;

    public Node(String value, NodeType nodeType, Datatype datatype, boolean isHelperNode, boolean isIDNode) {
        this.value = value;
        this.nodeType = nodeType;
        this.datatype = datatype;
        this.isHelperNode = isHelperNode;
        this.isIDNode = isIDNode;
    }

    public boolean isHelperNode() {
        return isHelperNode;
    }

    public boolean isIDNode() {
        return isIDNode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Node other) {
            return this.value.equals(other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
