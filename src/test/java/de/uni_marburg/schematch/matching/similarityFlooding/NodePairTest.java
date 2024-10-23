package de.uni_marburg.schematch.matching.similarityFlooding;

import de.uni_marburg.schematch.data.metadata.Datatype;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NodePairTest {

    @Test
    void getFirstNode() {
         Node node1 = new Node("lastname", NodeType.COLUMN, Datatype.STRING, false, null, null);
         Node node2 = new Node("surname", NodeType.COLUMN, Datatype.STRING, false, null, null);

         NodePair pair = new NodePair(node1, node2);

         assertEquals(node1, pair.getFirstNode());
    }

    @Test
    void getSecondNode() {
        Node node1 = new Node("lastname", NodeType.COLUMN, Datatype.STRING, false, null, null);
        Node node2 = new Node("surname", NodeType.COLUMN, Datatype.STRING, false, null, null);

        NodePair pair = new NodePair(node1, node2);

        assertEquals(node2, pair.getSecondNode());

    }

    @Test
    void testEquals() {
        Node node1 = new Node("lastname", NodeType.COLUMN, Datatype.STRING, false, null, null);
        Node node2 = new Node("surname", NodeType.COLUMN, Datatype.STRING, false, null, null);
        NodePair pair1 = new NodePair(node1, node2);

        Node node3 = new Node("lastname", NodeType.COLUMN, Datatype.STRING, false, null, null);
        Node node4 = new Node("surname", NodeType.COLUMN, Datatype.STRING, false, null, null);
        NodePair pair2 = new NodePair(node3, node4);

        Node node5 = new Node("age_month", NodeType.COLUMN, Datatype.INTEGER, false, null, null);
        Node node6 = new Node("age_years", NodeType.COLUMN, Datatype.INTEGER, false, null, null);
        NodePair pair3 = new NodePair(node5, node6);

        Assertions.assertEquals(pair1, pair2);

        Assertions.assertNotEquals(pair1, pair3);
        Assertions.assertNotEquals(pair2, pair3);
    }

    @Test
    void testHashCode() {
        Node node1 = new Node("lastname", NodeType.COLUMN, Datatype.STRING, false, null, null);
        Node node2 = new Node("surname", NodeType.COLUMN, Datatype.STRING, false, null, null);
        NodePair pair1 = new NodePair(node1, node2);

        Node node3 = new Node("lastname", NodeType.COLUMN, Datatype.STRING, false, null, null);
        Node node4 = new Node("surname", NodeType.COLUMN, Datatype.STRING, false, null, null);
        NodePair pair2 = new NodePair(node3, node4);

        Node node5 = new Node("age_month", NodeType.COLUMN, Datatype.INTEGER, false, null, null);
        Node node6 = new Node("age_years", NodeType.COLUMN, Datatype.INTEGER, false, null, null);
        NodePair pair3 = new NodePair(node5, node6);

        //Consistency
        Assertions.assertEquals(pair1.hashCode(), pair1.hashCode());

        //Equal Objects
        Assertions.assertEquals(pair1.hashCode(), pair2.hashCode());

        //Unequal Objects
        Assertions.assertNotEquals(pair1.hashCode(), pair3.hashCode());
        Assertions.assertNotEquals(pair2.hashCode(), pair3.hashCode());
    }

    @Test
    void testToString() {
        Node node1 = new Node("lastname", NodeType.COLUMN, Datatype.STRING, false, null, null);
        Node node2 = new Node("surname", NodeType.COLUMN, Datatype.STRING, false, null, null);
        NodePair pair = new NodePair(node1, node2);

        Assertions.assertEquals(("(lastname, surname)"), pair.toString());
    }
}