package de.uni_marburg.schematch.evaluation.metric;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrecisionTest {

    @Test
    void run() {

        Precision precision = new Precision();

        int[] groundTruthVector1 = {1, 0, 1, 0};
        float[] simVector1 = {1, 1, 1, 0};
        Assertions.assertEquals(2.0f/3.0f, precision.run(groundTruthVector1, simVector1));

        int[] groundTruthVector2 = {0, 0, 0, 0};
        float[] simVector2 = {1, 1, 1, 1};
        Assertions.assertEquals(0.0f, precision.run(groundTruthVector2, simVector2));

        int[] groundTruthVector3 = {1, 2, 1, 0};  // 2 is invalid
        float[] simVector3 = {-1, 1, 1, 0};
        Assertions.assertThrows(IllegalArgumentException.class, () -> precision.run(groundTruthVector3, simVector3));

        int[] groundTruthVector4 = {1, 1, 1, 1};
        float[] simVector4 = {0, 0, 0, 0};
        Assertions.assertEquals(0.0f, precision.run(groundTruthVector4, simVector4));

    }
}