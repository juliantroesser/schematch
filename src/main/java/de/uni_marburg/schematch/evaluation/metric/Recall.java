package de.uni_marburg.schematch.evaluation.metric;

public class Recall extends Metric {

    @Override
    public float run(int[] groundTruthVector, float[] simVector) {

        int TP = 0;
        int FN = 0;

        for (int i = 0; i < groundTruthVector.length; i++) {

            if (groundTruthVector[i] != 0 && groundTruthVector[i] != 1) {
                throw new IllegalArgumentException("groundTruthVector contains invalid values. Only 0 or 1 allowed.");
            }

            if (simVector[i] != 0 && simVector[i] != 1) {
                throw new IllegalArgumentException("simVector contains invalid values. Only 0 or 1 allowed.");
            }

            if (groundTruthVector[i] == 1 && simVector[i] == 1) {
                TP++;
            } else if (groundTruthVector[i] == 1 && simVector[i] == 0) {
                FN++;
            }
        }

        float score = 0f;

        if(TP + FN > 0) {
            score = (float) TP / (float) (TP + FN);
        }

        return score;
    }
}
