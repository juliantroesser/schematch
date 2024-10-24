package de.uni_marburg.schematch.evaluation.metric;

public class F1Score extends Metric {
    @Override
    public float run(int[] groundTruthVector, float[] simVector) {

        float precision = new Precision().run(groundTruthVector, simVector);
        float recall = new Recall().run(groundTruthVector, simVector);

        float score = 0f;

        if(precision + recall > 0) {
            score = (2 * precision * recall) / (precision + recall);
        }

        return score;
    }
}
