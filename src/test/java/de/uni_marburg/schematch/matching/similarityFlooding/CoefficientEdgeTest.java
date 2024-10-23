package de.uni_marburg.schematch.matching.similarityFlooding;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoefficientEdgeTest {

    @Test
    void testToString() {
        CoefficientEdge coefficientEdge = new CoefficientEdge(0.5);
        Assertions.assertEquals("0.5", coefficientEdge.toString());
    }

    @Test
    void getCoefficient() {
        CoefficientEdge coefficientEdge = new CoefficientEdge(0.5);
        Assertions.assertEquals(0.5, coefficientEdge.getCoefficient());
    }

    @Test
    void checkException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {CoefficientEdge coefficientEdge = new CoefficientEdge(-0.1);});
        Assertions.assertThrows(IllegalArgumentException.class, () -> {CoefficientEdge coefficientEdge = new CoefficientEdge(1.1);});
    }
}