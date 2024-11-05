package de.uni_marburg.schematch.evaluation.metric;

public class Overall extends Metric{

    @Override
    public float run(int[] groundTruthVector, float[] simVector) {

        float recall = new Recall().run(groundTruthVector, simVector);
        float precision = new Precision().run(groundTruthVector, simVector);

        float score = 0f;

        if(precision > 0) {
            score = recall * (2.0f - (1.0f / precision));
        }

        return score;
    }
}
