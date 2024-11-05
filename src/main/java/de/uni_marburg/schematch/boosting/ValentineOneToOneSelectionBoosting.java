package de.uni_marburg.schematch.boosting;

import de.uni_marburg.schematch.matchtask.MatchTask;
import de.uni_marburg.schematch.matchtask.matchstep.SimMatrixBoostingStep;

import java.util.Arrays;

public class ValentineOneToOneSelectionBoosting implements SimMatrixBoosting {

    @Override
    public float[][] run(MatchTask matchTask, SimMatrixBoostingStep matchStep, float[][] simMatrix) {

        float[][] simMatrixMatchesHigherThanMedian = removeMatchesLowerThanMedian(simMatrix);
        return greedilyKeepHighestMatches(simMatrixMatchesHigherThanMedian);

    }

    public float[][] removeMatchesLowerThanMedian(float[][] simMatrix) {

        int rows = simMatrix.length;
        int cols = simMatrix[0].length;
        float[] allEntries = new float[rows * cols];

        // Flatten the matrix to a single array
        int index = 0;
        for (float[] matrix : simMatrix) {
            for (int j = 0; j < cols; j++) {
                allEntries[index++] = matrix[j];
            }
        }

        // Sort the array to find the median
        Arrays.sort(allEntries);
        double median;
        int middle = allEntries.length / 2;
        if (allEntries.length % 2 == 0) {
            median = (allEntries[middle - 1] + allEntries[middle]) / 2.0;
        } else {
            median = allEntries[middle];
        }

        float[][] result = new float[rows][cols];

        // Set all entries less than the median to 0
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (simMatrix[i][j] >= median) {
                    result[i][j] = simMatrix[i][j];
                }
            }
        }

        return result;
    }

    public float[][] greedilyKeepHighestMatches(float[][] simMatrix) {

        float[][] result = new float[simMatrix.length][simMatrix[0].length];

        while(true) {
            float max = 0;
            int maxIndexX = -1;
            int maxIndexY = -1;

            for(int i = 0; i < simMatrix.length; i++) {
                for(int j = 0; j < simMatrix[0].length; j++) {
                    if(simMatrix[i][j] > max) {
                        max = simMatrix[i][j];
                        maxIndexX = i;
                        maxIndexY = j;
                    }
                }
            }

            if(max == 0.0f) {
                break;
            }

            simMatrix[maxIndexX][maxIndexY] = 0.0f; //Match entfernen, damit bei nächster Iteration nicht gefunden
            result[maxIndexX][maxIndexY] = 1.0f;

            setRowToZero(simMatrix, maxIndexX);
            setColumnToZero(simMatrix, maxIndexY);
        }

        return result;
    }

    public void setRowToZero(float[][] matrix, int rowIndex) {
        if (matrix == null || rowIndex < 0 || rowIndex >= matrix.length) {
            throw new IllegalArgumentException("Invalid matrix or rowIndex");
        }
        int numCols = matrix[rowIndex].length;
        for (int col = 0; col < numCols; col++) {
            matrix[rowIndex][col] = 0;
        }
    }

    public void setColumnToZero(float[][] matrix, int columnIndex) {
        if (matrix == null || columnIndex < 0 || columnIndex >= matrix[0].length) {
            throw new IllegalArgumentException("Invalid matrix or columnIndex");
        }
        int numRows = matrix.length;
        for (int row = 0; row < numRows; row++) {
            matrix[row][columnIndex] = 0;
        }
    }
}
