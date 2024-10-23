package de.uni_marburg.schematch.matching.similarityFlooding;

import de.uni_marburg.schematch.TestUtils;
import de.uni_marburg.schematch.data.Database;
import de.uni_marburg.schematch.data.Scenario;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

class GraphTransformationTest {

    //Database sourceDb;
    //Database targetDb;
    SimilarityFlooding similarityFlooding = new SimilarityFlooding();
    //Graph<Node, LabelEdge> graph1;
    //Graph<Node, LabelEdge> graph2;
    //Graph<NodePair, LabelEdge> connectivityGraph;
    //Graph<NodePair, CoefficientEdge> propagationGraph;
    //PropagationCoefficientPolicy propagationCoefficientPolicy = PropagationCoefficientPolicy.INVERSE_AVERAGE;
    //Map<NodePair, Double> initialMapping;
    //FixpointFormula fixpointFormula;

    @BeforeEach
    public void init() {




    }

    @Test
    public void transformIntoGraphRepresentation() {



    }

    @Test
    public void createConnectivityGraph() {

        //Build Graph Model A
        Graph<Node, LabelEdge> modelA = new DefaultDirectedGraph<>(LabelEdge.class);
        Node a = new Node("a", null, null, false, null, null);
        Node a1 = new Node("a1", null, null, false, null, null);
        Node a2 = new Node("a2", null, null, false, null, null);

        modelA.addVertex(a);
        modelA.addVertex(a1);
        modelA.addVertex(a2);

        modelA.addEdge(a, a1, new LabelEdge("l1"));
        modelA.addEdge(a, a2, new LabelEdge("l1"));
        modelA.addEdge(a1, a2, new LabelEdge("l2"));

        //Build Graph Model B
        Graph<Node, LabelEdge> modelB = new DefaultDirectedGraph<>(LabelEdge.class);
        Node b = new Node("b", null, null, false, null, null);
        Node b1 = new Node("b1", null, null, false, null, null);
        Node b2 = new Node("b2", null, null, false, null, null);

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
        Node a = new Node("a", null, null, false, null, null);
        Node a1 = new Node("a1", null, null, false, null, null);
        Node a2 = new Node("a2", null, null, false, null, null);

        modelA.addVertex(a);
        modelA.addVertex(a1);
        modelA.addVertex(a2);

        modelA.addEdge(a, a1, new LabelEdge("l1"));
        modelA.addEdge(a, a2, new LabelEdge("l1"));
        modelA.addEdge(a1, a2, new LabelEdge("l2"));

        //Build Graph Model B
        Graph<Node, LabelEdge> modelB = new DefaultDirectedGraph<>(LabelEdge.class);
        Node b = new Node("b", null, null, false, null, null);
        Node b1 = new Node("b1", null, null, false, null, null);
        Node b2 = new Node("b2", null, null, false, null, null);

        modelB.addVertex(b);
        modelB.addVertex(b1);
        modelB.addVertex(b2);

        modelB.addEdge(b, b1, new LabelEdge("l1"));
        modelB.addEdge(b, b2, new LabelEdge("l2"));
        modelB.addEdge(b2, b1, new LabelEdge("l2"));

        Graph<NodePair, LabelEdge> connectivityGraph = similarityFlooding.createConnectivityGraph(modelA, modelB);

        //Test for PropagationGraph constructed with the InverseAverage policy
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
        Assertions.assertEquals(new CoefficientEdge(1.0), propagationGraphInverseProduct.getEdge(a1b2, a2b1)); //Expected: 1.0, Actual 0.5
        Assertions.assertEquals(new CoefficientEdge(1.0), propagationGraphInverseProduct.getEdge(a2b1, a1b2));

        Assertions.assertTrue(propagationGraphInverseProduct.containsEdge(a1b, a2b2));
        Assertions.assertTrue(propagationGraphInverseProduct.containsEdge(a2b2, a1b));
        Assertions.assertEquals(new CoefficientEdge(1.0), propagationGraphInverseProduct.getEdge(a1b, a2b2));
        Assertions.assertEquals(new CoefficientEdge(1.0), propagationGraphInverseProduct.getEdge(a2b2, a1b)); //Expected: 1.0, Actual 0.5

        //TODO: Check if example on paper is correct
        //TODO: Calculate (by Hand) propagation graph for inverse average policy

        //Graph<NodePair, CoefficientEdge> propagationGraphInverseProduct = similarityFlooding.inducePropagationGraph(connectivityGraph, modelA, modelB, PropagationCoefficientPolicy.INVERSE_PRODUCT);

    }


}
