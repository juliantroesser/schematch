package de.uni_marburg.schematch.matching.similarity_flooding;

import de.uni_marburg.schematch.TestUtils;
import de.uni_marburg.schematch.data.Database;
import de.uni_marburg.schematch.data.Scenario;
import de.uni_marburg.schematch.matching.matrix_boosting.similarity_flooding.*;
import org.jgrapht.Graph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class GraphTransformationTest {

    Database sourceDb;
    Database targetDb;
    SimilarityFlooding similarityFlooding = new SimilarityFlooding();
    Graph<Node, LabelEdge> graph1;
    Graph<Node, LabelEdge> graph2;
    Graph<NodePair, LabelEdge> connectivityGraph;
    Graph<NodePair, CoefficientEdge> propagationGraph;
    PropagationCoefficientPolicy propagationCoefficientPolicy = PropagationCoefficientPolicy.INVERSE_AVERAGE;
    Map<NodePair, Double> initialMapping;

    @BeforeEach
    public void init() {
        TestUtils.TestData testData = TestUtils.getTestData();
        Scenario scenario = new Scenario(testData.getScenarios().get("test1").getPath());
        sourceDb = scenario.getSourceDatabase();
        targetDb = scenario.getTargetDatabase();
        graph1 = similarityFlooding.transformIntoValentineGraphRepresentation(sourceDb);
        graph2 = similarityFlooding.transformIntoValentineGraphRepresentation(targetDb);
        connectivityGraph = similarityFlooding.createConnectivityGraph(graph1, graph2);
        propagationGraph = similarityFlooding.inducePropagationGraph(connectivityGraph, graph1, graph2, propagationCoefficientPolicy);
        initialMapping = similarityFlooding.calculateInitialMappingValentine(propagationGraph); //Valentine Initial Mapping
    }

    public void transformIntoGraphRepresentationTest() {
        Graph<Node, LabelEdge> graphRepresentation = similarityFlooding.transformIntoGraphRepresentation(sourceDb);
    }

    //Tests the Transformation of the source Schema into the Valentine Graph Representation
    @Test
    public void transformIntoValentineGraphRepresentationTest() {

        //Graph should have 13 nodes
        int nodeCount = graph1.vertexSet().size();
        Assertions.assertEquals(13, nodeCount);

        //Graph should have 14 edges
        int edgeCount = graph1.edgeSet().size();
        Assertions.assertEquals(14, edgeCount);

        Node table = new Node("Table", null, null, false, false);
        Node column = new Node("Column", null, null, false, false);
        Node columnType = new Node("ColumnType", null, null, false, false);
        Node nodeID1 = new Node("NodeID1", null, null, false, false);
        Node authors = new Node("authors", null, null, false, false);
        Node nodeID2 = new Node("NodeID2", null, null, false, false);
        Node aid = new Node("aid", null, null, false, false);
        Node nodeID3 = new Node("NodeID3", null, null, false, false);
        Node integer = new Node("int", null, null, false, false);
        Node nodeID4 = new Node("NodeID4", null, null, false, false);
        Node name = new Node("name", null, null, false, false);
        Node nodeID5 = new Node("NodeID5", null, null, false, false);
        Node varchar = new Node("varchar", null, null, false, false);

        //Alle Knoten überprüfen
        Assertions.assertTrue(graph1.containsVertex(table));
        Assertions.assertTrue(graph1.containsVertex(column));
        Assertions.assertTrue(graph1.containsVertex(columnType));
        Assertions.assertTrue(graph1.containsVertex(nodeID1));
        Assertions.assertTrue(graph1.containsVertex(authors));
        Assertions.assertTrue(graph1.containsVertex(nodeID2));
        Assertions.assertTrue(graph1.containsVertex(aid));
        Assertions.assertTrue(graph1.containsVertex(nodeID3));
        Assertions.assertTrue(graph1.containsVertex(integer));
        Assertions.assertTrue(graph1.containsVertex(nodeID4));
        Assertions.assertTrue(graph1.containsVertex(name));
        Assertions.assertTrue(graph1.containsVertex(nodeID5));
        Assertions.assertTrue(graph1.containsVertex(varchar));

        //Alle Kanten überprüfen
        Assertions.assertTrue(graph1.containsEdge(nodeID1, authors));
        Assertions.assertEquals("name", graph1.getEdge(nodeID1, authors).getValue());

        Assertions.assertTrue(graph1.containsEdge(nodeID1, table));
        Assertions.assertEquals("type", graph1.getEdge(nodeID1, table).getValue());

        Assertions.assertTrue(graph1.containsEdge(nodeID1, nodeID2));
        Assertions.assertEquals("column", graph1.getEdge(nodeID1, nodeID2).getValue());

        Assertions.assertTrue(graph1.containsEdge(nodeID1, nodeID4));
        Assertions.assertEquals("column", graph1.getEdge(nodeID1, nodeID4).getValue());

        Assertions.assertTrue(graph1.containsEdge(nodeID2, column));
        Assertions.assertEquals("type", graph1.getEdge(nodeID2, column).getValue());

        Assertions.assertTrue(graph1.containsEdge(nodeID2, aid));
        Assertions.assertEquals("name", graph1.getEdge(nodeID2, aid).getValue());

        Assertions.assertTrue(graph1.containsEdge(nodeID2, nodeID3));
        Assertions.assertEquals("SQLtype", graph1.getEdge(nodeID2, nodeID3).getValue());

        Assertions.assertTrue(graph1.containsEdge(nodeID3, columnType));
        Assertions.assertEquals("type", graph1.getEdge(nodeID3, columnType).getValue());

        Assertions.assertTrue(graph1.containsEdge(nodeID3, integer));
        Assertions.assertEquals("name", graph1.getEdge(nodeID3, integer).getValue());

        Assertions.assertTrue(graph1.containsEdge(nodeID4, column));
        Assertions.assertEquals("type", graph1.getEdge(nodeID4, column).getValue());

        Assertions.assertTrue(graph1.containsEdge(nodeID4, name));
        Assertions.assertEquals("name", graph1.getEdge(nodeID4, name).getValue());

        Assertions.assertTrue(graph1.containsEdge(nodeID4, nodeID5));
        Assertions.assertEquals("SQLtype", graph1.getEdge(nodeID4, nodeID5).getValue());

        Assertions.assertTrue(graph1.containsEdge(nodeID5, columnType));
        Assertions.assertEquals("type", graph1.getEdge(nodeID5, columnType).getValue());

        Assertions.assertTrue(graph1.containsEdge(nodeID5, varchar));
        Assertions.assertEquals("name", graph1.getEdge(nodeID5, varchar).getValue());
    }

    //ConnectivityGraphTest for Valentine
    //TODO: PropagationGraph Aufbau überarbeiten, sodass keine Operationen mehr direkt auf dem ConnectivityGraph ausgeführt werden
    @Test
    public void createConnectivityGraphTest() {

        //ConnectivityGraph should have 59 nodes
        Assertions.assertEquals(59, connectivityGraph.vertexSet().size());

        //ConnectivityGraph should have 58 edges
        Assertions.assertEquals(58, connectivityGraph.edgeSet().size());

        Node graph1_table = new Node("Table", null, null, false, false);
        Node graph1_column = new Node("Column", null, null, false, false);
        Node graph1_columnType = new Node("ColumnType", null, null, false, false);
        Node graph1_nodeID1 = new Node("NodeID1", null, null, false, false);
        Node graph1_authors = new Node("authors", null, null, false, false);
        Node graph1_nodeID2 = new Node("NodeID2", null, null, false, false);
        Node graph1_aid = new Node("aid", null, null, false, false);
        Node graph1_nodeID3 = new Node("NodeID3", null, null, false, false);
        Node graph1_integer = new Node("int", null, null, false, false);
        Node graph1_nodeID4 = new Node("NodeID4", null, null, false, false);
        Node graph1_name = new Node("name", null, null, false, false);
        Node graph1_nodeID5 = new Node("NodeID5", null, null, false, false);
        Node graph1_varchar = new Node("varchar", null, null, false, false);

        Node graph2_table = new Node("Table", null, null, false, false);
        Node graph2_column = new Node("Column", null, null, false, false);
        Node graph2_columnType = new Node("ColumnType", null, null, false, false);
        Node graph2_nodeID1 = new Node("NodeID1", null, null, false, false);
        Node graph2_authors = new Node("authors", null, null, false, false);
        Node graph2_nodeID2 = new Node("NodeID2", null, null, false, false);
        Node graph2_authorid = new Node("authorid", null, null, false, false);
        Node graph2_nodeID3 = new Node("NodeID3", null, null, false, false);
        Node graph2_integer = new Node("int", null, null, false, false);
        Node graph2_nodeID4 = new Node("NodeID4", null, null, false, false);
        Node graph2_full_name = new Node("full_name", null, null, false, false);
        Node graph2_nodeID5 = new Node("NodeID5", null, null, false, false);
        Node graph2_varchar = new Node("varchar", null, null, false, false);

        //Testen, ob alle Knoten vorhanden
        NodePair nodeID1_NodeID1 = new NodePair(graph1_nodeID1, graph2_nodeID1);
        NodePair authors_authors = new NodePair(graph1_authors, graph2_authors);
        NodePair nodeID1_NodeID2 = new NodePair(graph1_nodeID1, graph2_nodeID2);
        NodePair authors_authorid = new NodePair(graph1_authors, graph2_authorid);
        NodePair nodeID1_NodeID3 = new NodePair(graph1_nodeID1, graph2_nodeID3);
        NodePair authors_int = new NodePair(graph1_authors, graph2_integer);
        NodePair nodeID1_NodeID4 = new NodePair(graph1_nodeID1, graph2_nodeID4);
        NodePair authors_full_name = new NodePair(graph1_authors, graph2_full_name);
        NodePair nodeID1_NodeID5 = new NodePair(graph1_nodeID1, graph2_nodeID5);
        NodePair authors_varchar = new NodePair(graph1_authors, graph2_varchar);
        NodePair table_table = new NodePair(graph1_table, graph2_table);
        NodePair table_column = new NodePair(graph1_table, graph2_column);
        NodePair table_columnType = new NodePair(graph1_table, graph2_columnType);
        NodePair nodeID2_NodeID2 = new NodePair(graph1_nodeID2, graph2_nodeID2);
        NodePair nodeID2_NodeID4 = new NodePair(graph1_nodeID2, graph2_nodeID4);
        NodePair nodeID4_NodeID2 = new NodePair(graph1_nodeID4, graph2_nodeID2);
        NodePair nodeID4_NodeID4 = new NodePair(graph1_nodeID4, graph2_nodeID4);
        NodePair nodeID2_NodeID1 = new NodePair(graph1_nodeID2, graph2_nodeID1);
        NodePair column_table = new NodePair(graph1_column, graph2_table);
        NodePair column_column = new NodePair(graph1_column, graph2_column);
        NodePair nodeID2_NodeID3 = new NodePair(graph1_nodeID2, graph2_nodeID3);
        NodePair column_columnType = new NodePair(graph1_column, graph2_columnType);
        NodePair nodeID2_NodeID5 = new NodePair(graph1_nodeID2, graph2_nodeID5);
        NodePair aid_authors = new NodePair(graph1_aid, graph2_authors);
        NodePair aid_authorid = new NodePair(graph1_aid, graph2_authorid);
        NodePair aid_int = new NodePair(graph1_aid, graph2_integer);
        NodePair aid_full_name = new NodePair(graph1_aid, graph2_full_name);
        NodePair aid_varchar = new NodePair(graph1_aid, graph2_varchar);
        NodePair nodeID3_NodeID3 = new NodePair(graph1_nodeID3, graph2_nodeID3);
        NodePair nodeID3_NodeID5 = new NodePair(graph1_nodeID3, graph2_nodeID5);
        NodePair nodeID3_NodeID1 = new NodePair(graph1_nodeID3, graph2_nodeID1);
        NodePair columnType_table = new NodePair(graph1_columnType, graph2_table);
        NodePair nodeID3_NodeID2 = new NodePair(graph1_nodeID3, graph2_nodeID2);
        NodePair columnType_column = new NodePair(graph1_columnType, graph2_column);
        NodePair columnType_columnType = new NodePair(graph1_columnType, graph2_columnType);
        NodePair nodeID3_NodeID4 = new NodePair(graph1_nodeID3, graph2_nodeID4);
        NodePair int_authors = new NodePair(graph1_integer, graph2_authors);
        NodePair int_authorid = new NodePair(graph1_integer, graph2_authorid);
        NodePair int_int = new NodePair(graph1_integer, graph2_integer);
        NodePair int_full_name = new NodePair(graph1_integer, graph2_full_name);
        NodePair int_varchar = new NodePair(graph1_integer, graph2_varchar);
        NodePair nodeID4_NodeID1 = new NodePair(graph1_nodeID4, graph2_nodeID1);
        NodePair nodeID4_NodeID3 = new NodePair(graph1_nodeID4, graph2_nodeID3);
        NodePair nodeID4_NodeID5 = new NodePair(graph1_nodeID4, graph2_nodeID5);
        NodePair name_authors = new NodePair(graph1_name, graph2_authors);
        NodePair name_authorid = new NodePair(graph1_name, graph2_authorid);
        NodePair name_int = new NodePair(graph1_name, graph2_integer);
        NodePair name_full_name = new NodePair(graph1_name, graph2_full_name);
        NodePair name_varchar = new NodePair(graph1_name, graph2_varchar);
        NodePair nodeID5_NodeID3 = new NodePair(graph1_nodeID5, graph2_nodeID3);
        NodePair nodeID5_NodeID5 = new NodePair(graph1_nodeID5, graph2_nodeID5);
        NodePair nodeID5_NodeID1 = new NodePair(graph1_nodeID5, graph2_nodeID1);
        NodePair nodeID5_NodeID2 = new NodePair(graph1_nodeID5, graph2_nodeID2);
        NodePair nodeID5_NodeID4 = new NodePair(graph1_nodeID5, graph2_nodeID4);
        NodePair varchar_authors = new NodePair(graph1_varchar, graph2_authors);
        NodePair varchar_authorid = new NodePair(graph1_varchar, graph2_authorid);
        NodePair varchar_int = new NodePair(graph1_varchar, graph2_integer);
        NodePair varchar_full_name = new NodePair(graph1_varchar, graph2_full_name);
        NodePair varchar_varchar = new NodePair(graph1_varchar, graph2_varchar);

        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID1_NodeID1));
        Assertions.assertTrue(connectivityGraph.containsVertex(authors_authors));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID1_NodeID2));
        Assertions.assertTrue(connectivityGraph.containsVertex(authors_authorid));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID1_NodeID3));
        Assertions.assertTrue(connectivityGraph.containsVertex(authors_int));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID1_NodeID4));
        Assertions.assertTrue(connectivityGraph.containsVertex(authors_full_name));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID1_NodeID5));
        Assertions.assertTrue(connectivityGraph.containsVertex(authors_varchar));
        Assertions.assertTrue(connectivityGraph.containsVertex(table_table));
        Assertions.assertTrue(connectivityGraph.containsVertex(table_column));
        Assertions.assertTrue(connectivityGraph.containsVertex(table_columnType));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID2_NodeID2));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID2_NodeID4));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID4_NodeID2));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID4_NodeID4));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID2_NodeID1));
        Assertions.assertTrue(connectivityGraph.containsVertex(column_table));
        Assertions.assertTrue(connectivityGraph.containsVertex(column_column));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID2_NodeID3));
        Assertions.assertTrue(connectivityGraph.containsVertex(column_columnType));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID2_NodeID5));
        Assertions.assertTrue(connectivityGraph.containsVertex(aid_authors));
        Assertions.assertTrue(connectivityGraph.containsVertex(aid_authorid));
        Assertions.assertTrue(connectivityGraph.containsVertex(aid_int));
        Assertions.assertTrue(connectivityGraph.containsVertex(aid_full_name));
        Assertions.assertTrue(connectivityGraph.containsVertex(aid_varchar));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID3_NodeID3));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID3_NodeID5));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID3_NodeID1));
        Assertions.assertTrue(connectivityGraph.containsVertex(columnType_table));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID3_NodeID2));
        Assertions.assertTrue(connectivityGraph.containsVertex(columnType_column));
        Assertions.assertTrue(connectivityGraph.containsVertex(columnType_columnType));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID3_NodeID4));
        Assertions.assertTrue(connectivityGraph.containsVertex(int_authors));
        Assertions.assertTrue(connectivityGraph.containsVertex(int_authorid));
        Assertions.assertTrue(connectivityGraph.containsVertex(int_int));
        Assertions.assertTrue(connectivityGraph.containsVertex(int_full_name));
        Assertions.assertTrue(connectivityGraph.containsVertex(int_varchar));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID4_NodeID1));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID4_NodeID3));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID4_NodeID5));
        Assertions.assertTrue(connectivityGraph.containsVertex(name_authors));
        Assertions.assertTrue(connectivityGraph.containsVertex(name_authorid));
        Assertions.assertTrue(connectivityGraph.containsVertex(name_int));
        Assertions.assertTrue(connectivityGraph.containsVertex(name_full_name));
        Assertions.assertTrue(connectivityGraph.containsVertex(name_varchar));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID5_NodeID3));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID5_NodeID5));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID5_NodeID1));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID5_NodeID2));
        Assertions.assertTrue(connectivityGraph.containsVertex(nodeID5_NodeID4));
        Assertions.assertTrue(connectivityGraph.containsVertex(varchar_authors));
        Assertions.assertTrue(connectivityGraph.containsVertex(varchar_authorid));
        Assertions.assertTrue(connectivityGraph.containsVertex(varchar_int));
        Assertions.assertTrue(connectivityGraph.containsVertex(varchar_full_name));
        Assertions.assertTrue(connectivityGraph.containsVertex(varchar_varchar));

        //Testen, ob alle Kanten vorhanden
        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID1_NodeID1, authors_authors));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID1_NodeID1, authors_authors).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID1_NodeID1, table_table));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID1_NodeID1, table_table).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID1_NodeID1, nodeID2_NodeID2));
        Assertions.assertEquals("column", connectivityGraph.getEdge(nodeID1_NodeID1, nodeID2_NodeID2).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID1_NodeID1, nodeID2_NodeID4));
        Assertions.assertEquals("column", connectivityGraph.getEdge(nodeID1_NodeID1, nodeID2_NodeID4).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID1_NodeID1, nodeID4_NodeID2));
        Assertions.assertEquals("column", connectivityGraph.getEdge(nodeID1_NodeID1, nodeID4_NodeID2).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID1_NodeID1, nodeID4_NodeID4));
        Assertions.assertEquals("column", connectivityGraph.getEdge(nodeID1_NodeID1, nodeID4_NodeID4).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID1_NodeID2, authors_authorid));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID1_NodeID2, authors_authorid).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID1_NodeID2, table_column));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID1_NodeID2, table_column).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID1_NodeID3, authors_int));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID1_NodeID3, authors_int).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID1_NodeID3, table_columnType));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID1_NodeID3, table_columnType).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID1_NodeID4, authors_full_name));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID1_NodeID4, authors_full_name).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID1_NodeID4, table_column));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID1_NodeID4, table_column).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID1_NodeID5, authors_varchar));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID1_NodeID5, authors_varchar).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID1_NodeID5, table_columnType));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID1_NodeID5, table_columnType).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID2_NodeID2, column_column));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID2_NodeID2, column_column).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID2_NodeID2, aid_authorid));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID2_NodeID2, aid_authorid).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID2_NodeID2, nodeID3_NodeID3));
        Assertions.assertEquals("SQLtype", connectivityGraph.getEdge(nodeID2_NodeID2, nodeID3_NodeID3).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID2_NodeID4, column_column));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID2_NodeID4, column_column).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID2_NodeID4, aid_full_name));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID2_NodeID4, aid_full_name).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID2_NodeID4, nodeID3_NodeID5));
        Assertions.assertEquals("SQLtype", connectivityGraph.getEdge(nodeID2_NodeID4, nodeID3_NodeID5).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID4_NodeID2, column_column));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID4_NodeID2, column_column).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID4_NodeID2, name_authorid));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID4_NodeID2, name_authorid).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID4_NodeID2, nodeID5_NodeID3));
        Assertions.assertEquals("SQLtype", connectivityGraph.getEdge(nodeID4_NodeID2, nodeID5_NodeID3).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID4_NodeID4, column_column));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID4_NodeID4, column_column).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID4_NodeID4, name_full_name));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID4_NodeID4, name_full_name).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID4_NodeID4, nodeID5_NodeID5));
        Assertions.assertEquals("SQLtype", connectivityGraph.getEdge(nodeID4_NodeID4, nodeID5_NodeID5).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID2_NodeID1, column_table));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID2_NodeID1, column_table).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID2_NodeID1, aid_authors));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID2_NodeID1, aid_authors).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID2_NodeID3, column_columnType));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID2_NodeID3, column_columnType).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID2_NodeID3, aid_int));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID2_NodeID3, aid_int).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID2_NodeID5, column_columnType));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID2_NodeID5, column_columnType).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID2_NodeID5, aid_varchar));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID2_NodeID5, aid_varchar).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID3_NodeID3, columnType_columnType));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID3_NodeID3, columnType_columnType).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID3_NodeID3, int_int));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID3_NodeID3, int_int).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID3_NodeID5, columnType_columnType));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID3_NodeID5, columnType_columnType).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID3_NodeID5, int_varchar));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID3_NodeID5, int_varchar).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID3_NodeID1, columnType_table));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID3_NodeID1, columnType_table).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID3_NodeID1, int_authors));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID3_NodeID1, int_authors).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID3_NodeID2, columnType_column));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID3_NodeID2, columnType_column).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID3_NodeID2, int_authorid));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID3_NodeID2, int_authorid).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID3_NodeID4, columnType_column));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID3_NodeID4, columnType_column).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID3_NodeID4, int_full_name));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID3_NodeID4, int_full_name).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID4_NodeID1, column_table));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID4_NodeID1, column_table).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID4_NodeID1, name_authors));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID4_NodeID1, name_authors).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID4_NodeID3, column_columnType));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID4_NodeID3, column_columnType).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID4_NodeID3, name_int));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID4_NodeID3, name_int).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID4_NodeID5, column_columnType));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID4_NodeID5, column_columnType).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID4_NodeID5, name_varchar));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID4_NodeID5, name_varchar).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID5_NodeID3, columnType_columnType));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID5_NodeID3, columnType_columnType).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID5_NodeID3, varchar_int));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID5_NodeID3, varchar_int).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID5_NodeID5, columnType_columnType));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID5_NodeID5, columnType_columnType).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID5_NodeID5, varchar_varchar));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID5_NodeID5, varchar_varchar).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID5_NodeID1, columnType_table));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID5_NodeID1, columnType_table).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID5_NodeID1, varchar_authors));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID5_NodeID1, varchar_authors).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID5_NodeID2, columnType_column));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID5_NodeID2, columnType_column).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID5_NodeID2, varchar_authorid));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID5_NodeID2, varchar_authorid).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID5_NodeID4, columnType_column));
        Assertions.assertEquals("type", connectivityGraph.getEdge(nodeID5_NodeID4, columnType_column).getValue());

        Assertions.assertTrue(connectivityGraph.containsEdge(nodeID5_NodeID4, varchar_full_name));
        Assertions.assertEquals("name", connectivityGraph.getEdge(nodeID5_NodeID4, varchar_full_name).getValue());
    }

    //Test für Inverse Average
    @Test
    public void createPropagationGraphInverseAverageTest() {

        //Should have 59 nodes
        Assertions.assertEquals(59, propagationGraph.vertexSet().size());

        //Should have 116 edges
        Assertions.assertEquals(116, propagationGraph.edgeSet().size());

        Node graph1_table = new Node("Table", null, null, false, false);
        Node graph1_column = new Node("Column", null, null, false, false);
        Node graph1_columnType = new Node("ColumnType", null, null, false, false);
        Node graph1_nodeID1 = new Node("NodeID1", null, null, false, false);
        Node graph1_authors = new Node("authors", null, null, false, false);
        Node graph1_nodeID2 = new Node("NodeID2", null, null, false, false);
        Node graph1_aid = new Node("aid", null, null, false, false);
        Node graph1_nodeID3 = new Node("NodeID3", null, null, false, false);
        Node graph1_integer = new Node("int", null, null, false, false);
        Node graph1_nodeID4 = new Node("NodeID4", null, null, false, false);
        Node graph1_name = new Node("name", null, null, false, false);
        Node graph1_nodeID5 = new Node("NodeID5", null, null, false, false);
        Node graph1_varchar = new Node("varchar", null, null, false, false);

        Node graph2_table = new Node("Table", null, null, false, false);
        Node graph2_column = new Node("Column", null, null, false, false);
        Node graph2_columnType = new Node("ColumnType", null, null, false, false);
        Node graph2_nodeID1 = new Node("NodeID1", null, null, false, false);
        Node graph2_authors = new Node("authors", null, null, false, false);
        Node graph2_nodeID2 = new Node("NodeID2", null, null, false, false);
        Node graph2_authorid = new Node("authorid", null, null, false, false);
        Node graph2_nodeID3 = new Node("NodeID3", null, null, false, false);
        Node graph2_integer = new Node("int", null, null, false, false);
        Node graph2_nodeID4 = new Node("NodeID4", null, null, false, false);
        Node graph2_full_name = new Node("full_name", null, null, false, false);
        Node graph2_nodeID5 = new Node("NodeID5", null, null, false, false);
        Node graph2_varchar = new Node("varchar", null, null, false, false);

        //Testen, ob alle Knoten vorhanden
        NodePair nodeID1_NodeID1 = new NodePair(graph1_nodeID1, graph2_nodeID1);
        NodePair authors_authors = new NodePair(graph1_authors, graph2_authors);
        NodePair nodeID1_NodeID2 = new NodePair(graph1_nodeID1, graph2_nodeID2);
        NodePair authors_authorid = new NodePair(graph1_authors, graph2_authorid);
        NodePair nodeID1_NodeID3 = new NodePair(graph1_nodeID1, graph2_nodeID3);
        NodePair authors_int = new NodePair(graph1_authors, graph2_integer);
        NodePair nodeID1_NodeID4 = new NodePair(graph1_nodeID1, graph2_nodeID4);
        NodePair authors_full_name = new NodePair(graph1_authors, graph2_full_name);
        NodePair nodeID1_NodeID5 = new NodePair(graph1_nodeID1, graph2_nodeID5);
        NodePair authors_varchar = new NodePair(graph1_authors, graph2_varchar);
        NodePair table_table = new NodePair(graph1_table, graph2_table);
        NodePair table_column = new NodePair(graph1_table, graph2_column);
        NodePair table_columnType = new NodePair(graph1_table, graph2_columnType);
        NodePair nodeID2_NodeID2 = new NodePair(graph1_nodeID2, graph2_nodeID2);
        NodePair nodeID2_NodeID4 = new NodePair(graph1_nodeID2, graph2_nodeID4);
        NodePair nodeID4_NodeID2 = new NodePair(graph1_nodeID4, graph2_nodeID2);
        NodePair nodeID4_NodeID4 = new NodePair(graph1_nodeID4, graph2_nodeID4);
        NodePair nodeID2_NodeID1 = new NodePair(graph1_nodeID2, graph2_nodeID1);
        NodePair column_table = new NodePair(graph1_column, graph2_table);
        NodePair column_column = new NodePair(graph1_column, graph2_column);
        NodePair nodeID2_NodeID3 = new NodePair(graph1_nodeID2, graph2_nodeID3);
        NodePair column_columnType = new NodePair(graph1_column, graph2_columnType);
        NodePair nodeID2_NodeID5 = new NodePair(graph1_nodeID2, graph2_nodeID5);
        NodePair aid_authors = new NodePair(graph1_aid, graph2_authors);
        NodePair aid_authorid = new NodePair(graph1_aid, graph2_authorid);
        NodePair aid_int = new NodePair(graph1_aid, graph2_integer);
        NodePair aid_full_name = new NodePair(graph1_aid, graph2_full_name);
        NodePair aid_varchar = new NodePair(graph1_aid, graph2_varchar);
        NodePair nodeID3_NodeID3 = new NodePair(graph1_nodeID3, graph2_nodeID3);
        NodePair nodeID3_NodeID5 = new NodePair(graph1_nodeID3, graph2_nodeID5);
        NodePair nodeID3_NodeID1 = new NodePair(graph1_nodeID3, graph2_nodeID1);
        NodePair columnType_table = new NodePair(graph1_columnType, graph2_table);
        NodePair nodeID3_NodeID2 = new NodePair(graph1_nodeID3, graph2_nodeID2);
        NodePair columnType_column = new NodePair(graph1_columnType, graph2_column);
        NodePair columnType_columnType = new NodePair(graph1_columnType, graph2_columnType);
        NodePair nodeID3_NodeID4 = new NodePair(graph1_nodeID3, graph2_nodeID4);
        NodePair int_authors = new NodePair(graph1_integer, graph2_authors);
        NodePair int_authorid = new NodePair(graph1_integer, graph2_authorid);
        NodePair int_int = new NodePair(graph1_integer, graph2_integer);
        NodePair int_full_name = new NodePair(graph1_integer, graph2_full_name);
        NodePair int_varchar = new NodePair(graph1_integer, graph2_varchar);
        NodePair nodeID4_NodeID1 = new NodePair(graph1_nodeID4, graph2_nodeID1);
        NodePair nodeID4_NodeID3 = new NodePair(graph1_nodeID4, graph2_nodeID3);
        NodePair nodeID4_NodeID5 = new NodePair(graph1_nodeID4, graph2_nodeID5);
        NodePair name_authors = new NodePair(graph1_name, graph2_authors);
        NodePair name_authorid = new NodePair(graph1_name, graph2_authorid);
        NodePair name_int = new NodePair(graph1_name, graph2_integer);
        NodePair name_full_name = new NodePair(graph1_name, graph2_full_name);
        NodePair name_varchar = new NodePair(graph1_name, graph2_varchar);
        NodePair nodeID5_NodeID3 = new NodePair(graph1_nodeID5, graph2_nodeID3);
        NodePair nodeID5_NodeID5 = new NodePair(graph1_nodeID5, graph2_nodeID5);
        NodePair nodeID5_NodeID1 = new NodePair(graph1_nodeID5, graph2_nodeID1);
        NodePair nodeID5_NodeID2 = new NodePair(graph1_nodeID5, graph2_nodeID2);
        NodePair nodeID5_NodeID4 = new NodePair(graph1_nodeID5, graph2_nodeID4);
        NodePair varchar_authors = new NodePair(graph1_varchar, graph2_authors);
        NodePair varchar_authorid = new NodePair(graph1_varchar, graph2_authorid);
        NodePair varchar_int = new NodePair(graph1_varchar, graph2_integer);
        NodePair varchar_full_name = new NodePair(graph1_varchar, graph2_full_name);
        NodePair varchar_varchar = new NodePair(graph1_varchar, graph2_varchar);

        Assertions.assertTrue(propagationGraph.containsVertex(nodeID1_NodeID1));
        Assertions.assertTrue(propagationGraph.containsVertex(authors_authors));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID1_NodeID2));
        Assertions.assertTrue(propagationGraph.containsVertex(authors_authorid));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID1_NodeID3));
        Assertions.assertTrue(propagationGraph.containsVertex(authors_int));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID1_NodeID4));
        Assertions.assertTrue(propagationGraph.containsVertex(authors_full_name));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID1_NodeID5));
        Assertions.assertTrue(propagationGraph.containsVertex(authors_varchar));
        Assertions.assertTrue(propagationGraph.containsVertex(table_table));
        Assertions.assertTrue(propagationGraph.containsVertex(table_column));
        Assertions.assertTrue(propagationGraph.containsVertex(table_columnType));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID2_NodeID2));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID2_NodeID4));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID4_NodeID2));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID4_NodeID4));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID2_NodeID1));
        Assertions.assertTrue(propagationGraph.containsVertex(column_table));
        Assertions.assertTrue(propagationGraph.containsVertex(column_column));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID2_NodeID3));
        Assertions.assertTrue(propagationGraph.containsVertex(column_columnType));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID2_NodeID5));
        Assertions.assertTrue(propagationGraph.containsVertex(aid_authors));
        Assertions.assertTrue(propagationGraph.containsVertex(aid_authorid));
        Assertions.assertTrue(propagationGraph.containsVertex(aid_int));
        Assertions.assertTrue(propagationGraph.containsVertex(aid_full_name));
        Assertions.assertTrue(propagationGraph.containsVertex(aid_varchar));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID3_NodeID3));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID3_NodeID5));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID3_NodeID1));
        Assertions.assertTrue(propagationGraph.containsVertex(columnType_table));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID3_NodeID2));
        Assertions.assertTrue(propagationGraph.containsVertex(columnType_column));
        Assertions.assertTrue(propagationGraph.containsVertex(columnType_columnType));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID3_NodeID4));
        Assertions.assertTrue(propagationGraph.containsVertex(int_authors));
        Assertions.assertTrue(propagationGraph.containsVertex(int_authorid));
        Assertions.assertTrue(propagationGraph.containsVertex(int_int));
        Assertions.assertTrue(propagationGraph.containsVertex(int_full_name));
        Assertions.assertTrue(propagationGraph.containsVertex(int_varchar));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID4_NodeID1));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID4_NodeID3));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID4_NodeID5));
        Assertions.assertTrue(propagationGraph.containsVertex(name_authors));
        Assertions.assertTrue(propagationGraph.containsVertex(name_authorid));
        Assertions.assertTrue(propagationGraph.containsVertex(name_int));
        Assertions.assertTrue(propagationGraph.containsVertex(name_full_name));
        Assertions.assertTrue(propagationGraph.containsVertex(name_varchar));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID5_NodeID3));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID5_NodeID5));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID5_NodeID1));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID5_NodeID2));
        Assertions.assertTrue(propagationGraph.containsVertex(nodeID5_NodeID4));
        Assertions.assertTrue(propagationGraph.containsVertex(varchar_authors));
        Assertions.assertTrue(propagationGraph.containsVertex(varchar_authorid));
        Assertions.assertTrue(propagationGraph.containsVertex(varchar_int));
        Assertions.assertTrue(propagationGraph.containsVertex(varchar_full_name));
        Assertions.assertTrue(propagationGraph.containsVertex(varchar_varchar));

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID1_NodeID1, authors_authors));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID1_NodeID1, authors_authors).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID1_NodeID1, table_table));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID1_NodeID1, table_table).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID1_NodeID1, nodeID2_NodeID2));
        Assertions.assertEquals(0.5, propagationGraph.getEdge(nodeID1_NodeID1, nodeID2_NodeID2).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID1_NodeID1, nodeID2_NodeID4));
        Assertions.assertEquals(0.5, propagationGraph.getEdge(nodeID1_NodeID1, nodeID2_NodeID4).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID1_NodeID1, nodeID4_NodeID2));
        Assertions.assertEquals(0.5, propagationGraph.getEdge(nodeID1_NodeID1, nodeID4_NodeID2).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID1_NodeID1, nodeID4_NodeID4));
        Assertions.assertEquals(0.5, propagationGraph.getEdge(nodeID1_NodeID1, nodeID4_NodeID4).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(authors_authors, nodeID1_NodeID1));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(authors_authors, nodeID1_NodeID1).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID1_NodeID2, authors_authorid));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID1_NodeID2, authors_authorid).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID1_NodeID2, table_column));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID1_NodeID2, table_column).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(authors_authorid, nodeID1_NodeID2));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(authors_authorid, nodeID1_NodeID2).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID1_NodeID3, authors_int));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID1_NodeID3, authors_int).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID1_NodeID3, table_columnType));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID1_NodeID3, table_columnType).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(authors_int, nodeID1_NodeID3));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(authors_int, nodeID1_NodeID3).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID1_NodeID4, authors_full_name));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID1_NodeID4, authors_full_name).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID1_NodeID4, table_column));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID1_NodeID4, table_column).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(authors_full_name, nodeID1_NodeID4));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(authors_full_name, nodeID1_NodeID4).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID1_NodeID5, authors_varchar));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID1_NodeID5, authors_varchar).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID1_NodeID5, table_columnType));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID1_NodeID5, table_columnType).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(authors_varchar, nodeID1_NodeID5));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(authors_varchar, nodeID1_NodeID5).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(table_table, nodeID1_NodeID1));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(table_table, nodeID1_NodeID1).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(table_column, nodeID1_NodeID2));
        Assertions.assertEquals(0.6666666666666666, propagationGraph.getEdge(table_column, nodeID1_NodeID2).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(table_column, nodeID1_NodeID4));
        Assertions.assertEquals(0.6666666666666666, propagationGraph.getEdge(table_column, nodeID1_NodeID4).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(table_columnType, nodeID1_NodeID3));
        Assertions.assertEquals(0.6666666666666666, propagationGraph.getEdge(table_columnType, nodeID1_NodeID3).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(table_columnType, nodeID1_NodeID5));
        Assertions.assertEquals(0.6666666666666666, propagationGraph.getEdge(table_columnType, nodeID1_NodeID5).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID2_NodeID2, nodeID1_NodeID1));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID2_NodeID2, nodeID1_NodeID1).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID2_NodeID2, column_column));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID2_NodeID2, column_column).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID2_NodeID2, aid_authorid));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID2_NodeID2, aid_authorid).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID2_NodeID2, nodeID3_NodeID3));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID2_NodeID2, nodeID3_NodeID3).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID2_NodeID4, nodeID1_NodeID1));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID2_NodeID4, nodeID1_NodeID1).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID2_NodeID4, column_column));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID2_NodeID4, column_column).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID2_NodeID4, aid_full_name));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID2_NodeID4, aid_full_name).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID2_NodeID4, nodeID3_NodeID5));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID2_NodeID4, nodeID3_NodeID5).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID4_NodeID2, nodeID1_NodeID1));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID4_NodeID2, nodeID1_NodeID1).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID4_NodeID2, column_column));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID4_NodeID2, column_column).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID4_NodeID2, name_authorid));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID4_NodeID2, name_authorid).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID4_NodeID2, nodeID5_NodeID3));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID4_NodeID2, nodeID5_NodeID3).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID4_NodeID4, nodeID1_NodeID1));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID4_NodeID4, nodeID1_NodeID1).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID4_NodeID4, column_column));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID4_NodeID4, column_column).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID4_NodeID4, name_full_name));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID4_NodeID4, name_full_name).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID4_NodeID4, nodeID5_NodeID5));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID4_NodeID4, nodeID5_NodeID5).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID2_NodeID1, column_table));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID2_NodeID1, column_table).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID2_NodeID1, aid_authors));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID2_NodeID1, aid_authors).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(column_table, nodeID2_NodeID1));
        Assertions.assertEquals(0.6666666666666666, propagationGraph.getEdge(column_table, nodeID2_NodeID1).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(column_table, nodeID4_NodeID1));
        Assertions.assertEquals(0.6666666666666666, propagationGraph.getEdge(column_table, nodeID4_NodeID1).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(column_column, nodeID2_NodeID2));
        Assertions.assertEquals(0.5, propagationGraph.getEdge(column_column, nodeID2_NodeID2).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(column_column, nodeID2_NodeID4));
        Assertions.assertEquals(0.5, propagationGraph.getEdge(column_column, nodeID2_NodeID4).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(column_column, nodeID4_NodeID2));
        Assertions.assertEquals(0.5, propagationGraph.getEdge(column_column, nodeID4_NodeID2).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(column_column, nodeID4_NodeID4));
        Assertions.assertEquals(0.5, propagationGraph.getEdge(column_column, nodeID4_NodeID4).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID2_NodeID3, column_columnType));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID2_NodeID3, column_columnType).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID2_NodeID3, aid_int));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID2_NodeID3, aid_int).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(column_columnType, nodeID2_NodeID3));
        Assertions.assertEquals(0.5, propagationGraph.getEdge(column_columnType, nodeID2_NodeID3).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(column_columnType, nodeID2_NodeID5));
        Assertions.assertEquals(0.5, propagationGraph.getEdge(column_columnType, nodeID2_NodeID5).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(column_columnType, nodeID4_NodeID3));
        Assertions.assertEquals(0.5, propagationGraph.getEdge(column_columnType, nodeID4_NodeID3).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(column_columnType, nodeID4_NodeID5));
        Assertions.assertEquals(0.5, propagationGraph.getEdge(column_columnType, nodeID4_NodeID5).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID2_NodeID5, column_columnType));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID2_NodeID5, column_columnType).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID2_NodeID5, aid_varchar));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID2_NodeID5, aid_varchar).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(aid_authors, nodeID2_NodeID1));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(aid_authors, nodeID2_NodeID1).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(aid_authorid, nodeID2_NodeID2));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(aid_authorid, nodeID2_NodeID2).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(aid_int, nodeID2_NodeID3));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(aid_int, nodeID2_NodeID3).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(aid_full_name, nodeID2_NodeID4));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(aid_full_name, nodeID2_NodeID4).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(aid_varchar, nodeID2_NodeID5));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(aid_varchar, nodeID2_NodeID5).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID3_NodeID3, nodeID2_NodeID2));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID3_NodeID3, nodeID2_NodeID2).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID3_NodeID3, columnType_columnType));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID3_NodeID3, columnType_columnType).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID3_NodeID3, int_int));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID3_NodeID3, int_int).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID3_NodeID5, nodeID2_NodeID4));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID3_NodeID5, nodeID2_NodeID4).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID3_NodeID5, columnType_columnType));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID3_NodeID5, columnType_columnType).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID3_NodeID5, int_varchar));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID3_NodeID5, int_varchar).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID3_NodeID1, columnType_table));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID3_NodeID1, columnType_table).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID3_NodeID1, int_authors));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID3_NodeID1, int_authors).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(columnType_table, nodeID3_NodeID1));
        Assertions.assertEquals(0.6666666666666666, propagationGraph.getEdge(columnType_table, nodeID3_NodeID1).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(columnType_table, nodeID5_NodeID1));
        Assertions.assertEquals(0.6666666666666666, propagationGraph.getEdge(columnType_table, nodeID5_NodeID1).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID3_NodeID2, columnType_column));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID3_NodeID2, columnType_column).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID3_NodeID2, int_authorid));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID3_NodeID2, int_authorid).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(columnType_column, nodeID3_NodeID2));
        Assertions.assertEquals(0.5, propagationGraph.getEdge(columnType_column, nodeID3_NodeID2).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(columnType_column, nodeID3_NodeID4));
        Assertions.assertEquals(0.5, propagationGraph.getEdge(columnType_column, nodeID3_NodeID4).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(columnType_column, nodeID5_NodeID2));
        Assertions.assertEquals(0.5, propagationGraph.getEdge(columnType_column, nodeID5_NodeID2).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(columnType_column, nodeID5_NodeID4));
        Assertions.assertEquals(0.5, propagationGraph.getEdge(columnType_column, nodeID5_NodeID4).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(columnType_columnType, nodeID3_NodeID3));
        Assertions.assertEquals(0.5, propagationGraph.getEdge(columnType_columnType, nodeID3_NodeID3).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(columnType_columnType, nodeID3_NodeID5));
        Assertions.assertEquals(0.5, propagationGraph.getEdge(columnType_columnType, nodeID3_NodeID5).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(columnType_columnType, nodeID5_NodeID3));
        Assertions.assertEquals(0.5, propagationGraph.getEdge(columnType_columnType, nodeID5_NodeID3).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(columnType_columnType, nodeID5_NodeID5));
        Assertions.assertEquals(0.5, propagationGraph.getEdge(columnType_columnType, nodeID5_NodeID5).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID3_NodeID4, columnType_column));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID3_NodeID4, columnType_column).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID3_NodeID4, int_full_name));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID3_NodeID4, int_full_name).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(int_authors, nodeID3_NodeID1));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(int_authors, nodeID3_NodeID1).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(int_authorid, nodeID3_NodeID2));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(int_authorid, nodeID3_NodeID2).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(int_int, nodeID3_NodeID3));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(int_int, nodeID3_NodeID3).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(int_full_name, nodeID3_NodeID4));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(int_full_name, nodeID3_NodeID4).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(int_varchar, nodeID3_NodeID5));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(int_varchar, nodeID3_NodeID5).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID4_NodeID1, column_table));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID4_NodeID1, column_table).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID4_NodeID1, name_authors));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID4_NodeID1, name_authors).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID4_NodeID3, column_columnType));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID4_NodeID3, column_columnType).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID4_NodeID3, name_int));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID4_NodeID3, name_int).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID4_NodeID5, column_columnType));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID4_NodeID5, column_columnType).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID4_NodeID5, name_varchar));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID4_NodeID5, name_varchar).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(name_authors, nodeID4_NodeID1));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(name_authors, nodeID4_NodeID1).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(name_authorid, nodeID4_NodeID2));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(name_authorid, nodeID4_NodeID2).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(name_int, nodeID4_NodeID3));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(name_int, nodeID4_NodeID3).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(name_full_name, nodeID4_NodeID4));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(name_full_name, nodeID4_NodeID4).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(name_varchar, nodeID4_NodeID5));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(name_varchar, nodeID4_NodeID5).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID5_NodeID3, nodeID4_NodeID2));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID5_NodeID3, nodeID4_NodeID2).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID5_NodeID3, columnType_columnType));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID5_NodeID3, columnType_columnType).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID5_NodeID3, varchar_int));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID5_NodeID3, varchar_int).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID5_NodeID5, nodeID4_NodeID4));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID5_NodeID5, nodeID4_NodeID4).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID5_NodeID5, columnType_columnType));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID5_NodeID5, columnType_columnType).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID5_NodeID5, varchar_varchar));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID5_NodeID5, varchar_varchar).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID5_NodeID1, columnType_table));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID5_NodeID1, columnType_table).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID5_NodeID1, varchar_authors));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID5_NodeID1, varchar_authors).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID5_NodeID2, columnType_column));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID5_NodeID2, columnType_column).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID5_NodeID2, varchar_authorid));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID5_NodeID2, varchar_authorid).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID5_NodeID4, columnType_column));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID5_NodeID4, columnType_column).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(nodeID5_NodeID4, varchar_full_name));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(nodeID5_NodeID4, varchar_full_name).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(varchar_authors, nodeID5_NodeID1));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(varchar_authors, nodeID5_NodeID1).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(varchar_authorid, nodeID5_NodeID2));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(varchar_authorid, nodeID5_NodeID2).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(varchar_int, nodeID5_NodeID3));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(varchar_int, nodeID5_NodeID3).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(varchar_full_name, nodeID5_NodeID4));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(varchar_full_name, nodeID5_NodeID4).getCoefficient());

        Assertions.assertTrue(propagationGraph.containsEdge(varchar_varchar, nodeID5_NodeID5));
        Assertions.assertEquals(1.0, propagationGraph.getEdge(varchar_varchar, nodeID5_NodeID5).getCoefficient());

    }

    @Test
    public void initialMappingValentineTest() {

        Map<NodePair, Double> valentineInitialMapping = new HashMap<>();

        Node graph1_table = new Node("Table", null, null, false, false);
        Node graph1_column = new Node("Column", null, null, false, false);
        Node graph1_columnType = new Node("ColumnType", null, null, false, false);
        Node graph1_nodeID1 = new Node("NodeID1", null, null, false, false);
        Node graph1_authors = new Node("authors", null, null, false, false);
        Node graph1_nodeID2 = new Node("NodeID2", null, null, false, false);
        Node graph1_aid = new Node("aid", null, null, false, false);
        Node graph1_nodeID3 = new Node("NodeID3", null, null, false, false);
        Node graph1_integer = new Node("int", null, null, false, false);
        Node graph1_nodeID4 = new Node("NodeID4", null, null, false, false);
        Node graph1_name = new Node("name", null, null, false, false);
        Node graph1_nodeID5 = new Node("NodeID5", null, null, false, false);
        Node graph1_varchar = new Node("varchar", null, null, false, false);

        Node graph2_table = new Node("Table", null, null, false, false);
        Node graph2_column = new Node("Column", null, null, false, false);
        Node graph2_columnType = new Node("ColumnType", null, null, false, false);
        Node graph2_nodeID1 = new Node("NodeID1", null, null, false, false);
        Node graph2_authors = new Node("authors", null, null, false, false);
        Node graph2_nodeID2 = new Node("NodeID2", null, null, false, false);
        Node graph2_authorid = new Node("authorid", null, null, false, false);
        Node graph2_nodeID3 = new Node("NodeID3", null, null, false, false);
        Node graph2_integer = new Node("int", null, null, false, false);
        Node graph2_nodeID4 = new Node("NodeID4", null, null, false, false);
        Node graph2_full_name = new Node("full_name", null, null, false, false);
        Node graph2_nodeID5 = new Node("NodeID5", null, null, false, false);
        Node graph2_varchar = new Node("varchar", null, null, false, false);

        NodePair nodeID1_NodeID1 = new NodePair(graph1_nodeID1, graph2_nodeID1);
        NodePair authors_authors = new NodePair(graph1_authors, graph2_authors);
        NodePair nodeID1_NodeID2 = new NodePair(graph1_nodeID1, graph2_nodeID2);
        NodePair authors_authorid = new NodePair(graph1_authors, graph2_authorid);
        NodePair nodeID1_NodeID3 = new NodePair(graph1_nodeID1, graph2_nodeID3);
        NodePair authors_int = new NodePair(graph1_authors, graph2_integer);
        NodePair nodeID1_NodeID4 = new NodePair(graph1_nodeID1, graph2_nodeID4);
        NodePair authors_full_name = new NodePair(graph1_authors, graph2_full_name);
        NodePair nodeID1_NodeID5 = new NodePair(graph1_nodeID1, graph2_nodeID5);
        NodePair authors_varchar = new NodePair(graph1_authors, graph2_varchar);
        NodePair table_table = new NodePair(graph1_table, graph2_table);
        NodePair table_column = new NodePair(graph1_table, graph2_column);
        NodePair table_columnType = new NodePair(graph1_table, graph2_columnType);
        NodePair nodeID2_NodeID2 = new NodePair(graph1_nodeID2, graph2_nodeID2);
        NodePair nodeID2_NodeID4 = new NodePair(graph1_nodeID2, graph2_nodeID4);
        NodePair nodeID4_NodeID2 = new NodePair(graph1_nodeID4, graph2_nodeID2);
        NodePair nodeID4_NodeID4 = new NodePair(graph1_nodeID4, graph2_nodeID4);
        NodePair nodeID2_NodeID1 = new NodePair(graph1_nodeID2, graph2_nodeID1);
        NodePair column_table = new NodePair(graph1_column, graph2_table);
        NodePair column_column = new NodePair(graph1_column, graph2_column);
        NodePair nodeID2_NodeID3 = new NodePair(graph1_nodeID2, graph2_nodeID3);
        NodePair column_columnType = new NodePair(graph1_column, graph2_columnType);
        NodePair nodeID2_NodeID5 = new NodePair(graph1_nodeID2, graph2_nodeID5);
        NodePair aid_authors = new NodePair(graph1_aid, graph2_authors);
        NodePair aid_authorid = new NodePair(graph1_aid, graph2_authorid);
        NodePair aid_int = new NodePair(graph1_aid, graph2_integer);
        NodePair aid_full_name = new NodePair(graph1_aid, graph2_full_name);
        NodePair aid_varchar = new NodePair(graph1_aid, graph2_varchar);
        NodePair nodeID3_NodeID3 = new NodePair(graph1_nodeID3, graph2_nodeID3);
        NodePair nodeID3_NodeID5 = new NodePair(graph1_nodeID3, graph2_nodeID5);
        NodePair nodeID3_NodeID1 = new NodePair(graph1_nodeID3, graph2_nodeID1);
        NodePair columnType_table = new NodePair(graph1_columnType, graph2_table);
        NodePair nodeID3_NodeID2 = new NodePair(graph1_nodeID3, graph2_nodeID2);
        NodePair columnType_column = new NodePair(graph1_columnType, graph2_column);
        NodePair columnType_columnType = new NodePair(graph1_columnType, graph2_columnType);
        NodePair nodeID3_NodeID4 = new NodePair(graph1_nodeID3, graph2_nodeID4);
        NodePair int_authors = new NodePair(graph1_integer, graph2_authors);
        NodePair int_authorid = new NodePair(graph1_integer, graph2_authorid);
        NodePair int_int = new NodePair(graph1_integer, graph2_integer);
        NodePair int_full_name = new NodePair(graph1_integer, graph2_full_name);
        NodePair int_varchar = new NodePair(graph1_integer, graph2_varchar);
        NodePair nodeID4_NodeID1 = new NodePair(graph1_nodeID4, graph2_nodeID1);
        NodePair nodeID4_NodeID3 = new NodePair(graph1_nodeID4, graph2_nodeID3);
        NodePair nodeID4_NodeID5 = new NodePair(graph1_nodeID4, graph2_nodeID5);
        NodePair name_authors = new NodePair(graph1_name, graph2_authors);
        NodePair name_authorid = new NodePair(graph1_name, graph2_authorid);
        NodePair name_int = new NodePair(graph1_name, graph2_integer);
        NodePair name_full_name = new NodePair(graph1_name, graph2_full_name);
        NodePair name_varchar = new NodePair(graph1_name, graph2_varchar);
        NodePair nodeID5_NodeID3 = new NodePair(graph1_nodeID5, graph2_nodeID3);
        NodePair nodeID5_NodeID5 = new NodePair(graph1_nodeID5, graph2_nodeID5);
        NodePair nodeID5_NodeID1 = new NodePair(graph1_nodeID5, graph2_nodeID1);
        NodePair nodeID5_NodeID2 = new NodePair(graph1_nodeID5, graph2_nodeID2);
        NodePair nodeID5_NodeID4 = new NodePair(graph1_nodeID5, graph2_nodeID4);
        NodePair varchar_authors = new NodePair(graph1_varchar, graph2_authors);
        NodePair varchar_authorid = new NodePair(graph1_varchar, graph2_authorid);
        NodePair varchar_int = new NodePair(graph1_varchar, graph2_integer);
        NodePair varchar_full_name = new NodePair(graph1_varchar, graph2_full_name);
        NodePair varchar_varchar = new NodePair(graph1_varchar, graph2_varchar);

        //Beobachtung: Initale Similarity nur für ähnliche Knoten berechnet, deswegen muss nicht ganze SimMatrix bestimmt werden
        //TODO: Valentine berechnet für alles Similarities nicht nur für die Knotenpaare im PropagationGraph

        valentineInitialMapping.put(table_table, 1.0);
        valentineInitialMapping.put(table_column, 0.0);
        valentineInitialMapping.put(table_columnType, 0.09999999999999998);
        valentineInitialMapping.put(column_table, 0.0);
        valentineInitialMapping.put(column_column, 1.0);
        valentineInitialMapping.put(column_columnType, 0.6);
        valentineInitialMapping.put(columnType_table, 0.09999999999999998);
        valentineInitialMapping.put(columnType_column, 0.6);
        valentineInitialMapping.put(columnType_columnType, 1.0);
        valentineInitialMapping.put(nodeID1_NodeID1, 0.0);
        valentineInitialMapping.put(nodeID1_NodeID2, 0.0);
        valentineInitialMapping.put(nodeID1_NodeID3, 0.0);
        valentineInitialMapping.put(nodeID1_NodeID4, 0.0);
        valentineInitialMapping.put(nodeID1_NodeID5, 0.0);
        valentineInitialMapping.put(authors_authors, 1.0);
        valentineInitialMapping.put(authors_authorid, 0.75);
        valentineInitialMapping.put(authors_int, 0.1428571428571429);
        valentineInitialMapping.put(authors_full_name, 0.11111111111111116);
        valentineInitialMapping.put(authors_varchar, 0.2857142857142857);
        valentineInitialMapping.put(nodeID2_NodeID1, 0.0);
        valentineInitialMapping.put(nodeID2_NodeID2, 0.0);
        valentineInitialMapping.put(nodeID2_NodeID3, 0.0);
        valentineInitialMapping.put(aid_authors, 0.1428571428571429);
        valentineInitialMapping.put(aid_authorid, 0.375);
        valentineInitialMapping.put(aid_int, 0.0);
        valentineInitialMapping.put(aid_full_name, 0.11111111111111116);
        valentineInitialMapping.put(aid_varchar, 0.1428571428571429);
        valentineInitialMapping.put(nodeID3_NodeID1, 0.0);
        valentineInitialMapping.put(nodeID3_NodeID2, 0.0);
        valentineInitialMapping.put(nodeID3_NodeID3, 0.0);
        valentineInitialMapping.put(nodeID3_NodeID4, 0.0);
        valentineInitialMapping.put(nodeID3_NodeID5, 0.0);
        valentineInitialMapping.put(int_authors, 0.1428571428571429);
        valentineInitialMapping.put(int_authorid, 0.125);
        valentineInitialMapping.put(int_int, 1.0);
        valentineInitialMapping.put(int_full_name, 0.11111111111111116);
        valentineInitialMapping.put(int_varchar, 0.0);
        valentineInitialMapping.put(nodeID4_NodeID1, 0.0);
        valentineInitialMapping.put(nodeID4_NodeID2, 0.0);
        valentineInitialMapping.put(nodeID4_NodeID3, 0.0);
        valentineInitialMapping.put(nodeID4_NodeID4, 0.0);
        valentineInitialMapping.put(nodeID4_NodeID5, 0.0);
        valentineInitialMapping.put(name_authors, 0.0);
        valentineInitialMapping.put(name_authorid, 0.0);
        valentineInitialMapping.put(name_int, 0.0);
        valentineInitialMapping.put(name_full_name, 0.4444444444444444);
        valentineInitialMapping.put(name_varchar, 0.1428571428571429);
        valentineInitialMapping.put(nodeID5_NodeID1, 0.0);
        valentineInitialMapping.put(nodeID5_NodeID2, 0.0);
        valentineInitialMapping.put(nodeID5_NodeID3, 0.0);
        valentineInitialMapping.put(nodeID5_NodeID4, 0.0);
        valentineInitialMapping.put(nodeID5_NodeID5, 0.0);
        valentineInitialMapping.put(varchar_authors, 0.2857142857142857);
        valentineInitialMapping.put(varchar_authorid, 0.25);
        valentineInitialMapping.put(varchar_int, 0.0);
        valentineInitialMapping.put(varchar_full_name, 0.11111111111111116);
        valentineInitialMapping.put(varchar_varchar, 1.0);
        valentineInitialMapping.put(nodeID2_NodeID5, 0.0);
        valentineInitialMapping.put(nodeID2_NodeID4, 0.0);

        Assertions.assertEquals(valentineInitialMapping.size(), initialMapping.size());

        for(NodePair key : initialMapping.keySet()) {
            try{
                Assertions.assertEquals(initialMapping.get(key), valentineInitialMapping.get(key), 1e-7); //Nur auf 7 Nachkommastellen genau
            } catch(Exception e) {
                System.out.println(key);
            }
        }
    }

}
