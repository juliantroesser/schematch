package de.uni_marburg.schematch.evaluation.metric;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class F1ScoreTest {

    @Test
    void run() {

        F1Score f1score = new F1Score();

        int[] groundTruthVector1 = {1, 1, 0, 0};
        float[] simVector1 = {1, 0, 1, 1};
        assertEquals(0.4f, f1score.run(groundTruthVector1, simVector1));

        int[] groundTruthVector2 = {1, 1, 1, 1};
        float[] simVector2 = {1, 1, 1, 1};
        assertEquals(1.0f, f1score.run(groundTruthVector2, simVector2));

        int[] groundTruthVector3 = {1, 1, 1, 1};
        float[] simVector3 = {0, 0, 0, 0};
        assertEquals(0.0f, f1score.run(groundTruthVector3, simVector3));

        int[] groundTruthVector4 = {1, 0, 1, 0};
        float[] simVector4 = {1, 1, 1, 0};
        assertEquals(0.8f, f1score.run(groundTruthVector4, simVector4));

    }
}