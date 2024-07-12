package de.uni_marburg.schematch.evaluation.metric;

public class Precision extends Metric {

    //TODO: PrecisionTop10Percent
    //TODO: RecallAtSizeOfGroundTruth

    @Override
    public float run(int[] groundTruthVector, float[] simVector) {

        int TP = 0;
        int FP = 0;

        for (int i = 0; i < groundTruthVector.length; i++) {
            if (groundTruthVector[i] == 1 && simVector[i] == 1) {
                TP++;
            } else if (groundTruthVector[i] == 0 && simVector[i] == 1) {
                FP++;
            }
        }

        return (float) TP / (float) (TP + FP);
    }


}
