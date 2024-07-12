package de.uni_marburg.schematch.boosting;

import de.uni_marburg.schematch.matchtask.MatchTask;
import de.uni_marburg.schematch.matchtask.matchstep.SimMatrixBoostingStep;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Getter
@Setter
public class ThresholdSelectBoosting implements SimMatrixBoosting {

    double threshold;

    public ThresholdSelectBoosting(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public float[][] run(MatchTask matchTask, SimMatrixBoostingStep matchStep, float[][] simMatrix) {

        for (float[] matrix : simMatrix) {
            onlyKeepRowValuesAboveThreshold(matrix);
        }

        return simMatrix;
    }

    private void convertRowToRelativeValues(float[] row) {

        float maxValue = 0.0f;

        for (float value : row) {
            if (value > maxValue) {
                maxValue = value;
            }
        }

        for (int i = 0; i < row.length; i++) {
            row[i] = row[i] / maxValue;
        }
    }

    private void onlyKeepRowValuesAboveThreshold(float[] row) {

        convertRowToRelativeValues(row);

        for (int i = 0; i < row.length; i++) {
            if (row[i] >= threshold) {
                row[i] = 1;
            } else {
                row[i] = 0;
            }
        }
    }

}
