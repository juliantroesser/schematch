package de.uni_marburg.schematch.matching.metadata;


import de.uni_marburg.schematch.data.Table;
import de.uni_marburg.schematch.matching.Matcher;
import de.uni_marburg.schematch.matchtask.tablepair.TablePair;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashSet;

@Data
@EqualsAndHashCode(callSuper = true)
public class DistinctCountMatcher extends Matcher {

    @Override
    public float[][] match(TablePair tablePair) {

        final float[][] simMatrix = tablePair.getEmptySimMatrix();
        final Table sourceTable = tablePair.getSourceTable();
        final Table targetTable = tablePair.getTargetTable();

        for (int i = 0; i < sourceTable.getNumberOfColumns(); i++) {
            for (int j = 0; j < targetTable.getNumberOfColumns(); j++) {

                ArrayList<String> sourceValues = new ArrayList<>(sourceTable.getColumn(i).getValues());
                ArrayList<String> targetValues = new ArrayList<>(targetTable.getColumn(j).getValues());

                var similarity = calculateSimilarity(sourceValues, targetValues);

                simMatrix[i][j] = (float) similarity;
            }
        }

        return simMatrix;
    }


    private float calculateSimilarity(ArrayList<String> sourceValues, ArrayList<String> targetValues) {
        HashSet<String> sourceElements = new HashSet<>(sourceValues);
        HashSet<String> targetElements = new HashSet<>(targetValues);

        return sourceElements.size() == targetElements.size()
                ? 1.0f
                : 0.0f;
    }

}
