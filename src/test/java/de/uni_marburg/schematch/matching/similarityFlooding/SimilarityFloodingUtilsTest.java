package de.uni_marburg.schematch.matching.similarityFlooding;

import de.uni_marburg.schematch.TestUtils;
import de.uni_marburg.schematch.data.Database;
import de.uni_marburg.schematch.data.Scenario;
import de.uni_marburg.schematch.data.Table;
import de.uni_marburg.schematch.matchtask.MatchTask;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static de.uni_marburg.schematch.matching.similarityFlooding.SimilarityFloodingUtils.calcResidualVector;
import static de.uni_marburg.schematch.matching.similarityFlooding.SimilarityFloodingUtils.hasConverged;
import static org.junit.jupiter.api.Assertions.*;

class SimilarityFloodingUtilsTest {

    @Test
    public void calcResidualVectorTest() {
        Node a = new Node("a", null, null, false, null, null, null);
        Node a1 = new Node("a1", null, null, false, null, null, null);
        Node a2 = new Node("a2", null, null, false, null, null, null);

        Node b = new Node("b", null, null, false, null, null, null);
        Node b1 = new Node("b1", null, null, false, null, null, null);
        Node b2 = new Node("b2", null, null, false, null, null, null);

        Map<NodePair, Double> sigma_i = new HashMap<>();
        sigma_i.put(new NodePair(a, b), 1.0);
        sigma_i.put(new NodePair(a1, b1), 1.0);
        sigma_i.put(new NodePair(a2, b1), 1.0);
        sigma_i.put(new NodePair(a1, b2), 1.0);
        sigma_i.put(new NodePair(a1, b), 1.0);
        sigma_i.put(new NodePair(a2, b2), 1.0);

        Map<NodePair, Double> sigma_i_plus_1 = new HashMap<>();
        sigma_i_plus_1.put(new NodePair(a, b), 1.0);
        sigma_i_plus_1.put(new NodePair(a1, b1), 0.91);
        sigma_i_plus_1.put(new NodePair(a2, b1), 0.69);
        sigma_i_plus_1.put(new NodePair(a1, b2), 0.39);
        sigma_i_plus_1.put(new NodePair(a1, b), 0.33);
        sigma_i_plus_1.put(new NodePair(a2, b2), 0.33);

        Assertions.assertEquals(1.172220115848555, calcResidualVector(sigma_i, sigma_i_plus_1), 1e-10);
    }

    @Test
    public void hasConvergedTest() {
        Node a = new Node("a", null, null, false, null, null, null);
        Node a1 = new Node("a1", null, null, false, null, null, null);
        Node a2 = new Node("a2", null, null, false, null, null, null);

        Node b = new Node("b", null, null, false, null, null, null);
        Node b1 = new Node("b1", null, null, false, null, null, null);
        Node b2 = new Node("b2", null, null, false, null, null, null);

        Map<NodePair, Double> sigma_i = new HashMap<>();
        sigma_i.put(new NodePair(a, b), 1.0);
        sigma_i.put(new NodePair(a1, b1), 1.0);
        sigma_i.put(new NodePair(a2, b1), 1.0);
        sigma_i.put(new NodePair(a1, b2), 1.0);
        sigma_i.put(new NodePair(a1, b), 1.0);
        sigma_i.put(new NodePair(a2, b2), 1.0);

        Map<NodePair, Double> sigma_i_plus_1 = new HashMap<>();
        sigma_i_plus_1.put(new NodePair(a, b), 1.0);
        sigma_i_plus_1.put(new NodePair(a1, b1), 0.91);
        sigma_i_plus_1.put(new NodePair(a2, b1), 0.69);
        sigma_i_plus_1.put(new NodePair(a1, b2), 0.39);
        sigma_i_plus_1.put(new NodePair(a1, b), 0.33);
        sigma_i_plus_1.put(new NodePair(a2, b2), 0.33);

        Assertions.assertFalse(hasConverged(sigma_i, sigma_i_plus_1, 1.1));
        Assertions.assertTrue(hasConverged(sigma_i, sigma_i_plus_1, 1.2));
    }
}