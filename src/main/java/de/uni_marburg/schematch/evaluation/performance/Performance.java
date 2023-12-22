package de.uni_marburg.schematch.evaluation.performance;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Performance {
    private float globalScore;

    public void addToGlobalScore(float a) {
        this.globalScore += a;
    }
}