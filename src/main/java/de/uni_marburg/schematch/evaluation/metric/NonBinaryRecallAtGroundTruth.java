package de.uni_marburg.schematch.evaluation.metric;

public class NonBinaryRecallAtGroundTruth extends Metric {
    @Override
    public float run(int[] groundTruthVector, float[] simVector) {

        float totalSimScoreTP = 0;
        float totalSimScoreFN = 0;

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
                }
            }
            if (simScore < lowestGTScore) {
                if (groundTruthVector[i] == 1) {
                    totalSimScoreFN += simScore;
                }
            }
        }

        float score = 0f;
        if (totalSimScoreTP + totalSimScoreFN > 0) {
            score = totalSimScoreTP / (totalSimScoreTP + totalSimScoreFN);
        }

        return score;

    }
}
