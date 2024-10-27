package de.uni_marburg.schematch.matching.similarityFlooding;

import lombok.Getter;

@Getter
public class LabelEdge {

    private final String label;

    public LabelEdge(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof LabelEdge otherlabelEdge) { //Equality holds between two Objects if both are of class LabelEdge
            return this.label.equals(otherlabelEdge.label); //And both hold the equal String value
        }
        return false;
    }

}
