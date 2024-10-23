package de.uni_marburg.schematch.boosting;

import de.uni_marburg.schematch.matching.Matcher;
import de.uni_marburg.schematch.matchtask.MatchTask;
import de.uni_marburg.schematch.matchtask.matchstep.MatchingStep;
import de.uni_marburg.schematch.matchtask.matchstep.SimMatrixBoostingStep;
import de.uni_marburg.schematch.utils.ArrayUtils;

import java.util.Arrays;
import java.util.List;

import static de.uni_marburg.schematch.utils.ArrayUtils.transposeMatrix;

public class ThresholdSelectionBoosting implements SimMatrixBoosting {

    @Override
    public float[][] run(MatchTask matchTask, SimMatrixBoostingStep matchStep, float[][] simMatrix) {

        float threshold = 1.0f;

        float[][] output = new float[simMatrix.length][simMatrix[0].length];
        float[][] relSimMatrix = convertMatrixRowsToRelativeValues(simMatrix);
        float[][] transposedRelSimMatrix = convertMatrixRowsToRelativeValues(ArrayUtils.transposeMatrix(simMatrix));

        for(int i = 0; i < relSimMatrix.length; i++) {
            for(int j = 0; j < relSimMatrix[i].length; j++) {
                if(relSimMatrix[i][j] >= threshold && transposedRelSimMatrix[j][i] >= threshold) {
                    output[i][j] = 1;
                }
            }
        }

        return output;
    }

    private float[][] convertMatrixRowsToRelativeValues(float[][] matrix) {

        float[][] relativeValueMatrix = new float[matrix.length][matrix[0].length];

        float maxRowValue = 0.0f;

        for(int i = 0; i < matrix.length; i++) {
            for(int j = 0; j < matrix[0].length; j++) {
                //Get Max Row Value
                if(matrix[i][j] > maxRowValue) {
                    maxRowValue = matrix[i][j];
                }
            }
            for(int j = 0; j < matrix[1].length; j++) {
                relativeValueMatrix[i][j] = matrix[i][j] / maxRowValue;
            }
            maxRowValue = 0.0f;
        }

        return relativeValueMatrix;
    }

//    @Override
//    public float[][] match(MatchTask matchTask, MatchingStep matchStep) {
//
//        List<Matcher> matchers = matchTask.getMatchersForLine(this.line);
//
//        for(Matcher matcher : ma)
//
//        matchTask.getSimMatrixFromPreviousMatchStep(this, )
//
//
//        return new float[0][];
//    }
}
