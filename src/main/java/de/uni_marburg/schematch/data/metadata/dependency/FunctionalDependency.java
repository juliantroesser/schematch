package de.uni_marburg.schematch.data.metadata.dependency;

import de.uni_marburg.schematch.data.Column;
import lombok.Data;

import java.util.*;

@Data
public class FunctionalDependency implements Dependency {
    Collection<Column> determinant;
    Column dependant;

    //{left1, left2,...} -> {right}
    public FunctionalDependency(Collection<Column> left, Column right) {
        this.determinant = left;
        this.dependant = right;
    }

    public static double getSelfDependencyScore(Collection<Column> columnCombination) {

        int N = columnCombination.iterator().next().getValues().size();
        ArrayList<String> distinctValues = Util.getDistinctValues(columnCombination, N);
        Map<String, Integer> frequencyCount = getFrequencyCount(columnCombination, N);

        double score = 0.0;

        for (String value : distinctValues) {
            int frequency = frequencyCount.get(value);
            score = score + (double) (frequency * frequency);
        }

        return score / (double) (N * N);
    } //probabilistic self dependency measure


    //----------------------------------------------------------------------------------------

    public static int countNumberOfRecordsWithGivenXandY(String x, String y, Collection<Column> columnCombinationForX, Column columnForY, int N) {

        int count = 0;

        for (int i = 0; i < N; i++) {

            StringBuilder stringBuilder = new StringBuilder();

            for (Column column : columnCombinationForX) {
                stringBuilder.append(column.getValues().get(i));
                stringBuilder.append(",");
            }
            stringBuilder.setLength(stringBuilder.length() - 1);

            if (stringBuilder.toString().equals(x) && columnForY.getValues().get(i).equals(y)) {
                count++;
            }
        }
        return count;
    }

    public static Map<String, Integer> getFrequencyCount(Collection<Column> columnCombination, int N) {

        Map<String, Integer> frequencyCount = new HashMap<>();

        for (int i = 0; i < N; i++) {
            StringBuilder stringBuilder = new StringBuilder();

            for (Column column : columnCombination) {
                stringBuilder.append(column.getValues().get(i));
                stringBuilder.append(",");
            }

            stringBuilder.setLength(stringBuilder.length() - 1);
            String valuesForX = stringBuilder.toString();

            frequencyCount.put(valuesForX, frequencyCount.getOrDefault(valuesForX, 0) + 1);
        }

        return frequencyCount;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        if (determinant.isEmpty()) {
            sb.append("[]");
        } else {
            for (Column column : determinant) { //[tableName1.columnName1, tableName2.columnName2, ... ]
                sb.append(column.getTable().getName());
                sb.append(".");
                sb.append(column.getLabel());
                sb.append(", ");
            }
        }

        sb.delete(sb.length() - 2, sb.length()); // Remove the trailing ", "
        sb.append("]");
        sb.append(" --> ");
        sb.append(dependant.getTable().getName()).append(".").append(dependant.getLabel()); // [tableName.columnName]

        return sb.toString();
    }

    public double getAltNGPDEPSumScore() {

        if (this.getDeterminant().isEmpty()) {
            return 0.0;
        }

        Column Y = this.getDependant();

        double gpdepXY = getGPDEPScore();

        Collection<FunctionalDependency> fdsWithDependentY = this.getDependant().getTable().getDatabase().getMetadata().getFunctionalDependenciesWithGivenDependent(Y);

        double sum = 0.0;

        for (FunctionalDependency fd : fdsWithDependentY) {
            sum += fd.getGPDEPScore();
        }

        if (sum == 0.0) {
            return 0.0;
        } else {
            double score = gpdepXY / sum;
            return Math.max(score, 0.0);
        }
    }

    //TODO: Utils Functions could be moved to another class

    public double getGPDEPScore() {

        if (this.getDeterminant().isEmpty()) { //As epdep Score for X -> Y with X = {} defaults to pdep(Y) which is 1 as all values are equal in Y
            return 0.0;
        }

        Collection<Column> X = this.getDeterminant();
        Column Y = this.getDependant();

        double pdepY = getSelfDependencyScore(List.of(Y));
        int N = this.getDependant().getValues().size();
        int K = Util.getDistinctValues(X, N).size();

        double pdepXY = this.getPDEPScore();
        double epdepXY = pdepY + ((K - 1.0) / (N - 1.0)) * (1.0 - pdepY);

        return pdepXY - epdepXY;
    } //PDEP Measure with reduced bias

    public double getPDEPScore() {

        if (this.getDeterminant().isEmpty()) { //As Pdep Score for X -> Y with X = {} defaults to sum p(x) over all x as pdep(Y|x) becomes 1 as there is only one value in Y which is not dependent on x
            return 1.0;
        }

        int N = this.getDependant().getValues().size();
        Collection<Column> X = this.getDeterminant();
        Column Y = this.getDependant();

        ArrayList<String> distinctXValues = Util.getDistinctValues(X, N);
        ArrayList<String> distinctYValues = Util.getDistinctValues(List.of(Y), N);
        Map<String, Integer> frequencyCountForX = getFrequencyCount(X, N);

        double score = 0.0;

        for (String xValue : distinctXValues) {
            for (String yValue : distinctYValues) {

                int jointCount = countNumberOfRecordsWithGivenXandY(xValue, yValue, this.getDeterminant(), this.getDependant(), N);
                int frequency = frequencyCountForX.get(xValue);

                score = score + ((jointCount * jointCount) / (double) frequency);
            }
        }

        return score / (double) N;
    } //Standard probabilistic dependency measure

    //-----------------------------------------------------------------------------------------


}
