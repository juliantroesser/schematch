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
    private final boolean isIDNode;
    private final Node name;

    //Valentine:
    //Name
    //long_name = table_name, table_guid, column_name, column_guid
    //Database

    public Node(String value, NodeType nodeType, Datatype datatype, boolean isIDNode, Node name) {
        this.value = value;
        this.nodeType = nodeType;
        this.datatype = datatype;
        this.isIDNode = isIDNode;
        this.name = name; //Only Necessary for NodeID Nodes

    }


    public boolean isIDNode() {
        return isIDNode;
    }

    public Node getNameNode() {
        return name;
    }

    //Valentine: equal if: this.name = other.name & this.db = other.db

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Node other) {
            return this.value.equals(other.value);
        }
        return false;
    }

    //Valentine: hash(name)

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
