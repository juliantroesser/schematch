package de.uni_marburg.schematch.boosting;

import de.uni_marburg.schematch.matchtask.MatchTask;
import de.uni_marburg.schematch.matchtask.matchstep.SimMatrixBoostingStep;

public class StableMarriageSelectionBoosting implements SimMatrixBoosting {
    @Override
    public float[][] run(MatchTask matchTask, SimMatrixBoostingStep matchStep, float[][] simMatrix) {
        return new float[0][];
    }
}
