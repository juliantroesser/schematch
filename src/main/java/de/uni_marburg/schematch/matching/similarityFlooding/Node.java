package de.uni_marburg.schematch.matching.similarityFlooding;

import de.uni_marburg.schematch.data.Table;
import de.uni_marburg.schematch.data.metadata.Datatype;
import lombok.Getter;

public class Node {

    @Getter
    private final String value;
    @Getter
    private final NodeType nodeType;
    @Getter
    private final Datatype datatype;
    private final boolean isIDNode;
    private final Node name;
    private final Table table;


    public Node(String value, NodeType nodeType, Datatype datatype, boolean isIDNode, Node name, Table table) {
        this.value = value;
        this.nodeType = nodeType;
        this.datatype = datatype;
        this.isIDNode = isIDNode;
        this.name = name; //Only Necessary for NodeID Nodes, represents the Node that contains the name attribute
        this.table = table;
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

            if(this.table == null || other.table == null) {
                return this.value.equals(other.value);
            } else {
                return this.value.equals(other.value) && this.table.getName().equals(other.table.getName());
            }

        }
        return false;
    }

    @Override
    public int hashCode() {

        if(table == null) {
            return this.value.hashCode();
        } else {
            return 31 * value.hashCode() + table.getName().hashCode();
        }
    }

    @Override
    public String toString() {

        if(this.table == null) {
            return this.value;
        } else {
            return this.table.getName() + "__" + value;
        }
    }
}
