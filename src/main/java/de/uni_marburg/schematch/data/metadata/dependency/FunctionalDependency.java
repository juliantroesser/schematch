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

    @Override
    public String toString() {
        String left = determinant.isEmpty() ? "[]" : Util.columnsToString(determinant);
        String right = Util.columnToString(dependant);
        return left + " --> " + right;
    }

    private List<String> getRowsAsStrings(Collection<Column> columns, int rowCount) {

        List<String> rowsAsString = new ArrayList<>(rowCount);
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < rowCount; i++) {
            for (Column column : columns) {
                stringBuilder.append(column.getValues().get(i)).append("|"); //use delimiter to avoid ambiguities
            }

            stringBuilder.setLength(stringBuilder.length() - 1); //remove last delimiter
            String rowAsString = stringBuilder.toString();
            rowsAsString.add(rowAsString);
            stringBuilder.setLength(0);
        }

        return rowsAsString;
    }

    private Map<String, Double> getProbabilityMap(List<String> rowsAsString, int rowCount) {

        Map<String, Double> probabilityMap = new HashMap<>();

        for (String rowAsString : rowsAsString) {
            probabilityMap.merge(rowAsString, 1.0, Double::sum);
        }

        probabilityMap.replaceAll((k, v) -> v / rowCount);
        return probabilityMap;
    }

    private Map<String, Map<String, Double>> getProbabilityMapGivenX(List<String> rowsAsStringX, List<String> rowsAsStringA, int rowCount) {

        Map<String, Map<String, Double>> probabilityMapGivenX = new HashMap<>();

        for (int i = 0; i < rowCount; i++) {
            String x = rowsAsStringX.get(i);
            String a = rowsAsStringA.get(i);

            Map<String, Double> innerMap = probabilityMapGivenX.computeIfAbsent(x, k -> new HashMap<>());
            innerMap.merge(a, 1.0, Double::sum);
        }

        for (Map<String, Double> map : probabilityMapGivenX.values()) {
            double total = map.values().stream().mapToDouble(Double::doubleValue).sum();
            map.replaceAll((k, v) -> v / total);
        }

        return probabilityMapGivenX;
    }

    public double calculateGPDEPScore() {

        int rowCount = this.dependant.getValues().size();

        List<String> rowsForXAsStrings = getRowsAsStrings(this.determinant, rowCount);
        List<String> rowsForAAsStrings = this.getDependant().getValues();

        Map<String, Double> probabilitiesForX = getProbabilityMap(rowsForXAsStrings, rowCount);
        Map<String, Double> probabilitiesForA = getProbabilityMap(rowsForAAsStrings, rowCount);
        Map<String, Map<String, Double>> probabilitiesForAGivenX = getProbabilityMapGivenX(rowsForXAsStrings, rowsForAAsStrings, rowCount);

        double pdepXA = 0.0;

        for(String x : probabilitiesForX.keySet()) {

            double probabilityForX = probabilitiesForX.get(x);
            double pdepAGivenX = 0;
            Map<String, Double> probabilitiesForAGivenXMap = probabilitiesForAGivenX.get(x);

            for(String a : probabilitiesForA.keySet()) {
                double probabilityForAGivenX = probabilitiesForAGivenXMap.getOrDefault(a, 0.0);
                pdepAGivenX = pdepAGivenX + probabilityForAGivenX * probabilityForAGivenX;
            }

            pdepXA = pdepXA + probabilityForX * pdepAGivenX;
        }

        double selfDependencyScoreA = probabilitiesForA.values().stream()
                .mapToDouble(p -> p * p)
                .sum();

        double epdepXA = selfDependencyScoreA + ((probabilitiesForX.keySet().size() - 1.0) / (rowCount - 1.0)) * (1 - selfDependencyScoreA);

        return pdepXA - epdepXA;
    }

    public double calculateNGPDEPScore() {
        if (this.getDeterminant().isEmpty()) {
            return 0.0;
        }

        Column A = this.getDependant();
        double gpdepXA = this.calculateGPDEPScore();

        Collection<FunctionalDependency> fdsWithDependentA =
                A.getTable().getDatabase().getMetadata().getFunctionalDependenciesWithGivenDependent(A);

        double sum = fdsWithDependentA.stream()
                .mapToDouble(FunctionalDependency::calculateGPDEPScore)
                .sum();

        return sum == 0.0 ? 0.0 : Math.max(gpdepXA / sum, 0.0);
    }

}
