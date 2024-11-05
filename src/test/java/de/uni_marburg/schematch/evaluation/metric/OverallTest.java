package de.uni_marburg.schematch.evaluation.metric;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OverallTest {

    @Test
    void run() {

        Overall overall = new Overall();

        int[] groundTruthVector1 = {1, 1, 0, 0};
        float[] simVector1 = {1, 0, 1, 1};
        assertEquals(-0.5f, overall.run(groundTruthVector1, simVector1));

        int[] groundTruthVector2 = {1, 1, 1, 1};
        float[] simVector2 = {1, 1, 1, 1};
        assertEquals(1.0f, overall.run(groundTruthVector2, simVector2));

        int[] groundTruthVector3 = {1, 1, 1, 1};
        float[] simVector3 = {0, 0, 0, 0};
        assertEquals(0.0f, overall.run(groundTruthVector3, simVector3));

        int[] groundTruthVector4 = {0, 0, 0, 0};
        float[] simVector4 = {1, 1, 0, 0};
        assertEquals(0.0, overall.run(groundTruthVector4, simVector4));

    }
}