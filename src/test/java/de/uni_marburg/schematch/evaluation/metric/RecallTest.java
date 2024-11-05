package de.uni_marburg.schematch.evaluation.metric;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecallTest {

    @Test
    void run() {

        Recall recall = new Recall();

        int[] groundTruthVector1 = {1, 1, 0, 0};
        float[] simVector1 = {1, 0, 0, 1};
        Assertions.assertEquals(1f/2f, recall.run(groundTruthVector1, simVector1));

        int[] groundTruthVector2 = {0, 1, 0, 1};
        float[] simVector2 = {1, 0, 1, 0};
        Assertions.assertEquals(0f, recall.run(groundTruthVector2, simVector2));

        int[] groundTruthVector3 = {1, 2, 1, 0};
        float[] simVector3 = {1, 1, -1, 0};
        Assertions.assertThrows(IllegalArgumentException.class, () -> recall.run(groundTruthVector3, simVector3));

        int[] groundTruthVector4 = {0, 0, 0, 0};
        float[] simVector4 = {1, 1, 1, 1};
        Assertions.assertEquals(0f, recall.run(groundTruthVector4, simVector4));
    }
}