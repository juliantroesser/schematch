package de.uni_marburg.schematch.data.metadata.dependency;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.data.Table;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        Set<String> distinctValues = getDistinctValues(columnCombination, N);
        Map<String, Integer> frequencyCount = getFrequencyCount(columnCombination, N);

        double score = 0.0;

        for (String value : distinctValues) {
            int frequency = frequencyCount.get(value);
            score = score + (double) (frequency * frequency);
        }

        return score / (double) (N * N);
    } //probabilistic self dependency measure

    //----------------------------------------------------------------------------------------

    public static Set<String> getDistinctValues(Collection<Column> columnCombination, int N) {

        Set<String> values = new HashSet<>();

        for (int i = 0; i < N; i++) {

            StringBuilder stringBuilder = new StringBuilder();

            for (Column column : columnCombination) {
                stringBuilder.append(column.getValues().get(i));
                stringBuilder.append(",");
            }

            stringBuilder.setLength(stringBuilder.length() - 1);

            values.add(stringBuilder.toString());
        }

        return values;
    }

    //----------------------------------------------------------------------------------------

    public static int countNumberOfRecordsWithGivenXandY(String x, String y, Collection<Column> columnCombinationForX, Column columnForY, int N) {
        return (int) IntStream.range(0, N).parallel()
                .filter(i -> {
                    String concatenated = columnCombinationForX.stream()
                            .map(column -> column.getValues().get(i))
                            .collect(Collectors.joining(","));
                    return concatenated.equals(x) && columnForY.getValues().get(i).equals(y);
                })
                .count();
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

    public double getRedundancyMeasure() {

        Map<String, Integer> valueOccurrenceCount = new HashMap<>();
        Table table = dependant.getTable();
        int numberRows = table.getColumns().get(0).getValues().size();

        for (int i = 0; i < numberRows; i++) { //iterate through all tuples

            StringBuilder valuesInDeterminant = new StringBuilder();

            for (Column column : determinant) { //Has no order?
                String valueInDeterminant = column.getValues().get(i);

                valuesInDeterminant.append(valueInDeterminant);
                valuesInDeterminant.append(", ");
            }

            valuesInDeterminant.delete(valuesInDeterminant.length() - 2, valuesInDeterminant.length()); //Deletes last ", "
            String key = valuesInDeterminant.toString();
            valueOccurrenceCount.put(key, valueOccurrenceCount.getOrDefault(key, 0) + 1); //count occurrences
        }

        Optional<Integer> count = valueOccurrenceCount.values().stream().reduce(Integer::sum);

        return count.map(integer -> (double) (integer - valueOccurrenceCount.size()) / (double) numberRows).orElse(0.0); //If no values are present in table, lowest possible score
    }

    public double getAltNGPDEPSumScore() {

        if (this.getDeterminant().isEmpty()) {
            return 0.0;
        }

        Collection<Column> X = this.getDeterminant();
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

    public double getAltNGPDEPMaxScore() {

        if (this.getDeterminant().isEmpty()) {
            return 0.0;
        }

        Collection<Column> X = this.getDeterminant();
        Column Y = this.getDependant();

        double gpdepXY = getGPDEPScore();

        Collection<FunctionalDependency> fdsWithDependentY = this.getDependant().getTable().getDatabase().getMetadata().getFunctionalDependenciesWithGivenDependent(Y);

        double max = Double.NEGATIVE_INFINITY;

        for (FunctionalDependency fd : fdsWithDependentY) {
            if (fd.getGPDEPScore() > max) {
                max = fd.getGPDEPScore();
            }
        }

        if (max == 0.0) {
            return 0.0;
        } else {
            double score = gpdepXY / max;
            return Math.max(score, 0.0);
        }
    }

    public double getNGPDEPScore() {

        if (this.getDeterminant().isEmpty()) {
            return 0.0;
        }

        Collection<Column> X = this.getDeterminant();
        Column Y = this.getDependant();

        double pdepXY = this.getPDEPScore();
        double pdepY = getSelfDependencyScore(List.of(Y));
        int N = this.getDependant().getValues().size();
        int K = getDistinctValues(X, N).size();

        if (pdepY == 1.0 || N == K) { //Avoid division by zero (If pdepY equals zero then, the amount of Information that X gives is none)
            return 0.0;
        } else {
            double score = 1.0 - (((1.0 - pdepXY) / (1.0 - pdepY)) * ((N - 1.0) / (N - K)));
            return Math.max(score, 0.0);
        }

    } //PDEP Measure with reduced bias and normalized

    public double getGPDEPScore() {

        if (this.getDeterminant().isEmpty()) { //As epdep Score for X -> Y with X = {} defaults to pdep(Y) which is 1 as all values are equal in Y
            return 0.0;
        }

        Collection<Column> X = this.getDeterminant();
        Column Y = this.getDependant();

        double pdepY = getSelfDependencyScore(List.of(Y));
        int N = this.getDependant().getValues().size();
        int K = getDistinctValues(X, N).size();

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

        Set<String> distinctXValues = getDistinctValues(X, N);
        Set<String> distinctYValues = getDistinctValues(List.of(Y), N);
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
