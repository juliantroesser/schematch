package de.uni_marburg.schematch.matching.similarityFlooding;

import lombok.Getter;

@Getter
public class LabelEdge {

    private final String value;

    public LabelEdge(String label) {
        this.value = label;
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
