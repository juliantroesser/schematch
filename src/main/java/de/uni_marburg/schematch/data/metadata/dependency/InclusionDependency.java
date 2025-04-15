package de.uni_marburg.schematch.data.metadata.dependency;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.similarity.string.Levenshtein;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InclusionDependency implements Dependency {
    Collection<Column> dependant; //Untermenge //In FKC ist Foreign Key
    Collection<Column> referenced; //Übermenge //dependent ist enthalten in referenced //In FKC ist Primary Key

    private static List<String> getTupleValuesForColumns(Collection<Column> columnCombination, int N) {

        List<String> values = new ArrayList<>(N);
        List<Column> sortedColumns = new ArrayList<>(columnCombination);
        sortedColumns.sort(Comparator.comparing(Column::getLabel));

        for (int i = 0; i < N; i++) {

            StringBuilder stringBuilder = new StringBuilder();

            for (Column column : sortedColumns) {
                stringBuilder.append(column.getValues().get(i));
                stringBuilder.append(",");
            }

            stringBuilder.setLength(stringBuilder.length() - 1);

            values.add(stringBuilder.toString());
        }

        return values;
    }

    private static String getLabel(Collection<Column> columnCombination, boolean withTablePrefix) {

        List<String> columnLabels = new ArrayList<>(columnCombination.size());
        for (Column column : columnCombination) {
            columnLabels.add(column.getLabel());
        }
        columnLabels.sort(String.CASE_INSENSITIVE_ORDER);

        String tableName = columnCombination.iterator().next().getTable().getName();

        StringBuilder stringBuilder = new StringBuilder();

        for (String label : columnLabels) {
            if (withTablePrefix) {
                stringBuilder.append(tableName);
                stringBuilder.append("_");
            }
            stringBuilder.append(label);
            stringBuilder.append("-");
        }

        stringBuilder.setLength(stringBuilder.length() - 1);

        return stringBuilder.toString();
    }

    public Collection<Column> getSubset() {
        return dependant;
    }

    public Collection<Column> getSuperset() {
        return referenced;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (Column column : dependant) {
            sb.append(column.getTable().getName());
            sb.append(".");
            sb.append(column.getLabel());
            sb.append(", ");
        }
        sb.delete(sb.length() - 2, sb.length()); //delete trailing ", "
        sb.append("]");
        sb.append(" [= ");
        sb.append("[");
        for (Column column : referenced) {
            sb.append(column.getTable().getName());
            sb.append(".");
            sb.append(column.getLabel());
            sb.append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append("]");
        return sb.toString();
    }

    public double getForeignKeyScore() {
        double coverageScore = coverageScore();
        double columnNameSimilarityScore = columnNameSimilarityScore();
        double valueLengthDifferenceScore = valueLengthDifferenceScore();
        double outOfRangeScore = outOfRangeScore();

        return (coverageScore + columnNameSimilarityScore + valueLengthDifferenceScore + outOfRangeScore) / 4.0;
    }

    private Set<String> getDistinctForeignKeyValues() {
        return new HashSet<>(getForeignKeyValues());
    }

    private Set<String> getDistinctPrimaryKeyValues() {
        return new HashSet<>(getPrimaryKeyValues());
    }

    private List<String> getForeignKeyValues() {
        Collection<Column> foreignKey = this.getDependant();
        int tupleCountForeignKey = foreignKey.iterator().next().getValues().size();
        return getTupleValuesForColumns(foreignKey, tupleCountForeignKey);
    }

    private List<String> getPrimaryKeyValues() {
        Collection<Column> primaryKey = this.getReferenced();
        int tupleCountPrimaryKey = primaryKey.iterator().next().getValues().size();
        return getTupleValuesForColumns(primaryKey, tupleCountPrimaryKey);
    }

    private String getPrimaryKeyName(boolean withTablePrefix) {
        Collection<Column> primaryKey = this.getReferenced();
        return getLabel(primaryKey, withTablePrefix);
    }

    private String getForeignKeyName(boolean withTablePrefix) {
        Collection<Column> foreignKey = this.getDependant();
        return getLabel(foreignKey, withTablePrefix);
    }

    public double coverageScore() { //Range [0,1]: 1 best, every FK value has a match in PK, 0 worst

        Set<String> distinctForeignKeyValues = this.getDistinctForeignKeyValues();
        Set<String> distinctPrimaryKeyValues = this.getDistinctPrimaryKeyValues();

        if (distinctForeignKeyValues.isEmpty()) {
            return 0.0;
        } else {

            int commonValueCount = 0;

            for (String value : distinctForeignKeyValues) { //Values of foreign Key that are also in primary Key
                if (distinctPrimaryKeyValues.contains(value)) {
                    commonValueCount++;
                }
            }

            //Ratio to all values in Foreign Key
            return (double) commonValueCount / (double) distinctForeignKeyValues.size();
        }
    }

    public double columnNameSimilarityScore() { // range [0,1] 1 is exact match, 0 worst
        Levenshtein levenshtein = new Levenshtein();

        String foreignKeyName = getForeignKeyName(false); //Do not add table name as prefix as some foreign Keys already contain table in their label
        String primaryKeyName = getPrimaryKeyName(true);

        return levenshtein.compare(foreignKeyName.toLowerCase(), primaryKeyName.toLowerCase());
    } //The higher the better

    public double valueLengthDifferenceScore() { //Range [0,1]: 1 indicating that both have the same average length, 0 that column(s) do not have values

        List<String> foreignKeyValues = getForeignKeyValues();
        List<String> primaryKeyValues = getPrimaryKeyValues();

        if (foreignKeyValues.isEmpty() || primaryKeyValues.isEmpty()) {
            return 0.0;
        } else {
            double avgValueLengthForeignKey = foreignKeyValues.stream().mapToInt(String::length).average().orElse(0.0);
            double avgValueLengthPrimaryKey = primaryKeyValues.stream().mapToInt(String::length).average().orElse(0.0);

            double differenceInLength = Math.abs(avgValueLengthForeignKey - avgValueLengthPrimaryKey);
            double maxAvgLength = Math.max(Math.max(avgValueLengthForeignKey, avgValueLengthPrimaryKey), 1.0);

            return 1.0 - (differenceInLength / maxAvgLength);
        }
    }

    public double outOfRangeScore() {

        Set<String> distinctForeignKeyValues = getDistinctForeignKeyValues();
        Set<String> distinctPrimaryKeyValues = getDistinctPrimaryKeyValues();

        if (distinctPrimaryKeyValues.isEmpty()) {
            return 0.0;
        }

        int countValuesOnlyInPrimaryKey = 0;

        for (String value : distinctPrimaryKeyValues) { //Values from primary Key that are not in foreign Key
            if (!distinctForeignKeyValues.contains(value)) {
                countValuesOnlyInPrimaryKey++;
            }
        }

        return 1.0 - (double) countValuesOnlyInPrimaryKey / distinctPrimaryKeyValues.size();
    } //Range [0,1]: 1 is best as all foreign Key values are contained in primary Key, 0 worst

}
