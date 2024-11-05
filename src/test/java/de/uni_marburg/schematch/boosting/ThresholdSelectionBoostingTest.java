package de.uni_marburg.schematch.boosting;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ThresholdSelectionBoostingTest {

    @Test
    void run() {

        ThresholdSelectionBoosting boosting = new ThresholdSelectionBoosting(1.0);

        float[][] simMatrix = {{0.85f, 0.21f, 0.35f},
                {0.31f, 0.72f, 0.69f},
                {0.42f, 0.73f, 0.52f},
                {0.10f, 0.21f, 0.81f}};

        float[][] expected = {{1f, 0f, 0f},
                {0f, 0f, 0f},
                {0f, 1f, 0f},
                {0f, 0f, 1f}};

        Assertions.assertTrue(Arrays.deepEquals(boosting.run(null, null, simMatrix), expected));

        float[][] simMatrix2 = {{0.1f, 0.1f, 0.1f},
                {0.1f, 0.1f, 0.1f}};

        //If all similarity values are equal it is plausible that each element with all the others
        float[][] expected2 = {{1f, 1f, 1f},
                {1f, 1f, 1f}};

        Assertions.assertTrue(Arrays.deepEquals(boosting.run(null, null, simMatrix2), expected2));

        float[][] simMatrix3 = {{0.21f, 0.7f, 0.95f},
                {0.9f, 0.8f, 0.32f},
                {0.1f, 0.89f, 0.05f}};

        float[][] expected3 = {{0f, 0f, 1f},
                {1f, 0f, 0f},
                {0f, 1f, 0f}};

        Assertions.assertTrue(Arrays.deepEquals(boosting.run(null, null, simMatrix3), expected3));

    }
}