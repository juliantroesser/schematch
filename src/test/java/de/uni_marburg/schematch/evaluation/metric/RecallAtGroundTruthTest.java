package de.uni_marburg.schematch.evaluation.metric;

import org.apache.logging.log4j.core.util.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecallAtGroundTruthTest {

    @Test
    void run() {

        NonBinaryRecallAtGroundTruth recallAtGroundTruth = new NonBinaryRecallAtGroundTruth();

        int[] groundTruthVector1 =  {0, 1, 0, 1};
        float[] simVector1 = {1f, 0f, 1f, 0f};
        Assertions.assertEquals(0, recallAtGroundTruth.run(groundTruthVector1, simVector1));

        int[] groundTruthVector2 =  {1, 0, 1, 1, 0};
        float[] simVector2 = {1f, 0f, 1f, 0f, 0f};
        Assertions.assertEquals(2f/3f,recallAtGroundTruth.run(groundTruthVector2, simVector2));

        int[] groundTruthVector3 =  {1, 0, 1, 0};
        float[] simVector3 = {0.2f, 0.3f, 0.9f, 0.1f};
        Assertions.assertEquals(1f/2f,recallAtGroundTruth.run(groundTruthVector3, simVector3));

        int[] groundTruthVector4 =  {1, 0, 1, 0, 1, 1};
        float[] simVector4 = {0.2f, 0.3f, 0.9f, 0.1f, 0.7f, 0.6f};
        Assertions.assertEquals(3f/4f,recallAtGroundTruth.run(groundTruthVector4, simVector4));

        int[] groundTruthVector5 =  {1, 0, 1, 0, 1, 1};
        float[] simVector5 = {0.9f, 0.1f, 0.8f, 0.2f, 0.7f, 0.6f};
        Assertions.assertEquals(1.0f,recallAtGroundTruth.run(groundTruthVector5, simVector5));
    }
}