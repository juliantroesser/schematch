package de.uni_marburg.schematch.matching.matrix_boosting.similarity_flooding;

public class CoefficientEdge {

    private final double coefficient;

    public CoefficientEdge(double coefficient) {
        if (coefficient < 0 || coefficient > 1) {
            throw new IllegalArgumentException("coefficient must be between 0 and 1"); //Coefficient must be between 0 and 1
        } else {
            this.coefficient = coefficient;
        }
    }

    public double getCoefficient() {
        return coefficient;
    }

    @Override
    public String toString() { //Edge is represented by its coefficient value
        return Double.toString(coefficient);
    }
}
