package de.uni_marburg.schematch.evaluation.metric;

import de.uni_marburg.schematch.utils.MetricUtils;

import java.util.*;

public class NonBinaryPrecisionAtGroundTruth extends Metric {
    @Override
    public float run(int[] groundTruthVector, float[] simVector) {
        List<Integer> sortedSimIndices = MetricUtils.getSortedSimIndices(simVector, groundTruthVector); //Indices in descending order of similarity
        List<Integer> groundTruthIndices = MetricUtils.getGroundTruthIndices(groundTruthVector); //Indices where GT=1

        float totalSimScoreTP = 0;
        float totalSimScoreFP = 0;

        for (int i = 0; i < groundTruthIndices.size(); i++) { //Only for GT=1
            int currSimIndex = sortedSimIndices.get(i);
            if (groundTruthIndices.contains(currSimIndex)) {
                totalSimScoreTP += simVector[currSimIndex];
            } else {
                totalSimScoreFP += simVector[currSimIndex];
            }
        }

        float score = 0f;
        if (totalSimScoreTP + totalSimScoreFP > 0) {
            score = totalSimScoreTP / (totalSimScoreTP + totalSimScoreFP);
        }

        return score;
    }

}
