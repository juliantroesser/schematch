package de.uni_marburg.schematch.matching.similarityFlooding;

import de.uni_marburg.schematch.TestUtils;
import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.data.Database;
import de.uni_marburg.schematch.data.Scenario;
import de.uni_marburg.schematch.data.Table;
import de.uni_marburg.schematch.data.metadata.DatabaseMetadata;
import de.uni_marburg.schematch.data.metadata.Datatype;
import de.uni_marburg.schematch.data.metadata.dependency.FunctionalDependency;
import de.uni_marburg.schematch.data.metadata.dependency.InclusionDependency;
import de.uni_marburg.schematch.data.metadata.dependency.UniqueColumnCombination;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.List;

class GraphTransformationTest {

    SimilarityFlooding similarityFlooding = new SimilarityFlooding();

    //Basisfall: 16 Knoten, 17 Kanten

    @Test
    public void transformIntoGraphRepresentationSchema() {
        TestUtils.TestData testData = TestUtils.getTestData();
        Scenario scenario = new Scenario(testData.getScenarios().get("test1").getPath());
        Database sourceDb = scenario.getSourceDatabase();
        Table sourceTable = sourceDb.getTables().get(0);

        Graph<Node, LabelEdge> graphRepresentation = similarityFlooding.transformIntoGraphRepresentationSchema(sourceDb, false, false, false, false);

        //All Type Nodes
        Node schema = new Node("Schema", NodeType.DATABASE, null, false, null, null, null);
        Node column = new Node("Column", NodeType.COLUMN, null, false, null, null, null);
        Node table = new Node("Table", NodeType.TABLE, null, false, null, null, null);
        Node columnType = new Node("ColumnType", NodeType.COLUMN_TYPE, null, false, null, null, null);

        //All Name Nodes
        Node source = new Node("source", NodeType.DATABASE, null, false, null, null, null);
        Node authors = new Node("authors", NodeType.TABLE, null, false, null, null, null);
        Node aid = new Node("aid", NodeType.COLUMN, Datatype.INTEGER, false, null, sourceTable, null);
        Node name = new Node("name", NodeType.COLUMN, Datatype.STRING, false, null, sourceTable, null);
        Node integer = new Node("INTEGER", NodeType.COLUMN_TYPE, Datatype.INTEGER, false, null, null, null);
        Node string = new Node("STRING", NodeType.COLUMN_TYPE, Datatype.STRING, false, null, null, null);

        //All Database Nodes
        Node NodeID1 = new Node("NodeID1", NodeType.DATABASE, null, true, source, null, null);

        //All Table Nodes
        Node NodeID2 = new Node("NodeID2", NodeType.TABLE, null, true, authors, null, null);

        //All Column Nodes
        Node NodeID3 = new Node("NodeID3", NodeType.COLUMN, Datatype.INTEGER, true, aid, sourceTable, null);
        Node NodeID5 = new Node("NodeID5", NodeType.COLUMN, Datatype.STRING, true, name, sourceTable, null);

        //All ColumnType Nodes
        Node NodeID4 = new Node("NodeID4", NodeType.COLUMN_TYPE, Datatype.INTEGER, true, integer, null, null);
        Node NodeID6 = new Node("NodeID6", NodeType.COLUMN_TYPE, Datatype.STRING, true, string, null, null);

        //Check vertices
        Assertions.assertEquals(16, graphRepresentation.vertexSet().size());

        Assertions.assertTrue(graphRepresentation.containsVertex(schema));
        Assertions.assertTrue(graphRepresentation.containsVertex(column));
        Assertions.assertTrue(graphRepresentation.containsVertex(table));
        Assertions.assertTrue(graphRepresentation.containsVertex(columnType));
        Assertions.assertTrue(graphRepresentation.containsVertex(source));
        Assertions.assertTrue(graphRepresentation.containsVertex(authors));
        Assertions.assertTrue(graphRepresentation.containsVertex(aid));
        Assertions.assertTrue(graphRepresentation.containsVertex(name));
        Assertions.assertTrue(graphRepresentation.containsVertex(integer));
        Assertions.assertTrue(graphRepresentation.containsVertex(string));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID1));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID2));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID3));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID4));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID5));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID6));

        //Check Edges
        Assertions.assertEquals(17, graphRepresentation.edgeSet().size());

        //All Edges from the Database Node / root-Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, source));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID1, source));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, schema));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID1, schema));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, NodeID2));
        Assertions.assertEquals(new LabelEdge("table"), graphRepresentation.getEdge(NodeID1, NodeID2));

        //All Edges from the Table Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, authors));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID2, authors));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, table));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID2, table));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, NodeID3));
        Assertions.assertEquals(new LabelEdge("column"), graphRepresentation.getEdge(NodeID2, NodeID3));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, NodeID5));
        Assertions.assertEquals(new LabelEdge("column"), graphRepresentation.getEdge(NodeID2, NodeID5));

        //All Edges from the first column (aid) Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, aid));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID3, aid));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, column));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID3, column));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, NodeID4));
        Assertions.assertEquals(new LabelEdge("datatype"), graphRepresentation.getEdge(NodeID3, NodeID4));

        //All Edges from the second column (name) Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, name));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID5, name));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, column));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID5, column));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, NodeID6));
        Assertions.assertEquals(new LabelEdge("datatype"), graphRepresentation.getEdge(NodeID5, NodeID6));

        //All Edges from the Integer Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID4, integer));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID4, integer));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID4, columnType));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID4, columnType));

        //All Edges from the String Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID6, string));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID6, string));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID6, columnType));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID6, columnType));
    }

    @Test
    public void transformIntoGraphRepresentationTable() {
        TestUtils.TestData testData = TestUtils.getTestData();
        Scenario scenario = new Scenario(testData.getScenarios().get("test1").getPath());
        Database sourceDb = scenario.getSourceDatabase();
        Table sourceTable = sourceDb.getTables().get(0);

        Graph<Node, LabelEdge> graphRepresentation = similarityFlooding.transformIntoGraphRepresentationTable(sourceDb, sourceTable, false, false, false, false, false, false);

        //All Type Nodes
        Node column = new Node("Column", NodeType.COLUMN, null, false, null, null, null);
        Node table = new Node("Table", NodeType.TABLE, null, false, null, null, null);
        Node columnType = new Node("ColumnType", NodeType.COLUMN_TYPE, null, false, null, null, null);

        //All Name Nodes
        Node authors = new Node("authors", NodeType.TABLE, null, false, null, null, null);
        Node aid = new Node("aid", NodeType.COLUMN, Datatype.INTEGER, false, null, sourceTable, null);
        Node name = new Node("name", NodeType.COLUMN, Datatype.STRING, false, null, sourceTable, null);
        Node integer = new Node("INTEGER", NodeType.COLUMN_TYPE, Datatype.INTEGER, false, null, null, null);
        Node string = new Node("STRING", NodeType.COLUMN_TYPE, Datatype.STRING, false, null, null, null);

        //All Table Nodes
        Node NodeID1 = new Node("NodeID1", NodeType.TABLE, null, true, authors, null, null);

        //All Column Nodes
        Node NodeID2 = new Node("NodeID2", NodeType.COLUMN, Datatype.INTEGER, true, aid, sourceTable, null);
        Node NodeID4 = new Node("NodeID4", NodeType.COLUMN, Datatype.STRING, true, name, sourceTable, null);

        //All ColumnType Nodes
        Node NodeID3 = new Node("NodeID3", NodeType.COLUMN_TYPE, Datatype.INTEGER, true, integer, null, null);
        Node NodeID5 = new Node("NodeID5", NodeType.COLUMN_TYPE, Datatype.STRING, true, string, null, null);

        //Check vertices
        Assertions.assertEquals(13, graphRepresentation.vertexSet().size());

        Assertions.assertTrue(graphRepresentation.containsVertex(column));
        Assertions.assertTrue(graphRepresentation.containsVertex(table));
        Assertions.assertTrue(graphRepresentation.containsVertex(columnType));
        Assertions.assertTrue(graphRepresentation.containsVertex(authors));
        Assertions.assertTrue(graphRepresentation.containsVertex(aid));
        Assertions.assertTrue(graphRepresentation.containsVertex(name));
        Assertions.assertTrue(graphRepresentation.containsVertex(integer));
        Assertions.assertTrue(graphRepresentation.containsVertex(string));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID2));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID3));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID4));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID5));

        //Check Edges
        Assertions.assertEquals(14, graphRepresentation.edgeSet().size());

        //All Edges from the Database Node / root-Node

        //All Edges from the Table Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, authors));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID1, authors));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, table));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID1, table));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, NodeID2));
        Assertions.assertEquals(new LabelEdge("column"), graphRepresentation.getEdge(NodeID1, NodeID2));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, NodeID4));
        Assertions.assertEquals(new LabelEdge("column"), graphRepresentation.getEdge(NodeID1, NodeID4));

        //All Edges from the first column (aid) Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, aid));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID2, aid));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, column));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID2, column));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, NodeID3));
        Assertions.assertEquals(new LabelEdge("datatype"), graphRepresentation.getEdge(NodeID2, NodeID3));

        //All Edges from the second column (name) Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID4, name));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID4, name));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID4, column));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID4, column));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID4, NodeID5));
        Assertions.assertEquals(new LabelEdge("datatype"), graphRepresentation.getEdge(NodeID4, NodeID5));

        //All Edges from the Integer Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, integer));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID3, integer));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, columnType));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID3, columnType));

        //All Edges from the String Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, string));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID5, string));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, columnType));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID5, columnType));
    }

    @Test
    public void transformIntoGraphRepresentationWithFDV1() {
        TestUtils.TestData testData = TestUtils.getTestData();
        Scenario scenario = new Scenario(testData.getScenarios().get("test1").getPath());

        Database sourceDb = scenario.getSourceDatabase();
        Table sourceTable = sourceDb.getTables().get(0);
        DatabaseMetadata metadata = new DatabaseMetadata();

        //Add test Dependency to Database
        Column columnAid = sourceDb.getTableByName("authors").getColumn(0);
        Column columnName = sourceDb.getTableByName("authors").getColumn(1);

        //Test Dependency aid --> name
        FunctionalDependency fd = new FunctionalDependency(List.of(columnAid), columnName);
        metadata.setFds(List.of(fd));

        sourceDb.setMetadata(metadata);

        Graph<Node, LabelEdge> graphRepresentation = similarityFlooding.transformIntoGraphRepresentationSchema(sourceDb, true, false, false, false);

        //All Type Nodes
        Node schema = new Node("Schema", NodeType.DATABASE, null, false, null, null, null);
        Node column = new Node("Column", NodeType.COLUMN, null, false, null, null, null);
        Node table = new Node("Table", NodeType.TABLE, null, false, null, null, null);
        Node columnType = new Node("ColumnType", NodeType.COLUMN_TYPE, null, false, null, null, null);

        //All Name Nodes
        Node source = new Node("source", NodeType.DATABASE, null, false, null, null, null);
        Node authors = new Node("authors", NodeType.TABLE, null, false, null, null, null);
        Node aid = new Node("aid", NodeType.COLUMN, Datatype.INTEGER, false, null, sourceTable, null);
        Node name = new Node("name", NodeType.COLUMN, Datatype.STRING, false, null, sourceTable, null);
        Node integer = new Node("INTEGER", NodeType.COLUMN_TYPE, Datatype.INTEGER, false, null, null, null);
        Node string = new Node("STRING", NodeType.COLUMN_TYPE, Datatype.STRING, false, null, null, null);

        //All Database Nodes
        Node NodeID1 = new Node("NodeID1", NodeType.DATABASE, null, true, source, null, null);

        //All Table Nodes
        Node NodeID2 = new Node("NodeID2", NodeType.TABLE, null, true, authors, null, null);

        //All Column Nodes
        Node NodeID3 = new Node("NodeID3", NodeType.COLUMN, Datatype.INTEGER, true, aid, sourceTable, null);
        Node NodeID5 = new Node("NodeID5", NodeType.COLUMN, Datatype.STRING, true, name, sourceTable, null);

        //All ColumnType Nodes
        Node NodeID4 = new Node("NodeID4", NodeType.COLUMN_TYPE, Datatype.INTEGER, true, integer, null, null);
        Node NodeID6 = new Node("NodeID6", NodeType.COLUMN_TYPE, Datatype.STRING, true, string, null, null);

        //Check vertices
        Assertions.assertEquals(16, graphRepresentation.vertexSet().size());

        Assertions.assertTrue(graphRepresentation.containsVertex(schema));
        Assertions.assertTrue(graphRepresentation.containsVertex(column));
        Assertions.assertTrue(graphRepresentation.containsVertex(table));
        Assertions.assertTrue(graphRepresentation.containsVertex(columnType));
        Assertions.assertTrue(graphRepresentation.containsVertex(source));
        Assertions.assertTrue(graphRepresentation.containsVertex(authors));
        Assertions.assertTrue(graphRepresentation.containsVertex(aid));
        Assertions.assertTrue(graphRepresentation.containsVertex(name));
        Assertions.assertTrue(graphRepresentation.containsVertex(integer));
        Assertions.assertTrue(graphRepresentation.containsVertex(string));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID1));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID2));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID3));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID4));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID5));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID6));

        //Check Edges
        Assertions.assertEquals(18, graphRepresentation.edgeSet().size());

        //All Edges from the Database Node / root-Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, source));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID1, source));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, schema));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID1, schema));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, NodeID2));
        Assertions.assertEquals(new LabelEdge("table"), graphRepresentation.getEdge(NodeID1, NodeID2));

        //All Edges from the Table Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, authors));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID2, authors));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, table));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID2, table));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, NodeID3));
        Assertions.assertEquals(new LabelEdge("column"), graphRepresentation.getEdge(NodeID2, NodeID3));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, NodeID5));
        Assertions.assertEquals(new LabelEdge("column"), graphRepresentation.getEdge(NodeID2, NodeID5));

        //All Edges from the first column (aid) Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, aid));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID3, aid));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, column));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID3, column));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, NodeID4));
        Assertions.assertEquals(new LabelEdge("datatype"), graphRepresentation.getEdge(NodeID3, NodeID4));

        //All Edges from the second column (name) Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, name));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID5, name));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, column));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID5, column));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, NodeID6));
        Assertions.assertEquals(new LabelEdge("datatype"), graphRepresentation.getEdge(NodeID5, NodeID6));

        //All Edges from the Integer Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID4, integer));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID4, integer));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID4, columnType));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID4, columnType));

        //All Edges from the String Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID6, string));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID6, string));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID6, columnType));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID6, columnType));

        //All Test for Extra Dependency Edges
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, NodeID5));
        Assertions.assertEquals(new LabelEdge("determines"), graphRepresentation.getEdge(NodeID3, NodeID5));
    }

    @Test
    public void transformIntoGraphRepresentationWithFDV2() {
        TestUtils.TestData testData = TestUtils.getTestData();
        Scenario scenario = new Scenario(testData.getScenarios().get("test1").getPath());

        Database sourceDb = scenario.getSourceDatabase();
        Table sourceTable = sourceDb.getTables().get(0);
        DatabaseMetadata metadata = new DatabaseMetadata();

        //Add test Dependency to Database
        Column columnAid = sourceDb.getTableByName("authors").getColumn(0);
        Column columnName = sourceDb.getTableByName("authors").getColumn(1);

        //Test Dependency aid --> name
        FunctionalDependency fd = new FunctionalDependency(List.of(columnAid), columnName);
        metadata.setFds(List.of(fd));

        sourceDb.setMetadata(metadata);

        Graph<Node, LabelEdge> graphRepresentation = similarityFlooding.transformIntoGraphRepresentationSchema(sourceDb, false, true, false, false);

        //All Type Nodes
        Node schema = new Node("Schema", NodeType.DATABASE, null, false, null, null, null);
        Node column = new Node("Column", NodeType.COLUMN, null, false, null, null, null);
        Node table = new Node("Table", NodeType.TABLE, null, false, null, null, null);
        Node columnType = new Node("ColumnType", NodeType.COLUMN_TYPE, null, false, null, null, null);
        Node constraint = new Node("Constraint", NodeType.CONSTRAINT, null, false, null, null, null);

        //All Name Nodes
        Node source = new Node("source", NodeType.DATABASE, null, false, null, null, null);
        Node authors = new Node("authors", NodeType.TABLE, null, false, null, null, null);
        Node aid = new Node("aid", NodeType.COLUMN, Datatype.INTEGER, false, null, sourceTable, null);
        Node name = new Node("name", NodeType.COLUMN, Datatype.STRING, false, null, sourceTable, null);
        Node integer = new Node("INTEGER", NodeType.COLUMN_TYPE, Datatype.INTEGER, false, null, null, null);
        Node string = new Node("STRING", NodeType.COLUMN_TYPE, Datatype.STRING, false, null, null, null);

        //All Database Nodes
        Node NodeID1 = new Node("NodeID1", NodeType.DATABASE, null, true, source, null, null);

        //All Table Nodes
        Node NodeID2 = new Node("NodeID2", NodeType.TABLE, null, true, authors, null, null);

        //All Column Nodes
        Node NodeID3 = new Node("NodeID3", NodeType.COLUMN, Datatype.INTEGER, true, aid, sourceTable, null);
        Node NodeID5 = new Node("NodeID5", NodeType.COLUMN, Datatype.STRING, true, name, sourceTable, null);

        //All ColumnType Nodes
        Node NodeID4 = new Node("NodeID4", NodeType.COLUMN_TYPE, Datatype.INTEGER, true, integer, null, null);
        Node NodeID6 = new Node("NodeID6", NodeType.COLUMN_TYPE, Datatype.STRING, true, string, null, null);

        //All FD Nodes
        Node fd1 = new Node("FD1", NodeType.CONSTRAINT, null, false, null, null, null);

        //Check vertices
        Assertions.assertEquals(18, graphRepresentation.vertexSet().size());

        Assertions.assertTrue(graphRepresentation.containsVertex(schema));
        Assertions.assertTrue(graphRepresentation.containsVertex(column));
        Assertions.assertTrue(graphRepresentation.containsVertex(table));
        Assertions.assertTrue(graphRepresentation.containsVertex(columnType));
        Assertions.assertTrue(graphRepresentation.containsVertex(constraint));
        Assertions.assertTrue(graphRepresentation.containsVertex(source));
        Assertions.assertTrue(graphRepresentation.containsVertex(authors));
        Assertions.assertTrue(graphRepresentation.containsVertex(aid));
        Assertions.assertTrue(graphRepresentation.containsVertex(name));
        Assertions.assertTrue(graphRepresentation.containsVertex(integer));
        Assertions.assertTrue(graphRepresentation.containsVertex(string));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID1));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID2));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID3));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID4));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID5));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID6));
        Assertions.assertTrue(graphRepresentation.containsVertex(fd1));

        //Check Edges
        Assertions.assertEquals(20, graphRepresentation.edgeSet().size());

        //All Edges from the Database Node / root-Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, source));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID1, source));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, schema));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID1, schema));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, NodeID2));
        Assertions.assertEquals(new LabelEdge("table"), graphRepresentation.getEdge(NodeID1, NodeID2));

        //All Edges from the Table Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, authors));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID2, authors));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, table));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID2, table));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, NodeID3));
        Assertions.assertEquals(new LabelEdge("column"), graphRepresentation.getEdge(NodeID2, NodeID3));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, NodeID5));
        Assertions.assertEquals(new LabelEdge("column"), graphRepresentation.getEdge(NodeID2, NodeID5));

        //All Edges from the first column (aid) Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, aid));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID3, aid));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, column));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID3, column));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, NodeID4));
        Assertions.assertEquals(new LabelEdge("datatype"), graphRepresentation.getEdge(NodeID3, NodeID4));

        //All Edges from the second column (name) Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, name));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID5, name));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, column));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID5, column));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, NodeID6));
        Assertions.assertEquals(new LabelEdge("datatype"), graphRepresentation.getEdge(NodeID5, NodeID6));

        //All Edges from the Integer Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID4, integer));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID4, integer));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID4, columnType));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID4, columnType));

        //All Edges from the String Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID6, string));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID6, string));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID6, columnType));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID6, columnType));

        //All Test for Extra Dependency Edges
        Assertions.assertTrue(graphRepresentation.containsEdge(fd1, NodeID3));
        Assertions.assertEquals(new LabelEdge("determinant"), graphRepresentation.getEdge(fd1, NodeID3));
        Assertions.assertTrue(graphRepresentation.containsEdge(fd1, NodeID5));
        Assertions.assertEquals(new LabelEdge("dependant"), graphRepresentation.getEdge(fd1, NodeID5));
        Assertions.assertTrue(graphRepresentation.containsEdge(fd1, constraint));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(fd1, constraint));
    }

    @Test
    public void transformIntoGraphRepresentationWithUCCV1() {
        TestUtils.TestData testData = TestUtils.getTestData();
        Scenario scenario = new Scenario(testData.getScenarios().get("test1").getPath());

        Database sourceDb = scenario.getSourceDatabase();
        Table sourceTable = sourceDb.getTables().get(0);
        DatabaseMetadata metadata = new DatabaseMetadata();

        //Add test Dependency to Database
        Column columnAid = sourceDb.getTableByName("authors").getColumn(0);
        Column columnName = sourceDb.getTableByName("authors").getColumn(1);

        //Test UniqueColumnCombination (aid, name)
        UniqueColumnCombination uniqueColumnCombination = new UniqueColumnCombination(List.of(columnAid, columnName));
        metadata.setUccs(List.of(uniqueColumnCombination));

        sourceDb.setMetadata(metadata);

        Graph<Node, LabelEdge> graphRepresentation = similarityFlooding.transformIntoGraphRepresentationSchema(sourceDb, false, false, true, false);

        //All Type Nodes
        Node schema = new Node("Schema", NodeType.DATABASE, null, false, null, null, null);
        Node column = new Node("Column", NodeType.COLUMN, null, false, null, null, null);
        Node table = new Node("Table", NodeType.TABLE, null, false, null, null, null);
        Node columnType = new Node("ColumnType", NodeType.COLUMN_TYPE, null, false, null, null, null);

        //All Name Nodes
        Node source = new Node("source", NodeType.DATABASE, null, false, null, null, null);
        Node authors = new Node("authors", NodeType.TABLE, null, false, null, null, null);
        Node aid = new Node("aid", NodeType.COLUMN, Datatype.INTEGER, false, null, sourceTable, null);
        Node name = new Node("name", NodeType.COLUMN, Datatype.STRING, false, null, sourceTable, null);
        Node integer = new Node("INTEGER", NodeType.COLUMN_TYPE, Datatype.INTEGER, false, null, null, null);
        Node string = new Node("STRING", NodeType.COLUMN_TYPE, Datatype.STRING, false, null, null, null);

        //All Database Nodes
        Node NodeID1 = new Node("NodeID1", NodeType.DATABASE, null, true, source, null, null);

        //All Table Nodes
        Node NodeID2 = new Node("NodeID2", NodeType.TABLE, null, true, authors, null, null);

        //All Column Nodes
        Node NodeID3 = new Node("NodeID3", NodeType.COLUMN, Datatype.INTEGER, true, aid, sourceTable, null);
        Node NodeID5 = new Node("NodeID5", NodeType.COLUMN, Datatype.STRING, true, name, sourceTable, null);

        //All ColumnType Nodes
        Node NodeID4 = new Node("NodeID4", NodeType.COLUMN_TYPE, Datatype.INTEGER, true, integer, null, null);
        Node NodeID6 = new Node("NodeID6", NodeType.COLUMN_TYPE, Datatype.STRING, true, string, null, null);

        //UCC Node
        Node ucc = new Node("UCC#2", NodeType.CONSTRAINT, null, false, null, null, null);

        //Check vertices
        Assertions.assertEquals(17, graphRepresentation.vertexSet().size());

        Assertions.assertTrue(graphRepresentation.containsVertex(schema));
        Assertions.assertTrue(graphRepresentation.containsVertex(column));
        Assertions.assertTrue(graphRepresentation.containsVertex(table));
        Assertions.assertTrue(graphRepresentation.containsVertex(columnType));
        Assertions.assertTrue(graphRepresentation.containsVertex(source));
        Assertions.assertTrue(graphRepresentation.containsVertex(authors));
        Assertions.assertTrue(graphRepresentation.containsVertex(aid));
        Assertions.assertTrue(graphRepresentation.containsVertex(name));
        Assertions.assertTrue(graphRepresentation.containsVertex(integer));
        Assertions.assertTrue(graphRepresentation.containsVertex(string));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID1));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID2));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID3));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID4));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID5));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID6));
        Assertions.assertTrue(graphRepresentation.containsVertex(ucc));

        //Check Edges
        Assertions.assertEquals(19, graphRepresentation.edgeSet().size());

        //All Edges from the Database Node / root-Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, source));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID1, source));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, schema));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID1, schema));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, NodeID2));
        Assertions.assertEquals(new LabelEdge("table"), graphRepresentation.getEdge(NodeID1, NodeID2));

        //All Edges from the Table Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, authors));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID2, authors));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, table));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID2, table));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, NodeID3));
        Assertions.assertEquals(new LabelEdge("column"), graphRepresentation.getEdge(NodeID2, NodeID3));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, NodeID5));
        Assertions.assertEquals(new LabelEdge("column"), graphRepresentation.getEdge(NodeID2, NodeID5));

        //All Edges from the first column (aid) Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, aid));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID3, aid));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, column));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID3, column));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, NodeID4));
        Assertions.assertEquals(new LabelEdge("datatype"), graphRepresentation.getEdge(NodeID3, NodeID4));

        //All Edges from the second column (name) Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, name));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID5, name));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, column));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID5, column));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, NodeID6));
        Assertions.assertEquals(new LabelEdge("datatype"), graphRepresentation.getEdge(NodeID5, NodeID6));

        //All Edges from the Integer Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID4, integer));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID4, integer));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID4, columnType));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID4, columnType));

        //All Edges from the String Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID6, string));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID6, string));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID6, columnType));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID6, columnType));

        //All Test for Extra UCC Edges
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, ucc));
        Assertions.assertEquals(new LabelEdge("ucc"), graphRepresentation.getEdge(NodeID3, ucc));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, ucc));
        Assertions.assertEquals(new LabelEdge("ucc"), graphRepresentation.getEdge(NodeID5, ucc));
    }

    @Test
    public void transformIntoGraphRepresentationWithUCCV2() {
        TestUtils.TestData testData = TestUtils.getTestData();
        Scenario scenario = new Scenario(testData.getScenarios().get("test1").getPath());

        Database sourceDb = scenario.getSourceDatabase();
        Table sourceTable = sourceDb.getTables().get(0);
        DatabaseMetadata metadata = new DatabaseMetadata();

        //Add test Dependency to Database
        Column columnAid = sourceDb.getTableByName("authors").getColumn(0);
        Column columnName = sourceDb.getTableByName("authors").getColumn(1);

        //Test UniqueColumnCombination (aid, name)
        UniqueColumnCombination uniqueColumnCombination1 = new UniqueColumnCombination(List.of(columnAid));
        UniqueColumnCombination uniqueColumnCombination2 = new UniqueColumnCombination(List.of(columnName));

        metadata.setUccs(List.of(uniqueColumnCombination1, uniqueColumnCombination2));

        sourceDb.setMetadata(metadata);

        Graph<Node, LabelEdge> graphRepresentation = similarityFlooding.transformIntoGraphRepresentationSchema(sourceDb, false, false, false, true);

        //All Type Nodes
        Node schema = new Node("Schema", NodeType.DATABASE, null, false, null, null, null);
        Node column = new Node("Column", NodeType.COLUMN, null, false, null, null, null);
        Node table = new Node("Table", NodeType.TABLE, null, false, null, null, null);
        Node columnType = new Node("ColumnType", NodeType.COLUMN_TYPE, null, false, null, null, null);
        Node constraint = new Node("Constraint", NodeType.CONSTRAINT, null, false, null, null, null);

        //All Name Nodes
        Node source = new Node("source", NodeType.DATABASE, null, false, null, null, null);
        Node authors = new Node("authors", NodeType.TABLE, null, false, null, null, null);
        Node aid = new Node("aid", NodeType.COLUMN, Datatype.INTEGER, false, null, sourceTable, null);
        Node name = new Node("name", NodeType.COLUMN, Datatype.STRING, false, null, sourceTable, null);
        Node integer = new Node("INTEGER", NodeType.COLUMN_TYPE, Datatype.INTEGER, false, null, null, null);
        Node string = new Node("STRING", NodeType.COLUMN_TYPE, Datatype.STRING, false, null, null, null);

        //All Database Nodes
        Node NodeID1 = new Node("NodeID1", NodeType.DATABASE, null, true, source, null, null);

        //All Table Nodes
        Node NodeID2 = new Node("NodeID2", NodeType.TABLE, null, true, authors, null, null);

        //All Column Nodes
        Node NodeID3 = new Node("NodeID3", NodeType.COLUMN, Datatype.INTEGER, true, aid, sourceTable, null);
        Node NodeID5 = new Node("NodeID5", NodeType.COLUMN, Datatype.STRING, true, name, sourceTable, null);

        //All ColumnType Nodes
        Node NodeID4 = new Node("NodeID4", NodeType.COLUMN_TYPE, Datatype.INTEGER, true, integer, null, null);
        Node NodeID6 = new Node("NodeID6", NodeType.COLUMN_TYPE, Datatype.STRING, true, string, null, null);

        //UCC Nodes
        Node ucc1 = new Node("UCC1", NodeType.CONSTRAINT, null, false, null, null, null);
        Node ucc2 = new Node("UCC2", NodeType.CONSTRAINT, null, false, null, null, null);
        Node ucc_size_1 = new Node("UCC#1", NodeType.CONSTRAINT, null, false, null, null, null);

        //Check vertices
        Assertions.assertEquals(20, graphRepresentation.vertexSet().size());

        Assertions.assertTrue(graphRepresentation.containsVertex(schema));
        Assertions.assertTrue(graphRepresentation.containsVertex(column));
        Assertions.assertTrue(graphRepresentation.containsVertex(table));
        Assertions.assertTrue(graphRepresentation.containsVertex(columnType));
        Assertions.assertTrue(graphRepresentation.containsVertex(constraint));
        Assertions.assertTrue(graphRepresentation.containsVertex(source));
        Assertions.assertTrue(graphRepresentation.containsVertex(authors));
        Assertions.assertTrue(graphRepresentation.containsVertex(aid));
        Assertions.assertTrue(graphRepresentation.containsVertex(name));
        Assertions.assertTrue(graphRepresentation.containsVertex(integer));
        Assertions.assertTrue(graphRepresentation.containsVertex(string));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID1));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID2));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID3));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID4));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID5));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID6));
        Assertions.assertTrue(graphRepresentation.containsVertex(ucc1));
        Assertions.assertTrue(graphRepresentation.containsVertex(ucc2));
        Assertions.assertTrue(graphRepresentation.containsVertex(ucc_size_1));

        //Check Edges
        Assertions.assertEquals(23, graphRepresentation.edgeSet().size());

        //All Edges from the Database Node / root-Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, source));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID1, source));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, schema));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID1, schema));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, NodeID2));
        Assertions.assertEquals(new LabelEdge("table"), graphRepresentation.getEdge(NodeID1, NodeID2));

        //All Edges from the Table Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, authors));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID2, authors));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, table));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID2, table));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, NodeID3));
        Assertions.assertEquals(new LabelEdge("column"), graphRepresentation.getEdge(NodeID2, NodeID3));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, NodeID5));
        Assertions.assertEquals(new LabelEdge("column"), graphRepresentation.getEdge(NodeID2, NodeID5));

        //All Edges from the first column (aid) Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, aid));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID3, aid));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, column));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID3, column));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, NodeID4));
        Assertions.assertEquals(new LabelEdge("datatype"), graphRepresentation.getEdge(NodeID3, NodeID4));

        //All Edges from the second column (name) Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, name));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID5, name));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, column));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID5, column));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, NodeID6));
        Assertions.assertEquals(new LabelEdge("datatype"), graphRepresentation.getEdge(NodeID5, NodeID6));

        //All Edges from the Integer Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID4, integer));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID4, integer));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID4, columnType));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID4, columnType));

        //All Edges from the String Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID6, string));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID6, string));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID6, columnType));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID6, columnType));

        //All Test for Extra UCC Edges
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, ucc1));
        Assertions.assertEquals(new LabelEdge("ucc"), graphRepresentation.getEdge(NodeID3, ucc1));
        Assertions.assertTrue(graphRepresentation.containsEdge(ucc1, ucc_size_1));
        Assertions.assertEquals(new LabelEdge("size"), graphRepresentation.getEdge(ucc1, ucc_size_1));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, ucc2));
        Assertions.assertEquals(new LabelEdge("ucc"), graphRepresentation.getEdge(NodeID5, ucc2));
        Assertions.assertTrue(graphRepresentation.containsEdge(ucc2, ucc_size_1));
        Assertions.assertEquals(new LabelEdge("size"), graphRepresentation.getEdge(ucc2, ucc_size_1));
        Assertions.assertTrue(graphRepresentation.containsEdge(ucc1, constraint));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(ucc1, constraint));
        Assertions.assertTrue(graphRepresentation.containsEdge(ucc2, constraint));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(ucc2, constraint));
    }

    @Test
    public void transformIntoGraphRepresentationWithINDV1() {
        TestUtils.TestData testData = TestUtils.getTestData();
        Scenario scenario = new Scenario(testData.getScenarios().get("test1").getPath());

        Database sourceDb = scenario.getSourceDatabase();
        Table sourceTable = sourceDb.getTables().get(0);
        DatabaseMetadata metadata = new DatabaseMetadata();

        //Add test Dependency to Database
        Column columnAid = sourceDb.getTableByName("authors").getColumn(0);
        Column columnName = sourceDb.getTableByName("authors").getColumn(1);

        //Test Dependency name [= aid
        InclusionDependency inclusionDependency = new InclusionDependency(List.of(columnName), List.of(columnAid));
        metadata.setInds(List.of(inclusionDependency));
        sourceDb.setMetadata(metadata);

        Graph<Node, LabelEdge> graphRepresentation = similarityFlooding.transformIntoGraphRepresentationSchema(sourceDb, false, false, false, false);

        //All Type Nodes
        Node schema = new Node("Schema", NodeType.DATABASE, null, false, null, null, null);
        Node column = new Node("Column", NodeType.COLUMN, null, false, null, null, null);
        Node table = new Node("Table", NodeType.TABLE, null, false, null, null, null);
        Node columnType = new Node("ColumnType", NodeType.COLUMN_TYPE, null, false, null, null, null);

        //All Name Nodes
        Node source = new Node("source", NodeType.DATABASE, null, false, null, null, null);
        Node authors = new Node("authors", NodeType.TABLE, null, false, null, null, null);
        Node aid = new Node("aid", NodeType.COLUMN, Datatype.INTEGER, false, null, sourceTable, null);
        Node name = new Node("name", NodeType.COLUMN, Datatype.STRING, false, null, sourceTable, null);
        Node integer = new Node("INTEGER", NodeType.COLUMN_TYPE, Datatype.INTEGER, false, null, null, null);
        Node string = new Node("STRING", NodeType.COLUMN_TYPE, Datatype.STRING, false, null, null, null);

        //All Database Nodes
        Node NodeID1 = new Node("NodeID1", NodeType.DATABASE, null, true, source, null, null);

        //All Table Nodes
        Node NodeID2 = new Node("NodeID2", NodeType.TABLE, null, true, authors, null, null);

        //All Column Nodes
        Node NodeID3 = new Node("NodeID3", NodeType.COLUMN, Datatype.INTEGER, true, aid, sourceTable, null);
        Node NodeID5 = new Node("NodeID5", NodeType.COLUMN, Datatype.STRING, true, name, sourceTable, null);

        //All ColumnType Nodes
        Node NodeID4 = new Node("NodeID4", NodeType.COLUMN_TYPE, Datatype.INTEGER, true, integer, null, null);
        Node NodeID6 = new Node("NodeID6", NodeType.COLUMN_TYPE, Datatype.STRING, true, string, null, null);

        //Check vertices
        Assertions.assertEquals(16, graphRepresentation.vertexSet().size());

        Assertions.assertTrue(graphRepresentation.containsVertex(schema));
        Assertions.assertTrue(graphRepresentation.containsVertex(column));
        Assertions.assertTrue(graphRepresentation.containsVertex(table));
        Assertions.assertTrue(graphRepresentation.containsVertex(columnType));
        Assertions.assertTrue(graphRepresentation.containsVertex(source));
        Assertions.assertTrue(graphRepresentation.containsVertex(authors));
        Assertions.assertTrue(graphRepresentation.containsVertex(aid));
        Assertions.assertTrue(graphRepresentation.containsVertex(name));
        Assertions.assertTrue(graphRepresentation.containsVertex(integer));
        Assertions.assertTrue(graphRepresentation.containsVertex(string));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID1));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID2));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID3));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID4));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID5));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID6));

        //Check Edges
        Assertions.assertEquals(18, graphRepresentation.edgeSet().size());

        //All Edges from the Database Node / root-Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, source));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID1, source));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, schema));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID1, schema));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, NodeID2));
        Assertions.assertEquals(new LabelEdge("table"), graphRepresentation.getEdge(NodeID1, NodeID2));

        //All Edges from the Table Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, authors));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID2, authors));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, table));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID2, table));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, NodeID3));
        Assertions.assertEquals(new LabelEdge("column"), graphRepresentation.getEdge(NodeID2, NodeID3));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, NodeID5));
        Assertions.assertEquals(new LabelEdge("column"), graphRepresentation.getEdge(NodeID2, NodeID5));

        //All Edges from the first column (aid) Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, aid));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID3, aid));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, column));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID3, column));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, NodeID4));
        Assertions.assertEquals(new LabelEdge("datatype"), graphRepresentation.getEdge(NodeID3, NodeID4));

        //All Edges from the second column (name) Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, name));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID5, name));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, column));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID5, column));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, NodeID6));
        Assertions.assertEquals(new LabelEdge("datatype"), graphRepresentation.getEdge(NodeID5, NodeID6));

        //All Edges from the Integer Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID4, integer));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID4, integer));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID4, columnType));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID4, columnType));

        //All Edges from the String Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID6, string));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID6, string));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID6, columnType));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID6, columnType));

        //All Test for Extra Dependency Edges
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, NodeID5));
        Assertions.assertEquals(new LabelEdge("contains"), graphRepresentation.getEdge(NodeID3, NodeID5));
    }

    @Test
    public void transformIntoGraphRepresentationWithINDV2() {
        TestUtils.TestData testData = TestUtils.getTestData();
        Scenario scenario = new Scenario(testData.getScenarios().get("test1").getPath());

        Database sourceDb = scenario.getSourceDatabase();
        Table sourceTable = sourceDb.getTables().get(0);
        DatabaseMetadata metadata = new DatabaseMetadata();

        //Add test Dependency to Database
        Column columnAid = sourceDb.getTableByName("authors").getColumn(0);
        Column columnName = sourceDb.getTableByName("authors").getColumn(1);

        //Test Dependency name [= aid
        InclusionDependency inclusionDependency = new InclusionDependency(List.of(columnName), List.of(columnAid));
        metadata.setInds(List.of(inclusionDependency));
        sourceDb.setMetadata(metadata);

        Graph<Node, LabelEdge> graphRepresentation = similarityFlooding.transformIntoGraphRepresentationSchema(sourceDb, false, false, false, false);

        //All Type Nodes
        Node schema = new Node("Schema", NodeType.DATABASE, null, false, null, null, null);
        Node column = new Node("Column", NodeType.COLUMN, null, false, null, null, null);
        Node table = new Node("Table", NodeType.TABLE, null, false, null, null, null);
        Node columnType = new Node("ColumnType", NodeType.COLUMN_TYPE, null, false, null, null, null);
        Node constraint = new Node("Constraint", NodeType.CONSTRAINT, null, false, null, null, null);

        //All Name Nodes
        Node source = new Node("source", NodeType.DATABASE, null, false, null, null, null);
        Node authors = new Node("authors", NodeType.TABLE, null, false, null, null, null);
        Node aid = new Node("aid", NodeType.COLUMN, Datatype.INTEGER, false, null, sourceTable, null);
        Node name = new Node("name", NodeType.COLUMN, Datatype.STRING, false, null, sourceTable, null);
        Node integer = new Node("INTEGER", NodeType.COLUMN_TYPE, Datatype.INTEGER, false, null, null, null);
        Node string = new Node("STRING", NodeType.COLUMN_TYPE, Datatype.STRING, false, null, null, null);

        //All Database Nodes
        Node NodeID1 = new Node("NodeID1", NodeType.DATABASE, null, true, source, null, null);

        //All Table Nodes
        Node NodeID2 = new Node("NodeID2", NodeType.TABLE, null, true, authors, null, null);

        //All Column Nodes
        Node NodeID3 = new Node("NodeID3", NodeType.COLUMN, Datatype.INTEGER, true, aid, sourceTable, null);
        Node NodeID5 = new Node("NodeID5", NodeType.COLUMN, Datatype.STRING, true, name, sourceTable, null);

        //All ColumnType Nodes
        Node NodeID4 = new Node("NodeID4", NodeType.COLUMN_TYPE, Datatype.INTEGER, true, integer, null, null);
        Node NodeID6 = new Node("NodeID6", NodeType.COLUMN_TYPE, Datatype.STRING, true, string, null, null);

        //Additional IND Nodes
        Node ind1 = new Node("IND1", NodeType.CONSTRAINT, null, false, null, null, null);

        //Check vertices
        Assertions.assertEquals(18, graphRepresentation.vertexSet().size());

        Assertions.assertTrue(graphRepresentation.containsVertex(schema));
        Assertions.assertTrue(graphRepresentation.containsVertex(column));
        Assertions.assertTrue(graphRepresentation.containsVertex(table));
        Assertions.assertTrue(graphRepresentation.containsVertex(columnType));
        Assertions.assertTrue(graphRepresentation.containsVertex(constraint));
        Assertions.assertTrue(graphRepresentation.containsVertex(source));
        Assertions.assertTrue(graphRepresentation.containsVertex(authors));
        Assertions.assertTrue(graphRepresentation.containsVertex(aid));
        Assertions.assertTrue(graphRepresentation.containsVertex(name));
        Assertions.assertTrue(graphRepresentation.containsVertex(integer));
        Assertions.assertTrue(graphRepresentation.containsVertex(string));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID1));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID2));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID3));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID4));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID5));
        Assertions.assertTrue(graphRepresentation.containsVertex(NodeID6));
        Assertions.assertTrue(graphRepresentation.containsVertex(ind1));

        //Check Edges
        Assertions.assertEquals(20, graphRepresentation.edgeSet().size());

        //All Edges from the Database Node / root-Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, source));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID1, source));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, schema));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID1, schema));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID1, NodeID2));
        Assertions.assertEquals(new LabelEdge("table"), graphRepresentation.getEdge(NodeID1, NodeID2));

        //All Edges from the Table Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, authors));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID2, authors));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, table));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID2, table));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, NodeID3));
        Assertions.assertEquals(new LabelEdge("column"), graphRepresentation.getEdge(NodeID2, NodeID3));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID2, NodeID5));
        Assertions.assertEquals(new LabelEdge("column"), graphRepresentation.getEdge(NodeID2, NodeID5));

        //All Edges from the first column (aid) Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, aid));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID3, aid));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, column));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID3, column));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID3, NodeID4));
        Assertions.assertEquals(new LabelEdge("datatype"), graphRepresentation.getEdge(NodeID3, NodeID4));

        //All Edges from the second column (name) Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, name));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID5, name));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, column));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID5, column));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID5, NodeID6));
        Assertions.assertEquals(new LabelEdge("datatype"), graphRepresentation.getEdge(NodeID5, NodeID6));

        //All Edges from the Integer Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID4, integer));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID4, integer));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID4, columnType));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID4, columnType));

        //All Edges from the String Node
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID6, string));
        Assertions.assertEquals(new LabelEdge("name"), graphRepresentation.getEdge(NodeID6, string));
        Assertions.assertTrue(graphRepresentation.containsEdge(NodeID6, columnType));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(NodeID6, columnType));

        //All Test for Extra Dependency Edges
        Assertions.assertTrue(graphRepresentation.containsEdge(ind1, NodeID3));
        Assertions.assertEquals(new LabelEdge("referenced"), graphRepresentation.getEdge(ind1, NodeID3));
        Assertions.assertTrue(graphRepresentation.containsEdge(ind1, NodeID5));
        Assertions.assertEquals(new LabelEdge("dependant"), graphRepresentation.getEdge(ind1, NodeID5));
        Assertions.assertTrue(graphRepresentation.containsEdge(ind1, constraint));
        Assertions.assertEquals(new LabelEdge("type"), graphRepresentation.getEdge(ind1, constraint));
    }

    @Test
    public void createConnectivityGraph() {

        //Build Graph Model A
        Graph<Node, LabelEdge> modelA = new DefaultDirectedGraph<>(LabelEdge.class);
        Node a = new Node("a", null, null, false, null, null, null);
        Node a1 = new Node("a1", null, null, false, null, null, null);
        Node a2 = new Node("a2", null, null, false, null, null, null);

        modelA.addVertex(a);
        modelA.addVertex(a1);
        modelA.addVertex(a2);

        modelA.addEdge(a, a1, new LabelEdge("l1"));
        modelA.addEdge(a, a2, new LabelEdge("l1"));
        modelA.addEdge(a1, a2, new LabelEdge("l2"));

        //Build Graph Model B
        Graph<Node, LabelEdge> modelB = new DefaultDirectedGraph<>(LabelEdge.class);
        Node b = new Node("b", null, null, false, null, null, null);
        Node b1 = new Node("b1", null, null, false, null, null, null);
        Node b2 = new Node("b2", null, null, false, null, null, null);

        modelB.addVertex(b);
        modelB.addVertex(b1);
        modelB.addVertex(b2);

        modelB.addEdge(b, b1, new LabelEdge("l1"));
        modelB.addEdge(b, b2, new LabelEdge("l2"));
        modelB.addEdge(b2, b1, new LabelEdge("l2"));

        Graph<NodePair, LabelEdge> connectivityGraph = similarityFlooding.createConnectivityGraph(modelA, modelB);

        NodePair ab = new NodePair(a, b);
        NodePair a1b1 = new NodePair(a1, b1);
        NodePair a2b1 = new NodePair(a2, b1);
        NodePair a1b2 = new NodePair(a1, b2);
        NodePair a1b = new NodePair(a1, b);
        NodePair a2b2 = new NodePair(a2, b2);

        //ConnectivityGraph should have 6 nodes
        Assertions.assertEquals(6, connectivityGraph.vertexSet().size());

        //Check for correct vertices (NodePairs)
        Assertions.assertTrue(connectivityGraph.containsVertex(ab));
        Assertions.assertTrue(connectivityGraph.containsVertex(a1b1));
        Assertions.assertTrue(connectivityGraph.containsVertex(a2b1));
        Assertions.assertTrue(connectivityGraph.containsVertex(a1b2));
        Assertions.assertTrue(connectivityGraph.containsVertex(a1b));
        Assertions.assertTrue(connectivityGraph.containsVertex(a2b2));

        //ConnectivityGraph should have 4 edges
        Assertions.assertEquals(4, connectivityGraph.edgeSet().size());

        //Check for correctly labeled edges
        Assertions.assertTrue(connectivityGraph.containsEdge(ab, a1b1));
        Assertions.assertEquals(new LabelEdge("l1"), connectivityGraph.getEdge(ab, a1b1));

        Assertions.assertTrue(connectivityGraph.containsEdge(ab, a2b1));
        Assertions.assertEquals(new LabelEdge("l1"), connectivityGraph.getEdge(ab, a2b1));

        Assertions.assertTrue(connectivityGraph.containsEdge(a1b2, a2b1));
        Assertions.assertEquals(new LabelEdge("l2"), connectivityGraph.getEdge(a1b2, a2b1));

        Assertions.assertTrue(connectivityGraph.containsEdge(a1b, a2b2));
        Assertions.assertEquals(new LabelEdge("l2"), connectivityGraph.getEdge(a1b, a2b2));
    }

    @Test
    public void inducePropagationGraph() {

        //Build Graph Model A
        Graph<Node, LabelEdge> modelA = new DefaultDirectedGraph<>(LabelEdge.class);
        Node a = new Node("a", null, null, false, null, null, null);
        Node a1 = new Node("a1", null, null, false, null, null, null);
        Node a2 = new Node("a2", null, null, false, null, null, null);

        modelA.addVertex(a);
        modelA.addVertex(a1);
        modelA.addVertex(a2);

        modelA.addEdge(a, a1, new LabelEdge("l1"));
        modelA.addEdge(a, a2, new LabelEdge("l1"));
        modelA.addEdge(a1, a2, new LabelEdge("l2"));

        //Build Graph Model B
        Graph<Node, LabelEdge> modelB = new DefaultDirectedGraph<>(LabelEdge.class);
        Node b = new Node("b", null, null, false, null, null, null);
        Node b1 = new Node("b1", null, null, false, null, null, null);
        Node b2 = new Node("b2", null, null, false, null, null, null);

        modelB.addVertex(b);
        modelB.addVertex(b1);
        modelB.addVertex(b2);

        modelB.addEdge(b, b1, new LabelEdge("l1"));
        modelB.addEdge(b, b2, new LabelEdge("l2"));
        modelB.addEdge(b2, b1, new LabelEdge("l2"));

        Graph<NodePair, LabelEdge> connectivityGraph = similarityFlooding.createConnectivityGraph(modelA, modelB);

        //Test for PropagationGraph constructed with the InverseProduct policy
        Graph<NodePair, CoefficientEdge> propagationGraphInverseProduct = similarityFlooding.inducePropagationGraph(connectivityGraph, modelA, modelB, PropagationCoefficientPolicy.INVERSE_PRODUCT);

        NodePair ab = new NodePair(a, b);
        NodePair a1b1 = new NodePair(a1, b1);
        NodePair a2b1 = new NodePair(a2, b1);
        NodePair a1b2 = new NodePair(a1, b2);
        NodePair a1b = new NodePair(a1, b);
        NodePair a2b2 = new NodePair(a2, b2);

        //PropagationGraph should have 6 nodes
        Assertions.assertEquals(6, propagationGraphInverseProduct.vertexSet().size());

        //Check for correct vertices (NodePairs)
        Assertions.assertTrue(propagationGraphInverseProduct.containsVertex(ab));
        Assertions.assertTrue(propagationGraphInverseProduct.containsVertex(a1b1));
        Assertions.assertTrue(propagationGraphInverseProduct.containsVertex(a2b1));
        Assertions.assertTrue(propagationGraphInverseProduct.containsVertex(a1b2));
        Assertions.assertTrue(propagationGraphInverseProduct.containsVertex(a1b));
        Assertions.assertTrue(propagationGraphInverseProduct.containsVertex(a2b2));

        //ConnectivityGraph should have 4 edges
        Assertions.assertEquals(8, propagationGraphInverseProduct.edgeSet().size());

        //Check for correctly labeled edges
        Assertions.assertTrue(propagationGraphInverseProduct.containsEdge(ab, a1b1));
        Assertions.assertTrue(propagationGraphInverseProduct.containsEdge(a1b1, ab));
        Assertions.assertEquals(new CoefficientEdge(0.5), propagationGraphInverseProduct.getEdge(ab, a1b1));
        Assertions.assertEquals(new CoefficientEdge(1.0), propagationGraphInverseProduct.getEdge(a1b1, ab));

        Assertions.assertTrue(propagationGraphInverseProduct.containsEdge(ab, a2b1));
        Assertions.assertTrue(propagationGraphInverseProduct.containsEdge(a2b1, ab));
        Assertions.assertEquals(new CoefficientEdge(0.5), propagationGraphInverseProduct.getEdge(ab, a2b1));
        Assertions.assertEquals(new CoefficientEdge(1.0), propagationGraphInverseProduct.getEdge(a2b1, ab));

        Assertions.assertTrue(propagationGraphInverseProduct.containsEdge(a1b2, a2b1));
        Assertions.assertTrue(propagationGraphInverseProduct.containsEdge(a2b1, a1b2));
        Assertions.assertEquals(new CoefficientEdge(1.0), propagationGraphInverseProduct.getEdge(a1b2, a2b1));
        Assertions.assertEquals(new CoefficientEdge(1.0), propagationGraphInverseProduct.getEdge(a2b1, a1b2));

        Assertions.assertTrue(propagationGraphInverseProduct.containsEdge(a1b, a2b2));
        Assertions.assertTrue(propagationGraphInverseProduct.containsEdge(a2b2, a1b));
        Assertions.assertEquals(new CoefficientEdge(1.0), propagationGraphInverseProduct.getEdge(a1b, a2b2));
        Assertions.assertEquals(new CoefficientEdge(1.0), propagationGraphInverseProduct.getEdge(a2b2, a1b));

        //Test for PropagationGraph constructed with the InverseAverage policy
        Graph<NodePair, CoefficientEdge> propagationGraphInverseAverage = similarityFlooding.inducePropagationGraph(connectivityGraph, modelA, modelB, PropagationCoefficientPolicy.INVERSE_AVERAGE);

        //PropagationGraph should have 6 nodes
        Assertions.assertEquals(6, propagationGraphInverseAverage.vertexSet().size());

        //Check for correct vertices (NodePairs)
        Assertions.assertTrue(propagationGraphInverseAverage.containsVertex(ab));
        Assertions.assertTrue(propagationGraphInverseAverage.containsVertex(a1b1));
        Assertions.assertTrue(propagationGraphInverseAverage.containsVertex(a2b1));
        Assertions.assertTrue(propagationGraphInverseAverage.containsVertex(a1b2));
        Assertions.assertTrue(propagationGraphInverseAverage.containsVertex(a1b));
        Assertions.assertTrue(propagationGraphInverseAverage.containsVertex(a2b2));

        //ConnectivityGraph should have 4 edges
        Assertions.assertEquals(8, propagationGraphInverseAverage.edgeSet().size());

        //Check for correctly labeled edges
        Assertions.assertTrue(propagationGraphInverseAverage.containsEdge(ab, a1b1));
        Assertions.assertTrue(propagationGraphInverseAverage.containsEdge(a1b1, ab));
        Assertions.assertEquals(new CoefficientEdge(2.0 / 3.0), propagationGraphInverseAverage.getEdge(ab, a1b1));
        Assertions.assertEquals(new CoefficientEdge(1.0), propagationGraphInverseAverage.getEdge(a1b1, ab));

        Assertions.assertTrue(propagationGraphInverseAverage.containsEdge(ab, a2b1));
        Assertions.assertTrue(propagationGraphInverseAverage.containsEdge(a2b1, ab));
        Assertions.assertEquals(new CoefficientEdge(2.0 / 3.0), propagationGraphInverseAverage.getEdge(ab, a2b1));
        Assertions.assertEquals(new CoefficientEdge(1.0), propagationGraphInverseAverage.getEdge(a2b1, ab));

        Assertions.assertTrue(propagationGraphInverseAverage.containsEdge(a1b2, a2b1));
        Assertions.assertTrue(propagationGraphInverseAverage.containsEdge(a2b1, a1b2));
        Assertions.assertEquals(new CoefficientEdge(1.0), propagationGraphInverseAverage.getEdge(a1b2, a2b1));
        Assertions.assertEquals(new CoefficientEdge(1.0), propagationGraphInverseAverage.getEdge(a2b1, a1b2));

        Assertions.assertTrue(propagationGraphInverseAverage.containsEdge(a1b, a2b2));
        Assertions.assertTrue(propagationGraphInverseAverage.containsEdge(a2b2, a1b));
        Assertions.assertEquals(new CoefficientEdge(1.0), propagationGraphInverseAverage.getEdge(a1b, a2b2));
        Assertions.assertEquals(new CoefficientEdge(1.0), propagationGraphInverseAverage.getEdge(a2b2, a1b));
    }
}
