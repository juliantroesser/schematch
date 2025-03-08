package de.uni_marburg.schematch.matching.similarityFlooding;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class SimilarityFloodingTest {

//    @Test
//    public void calculateInitialMappingTest() {
//
//        TestUtils.TestData testData = TestUtils.getTestData();
//        Scenario scenario = new Scenario(testData.getScenarios().get("test1").getPath());
//        Database sourceDb = scenario.getSourceDatabase();
//        Database targetDb = scenario.getTargetDatabase();
//        PropagationCoefficientPolicy policy = PropagationCoefficientPolicy.INVERSE_AVERAGE; //Policy does not matter for this test
//
//        //vanilla, fdv2, uccv1, uccv2, indv2 testen
//
//        //Vanilla Mapping
//        Graph<Node, LabelEdge> graphRepresentationSourceDB = similarityFlooding.transformIntoGraphRepresentation(sourceDb, false, false, false, false, false, false);
//        Graph<Node, LabelEdge> graphRepresentationTargetDB = similarityFlooding.transformIntoGraphRepresentation(targetDb, false, false, false, false, false, false);
//        Graph<NodePair, LabelEdge> connectivityGraph = similarityFlooding.createConnectivityGraph(graphRepresentationSourceDB, graphRepresentationTargetDB);
//        Graph<NodePair, CoefficientEdge> propagationGraph = similarityFlooding.inducePropagationGraph(connectivityGraph, graphRepresentationSourceDB, graphRepresentationTargetDB, policy);
//        Map<NodePair, Double> initialMapping = similarityFlooding.calculateInitialMapping(propagationGraph);
//
//        Assertions.assertEquals(88, initialMapping.size());
//
//        //fdv2 Mapping
//        Graph<Node, LabelEdge> graphRepresentationSourceDBFDV2 = similarityFlooding.transformIntoGraphRepresentation(sourceDb, false, true, false, false, false, false);
//        Graph<Node, LabelEdge> graphRepresentationTargetDBFDV2 = similarityFlooding.transformIntoGraphRepresentation(targetDb, false, true, false, false, false, false);
//        Graph<NodePair, LabelEdge> connectivityGraphFDV2 = similarityFlooding.createConnectivityGraph(graphRepresentationSourceDBFDV2, graphRepresentationTargetDBFDV2);
//        Graph<NodePair, CoefficientEdge> propagationGraphFDV2 = similarityFlooding.inducePropagationGraph(connectivityGraphFDV2, graphRepresentationSourceDBFDV2, graphRepresentationTargetDBFDV2, policy);
//        Map<NodePair, Double> initialMappingFDV2 = similarityFlooding.calculateInitialMapping(propagationGraphFDV2);
//
//        initialMappingFDV2.entrySet().removeAll(initialMapping.entrySet());
//
//        for(Map.Entry<NodePair, Double> entry : initialMappingFDV2.entrySet()) {
//            System.out.println(entry);
//        }
//    }

    @Test
    public void similarityFloodingTest() {

        SimilarityFlooding similarityFlooding = new SimilarityFlooding();

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

        PropagationCoefficientPolicy policy = PropagationCoefficientPolicy.INVERSE_PRODUCT;
        Graph<NodePair, LabelEdge> connectivityGraph = similarityFlooding.createConnectivityGraph(modelA, modelB);
        Graph<NodePair, CoefficientEdge> propagationGraph = similarityFlooding.inducePropagationGraph(connectivityGraph, modelA, modelB, policy);

        Map<NodePair, Double> initialMapping = new HashMap<>();
        initialMapping.put(new NodePair(a, b), 1.0);
        initialMapping.put(new NodePair(a1, b1), 1.0);
        initialMapping.put(new NodePair(a2, b1), 1.0);
        initialMapping.put(new NodePair(a1, b2), 1.0);
        initialMapping.put(new NodePair(a1, b), 1.0);
        initialMapping.put(new NodePair(a2, b2), 1.0);

//        similarityFlooding.setMaxIter("5");
//        similarityFlooding("0.0001");
        Map<NodePair, Double> mappingAfterFiveIterations = similarityFlooding.similarityFlooding(propagationGraph, initialMapping, FixpointFormula.BASIC);

        Assertions.assertEquals(1.0, (double) Math.round(mappingAfterFiveIterations.get(new NodePair(a, b)) * 100) / 100);
        Assertions.assertEquals(0.91, (double) Math.round(mappingAfterFiveIterations.get(new NodePair(a2, b1)) * 100) / 100);
        Assertions.assertEquals(0.69, (double) Math.round(mappingAfterFiveIterations.get(new NodePair(a1, b2)) * 100) / 100);
        Assertions.assertEquals(0.39, (double) Math.round(mappingAfterFiveIterations.get(new NodePair(a1, b1)) * 100) / 100);
        Assertions.assertEquals(0.33, (double) Math.round(mappingAfterFiveIterations.get(new NodePair(a1, b)) * 100) / 100);
        Assertions.assertEquals(0.33, (double) Math.round(mappingAfterFiveIterations.get(new NodePair(a2, b2)) * 100) / 100);
    }



}