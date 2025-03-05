package de.uni_marburg.schematch.matching.similarityFlooding;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.data.Table;
import de.uni_marburg.schematch.data.metadata.Datatype;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NodeTest {

    @Test
    void isIDNode() {
        Column column = new Column("lastname", List.of("Johnson, Martinez, O'Connor"));
        Table table = new Table("table", List.of("lastname"), List.of(column), null, null);

        Node node1 = new Node("lastname", NodeType.COLUMN, Datatype.STRING, false, null, null);
        Node node2 = new Node("NodeID1", NodeType.COLUMN, Datatype.STRING, true, node1, table);

        Assertions.assertFalse(node1.isIDNode());
        Assertions.assertTrue(node2.isIDNode());
    }

    @Test
    void getNameNode() {
        Column column = new Column("lastname", List.of("Johnson, Martinez, O'Connor"));
        Table table = new Table("table", List.of("lastname"), List.of(column), null, null);

        Node node1 = new Node("lastname", NodeType.COLUMN, Datatype.STRING, false, null, null);
        Node node2 = new Node("NodeID1", NodeType.COLUMN, Datatype.STRING, true, node1, table);

        Assertions.assertFalse(node1.isIDNode());
        Assertions.assertTrue(node2.isIDNode());

        Assertions.assertEquals(node1, node2.getNameNode());
    }

    @Test
    void testEquals() {
        Column column1 = new Column("lastname", List.of("Johnson, Martinez, O'Connor"));
        Column column2 = new Column("surname", List.of("Patel, Kim, Garcia"));
        Table table1 = new Table("table1", List.of("lastname, surname"), List.of(column1, column2), null, null);
        Table table2 = new Table("table2", List.of("lastname, surname"), List.of(column1, column2), null, null);

        //Same Name, same table
        Node node1 = new Node("lastname", NodeType.COLUMN, Datatype.STRING, false, null, table1);
        Node node2 = new Node("lastname", NodeType.COLUMN, Datatype.STRING, true, node1, table1);
        Assertions.assertEquals(node1, node2);

        //Different Name, same table
        Node node3 = new Node("surname", NodeType.COLUMN, Datatype.STRING, false, null, table1);
        Assertions.assertNotEquals(node1, node3);
        Assertions.assertNotEquals(node2, node3);

        //Same Name, Different Table
        Node node4 = new Node("lastname", NodeType.COLUMN, Datatype.STRING, false, null, table2);
        Assertions.assertNotEquals(node1, node4);

        //Same Name, null value as Table
        Node node5 = new Node("lastname", NodeType.COLUMN, Datatype.STRING, false, null, null);
        Node node6 = new Node("lastname", NodeType.COLUMN, Datatype.STRING, true, node1, null);
        Assertions.assertEquals(node5, node6);

        Node node7 = new Node("surname", NodeType.COLUMN, Datatype.STRING, true, node1, null);
        Assertions.assertNotEquals(node5, node7);
        Assertions.assertNotEquals(node6, node7);
    }

    @Test
    void testHashCode() {

        //Case 1: Table is null
        Node node1 = new Node("lastname", NodeType.COLUMN, Datatype.STRING, false, null, null);
        Node node2 = new Node("lastname", NodeType.COLUMN, Datatype.STRING, true, null, null);
        Node node3 = new Node("surname", NodeType.COLUMN, Datatype.STRING, true, null, null);

        Assertions.assertEquals(node1.hashCode(), node1.hashCode());
        Assertions.assertEquals(node1.hashCode(), node2.hashCode());
        Assertions.assertNotEquals(node1.hashCode(), node3.hashCode());
        Assertions.assertNotEquals(node2.hashCode(), node3.hashCode());

        //Case 2: Table is not null
        Column column1 = new Column("lastname", List.of("Johnson, Martinez, O'Connor"));
        Column column2 = new Column("surname", List.of("Patel, Kim, Garcia"));
        Table table1 = new Table("table1", List.of("lastname, surname"), List.of(column1, column2), null, null);
        Node node4 = new Node("lastname", NodeType.COLUMN, Datatype.STRING, false, null, table1);
        Node node5 = new Node("lastname", NodeType.COLUMN, Datatype.STRING, true, null, table1);
        Node node6 = new Node("surname", NodeType.COLUMN, Datatype.STRING, false, null, table1);

        //Consistency
        Assertions.assertEquals(node4.hashCode(), node4.hashCode());
        Assertions.assertEquals(node4.hashCode(), node5.hashCode());
        Assertions.assertNotEquals(node4.hashCode(), node6.hashCode());
        Assertions.assertNotEquals(node5.hashCode(), node6.hashCode());
    }

    @Test
    void testToString() {
        Column column1 = new Column("lastname", List.of("Johnson, Martinez, O'Connor"));
        Column column2 = new Column("surname", List.of("Patel, Kim, Garcia"));
        Table table1 = new Table("table1", List.of("lastname, surname"), List.of(column1, column2), null, null);
        Node node1 = new Node("lastname", NodeType.COLUMN, Datatype.STRING, false, null, table1);

        Assertions.assertEquals("table1__lastname", node1.toString());

        Node node2 = new Node("surname", NodeType.COLUMN, Datatype.STRING, true, null, null);

        Assertions.assertEquals("surname", node2.toString());
    }
}