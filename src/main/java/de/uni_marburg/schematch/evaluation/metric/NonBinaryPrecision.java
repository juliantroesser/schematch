package de.uni_marburg.schematch.evaluation.metric;

public class NonBinaryPrecision extends Metric {
    @Override
    public float run(int[] groundTruthVector, float[] simVector) {

        float totalSimScoreTP = 0;
        float totalSimScoreFP = 0;

        // find the lowest ground truth score
        float lowestGTScore = Float.MAX_VALUE;
        for (int i = 0; i < groundTruthVector.length; i++) {
            if (groundTruthVector[i] == 1 && simVector[i] < lowestGTScore) {
                lowestGTScore = simVector[i];
            }
        }

        // flag all scores >= lowest ground truth score as TP/FP
        for (int i = 0; i < groundTruthVector.length; i++) {
            float simScore = simVector[i];
            if (simScore >= lowestGTScore) {
                if (groundTruthVector[i] == 1) {
                    totalSimScoreTP += simScore;
                } else {
                    totalSimScoreFP += simScore;
                }
            }
        }

        float score = 0f;
        if (totalSimScoreTP + totalSimScoreFP > 0) {
            score = totalSimScoreTP / (totalSimScoreTP + totalSimScoreFP);
        }

        return score;
    }
}
