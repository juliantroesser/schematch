package de.uni_marburg.schematch.evaluation.metric;

import de.uni_marburg.schematch.utils.MetricUtils;

import java.util.List;

public class NonBinaryRecallAtGroundTruth extends Metric {

    @Override
    public float run(int[] groundTruthVector, float[] simVector) {

//        for (int i = 0; i < groundTruthVector.length; i++) {
//            if (groundTruthVector[i] != 0 && groundTruthVector[i] != 1) {
//                throw new IllegalArgumentException("groundTruthVector contains invalid values. Only 0 or 1 allowed.");
//            }
//            if (simVector[i] != 0 && simVector[i] != 1) {
//                throw new IllegalArgumentException("simVector contains invalid values. Only 0 or 1 allowed.");
//            }
//        }

        List<Integer> sortedSimIndices = MetricUtils.getSortedSimIndices(simVector, groundTruthVector); //Indices in descending order of similarity
        List<Integer> groundTruthIndices = MetricUtils.getGroundTruthIndices(groundTruthVector); //Indices where GT=1

        int groundTruthSize = groundTruthIndices.size(); //Recall@k with k = #GT

        float countTruePositive = 0;

        for(int i = 0; i < groundTruthSize; i++) {
            int currSimIndex = sortedSimIndices.get(i);
            if(groundTruthIndices.contains(currSimIndex)) {
                countTruePositive++;
            }
        }

        return countTruePositive / groundTruthSize;
    }
}
