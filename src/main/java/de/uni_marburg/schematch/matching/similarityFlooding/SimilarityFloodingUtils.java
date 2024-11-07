package de.uni_marburg.schematch.matching.similarityFlooding;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.data.Table;
import java.util.List;
import java.util.Map;

public class SimilarityFloodingUtils {

    public static double calcResidualVector(Map<NodePair, Double> sigma_i, Map<NodePair, Double> sigma_i_plus_1) {

        double residualSum = 0;

        for (NodePair node : sigma_i.keySet()) {

            double value_i = sigma_i.get(node);
            double value_i_plus_1 = sigma_i_plus_1.get(node);

            residualSum = residualSum + Math.pow((value_i - value_i_plus_1), 2);
        }

        return Math.sqrt(residualSum);
    }

    public static boolean hasConverged(Map<NodePair, Double> sigma_i, Map<NodePair, Double> sigma_i_plus_1, double epsilon) {
        return calcResidualVector(sigma_i, sigma_i_plus_1) < epsilon;
    }


    public static void populateSimMatrix(float[][] simMatrix, Map<NodePair, Double> mapping, Table sourceTable, Table targetTable) {

        List<Column> sourceColumns = sourceTable.getColumns();
        List<Column> targetColumns = targetTable.getColumns();

        for (int i = 0; i < sourceColumns.size(); i++) {

            String sourceLabel = sourceColumns.get(i).getLabel();
            Node sourceNode = new Node(sourceLabel, NodeType.COLUMN, null, false, null, sourceTable);

            for (int j = 0; j < targetColumns.size(); j++) {

                String targetLabel = targetColumns.get(j).getLabel();
                Node targetNode = new Node(targetLabel, NodeType.COLUMN, null, false, null, targetTable);

                //TODO: Problem falls zwei verschiedene Tabellen beide Source sind und Attribut mit gleichem Namen haben -> Node langen Namen geben

                float similarity = mapping.getOrDefault(new NodePair(sourceNode, targetNode), 0.0).floatValue();

                simMatrix[sourceTable.getOffset() + i][targetTable.getOffset() + j] = similarity;
            }
        }
    }

}
