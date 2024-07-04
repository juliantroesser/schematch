package de.uni_marburg.schematch.matching.matrix_boosting.similarity_flooding;

public class LabelEdge {

    private final String value;

    public LabelEdge(String label) {
        this.value = label;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof LabelEdge otherlabelEdge) { //Equality holds between two Objects if both are of class LabelEdge
            return this.value.equals(otherlabelEdge.value); //And both hold the equal String value
        }
        return false;
    }

}
