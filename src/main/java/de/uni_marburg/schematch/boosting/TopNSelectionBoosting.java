package de.uni_marburg.schematch.boosting;

import de.uni_marburg.schematch.matching.Matcher;
import de.uni_marburg.schematch.matchtask.MatchTask;
import de.uni_marburg.schematch.matchtask.matchstep.MatchingStep;
import de.uni_marburg.schematch.matchtask.matchstep.SimMatrixBoostingStep;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.Comparator;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TopNSelectionBoosting implements SimMatrixBoosting {

    private int n = 1;

    @Override
    public float[][] run(MatchTask matchTask, SimMatrixBoostingStep matchStep, float[][] simMatrix) {
        return setTopNOfEachRowToOne(convertMatrixRowsToRelativeValues(simMatrix), n);
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

    private float[][] setTopNOfEachRowToOne(float[][] matrix, int n) {

        float[][] topNMatrix = new float[matrix.length][matrix[0].length];

        for(int i = 0; i < matrix.length; i++) {

            int[] topNIndices = getTopNIndicesForRow(matrix[i], n);

            for(int topNIndex : topNIndices) {
                topNMatrix[i][topNIndex] = 1.0f;
            }
        }

        return topNMatrix;
    }

    private int[] getTopNIndicesForRow(float[] row, int n) {

        Integer[] indices = new Integer[row.length];

        for(int i = 0; i < row.length; i++) {
            indices[i] = i;
        }

        Arrays.sort(indices, new Comparator<Integer>() {
            @Override
            public int compare(Integer i1, Integer i2) {
                return Float.compare(row[i2], row[i1]);
            }
        });

        int[] topNIndices = new int[n];
        for(int i = 0; i < n; i++) {
            topNIndices[i] = indices[i];
        }

        return topNIndices;
    }

    public static void main(String[] args) {

        TopNSelectionBoosting boosting = new TopNSelectionBoosting();

        float[][] testMatrix = {{0.95f, 0.67f},
                                {0.32f, 0.83f},
                                {0.52f, 0.52f}};

        float[] testArray = {0.95f, 0.32f, 0.75f, 0.76f};

        //System.out.println(Arrays.toString(boosting.getTopNIndicesForRow(testArray, 2)));

        System.out.println(Arrays.deepToString(boosting.setTopNOfEachRowToOne(testMatrix, 2)));

    }

}
