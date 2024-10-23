package de.uni_marburg.schematch.matching.similarityFlooding;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LabelEdgeTest {

    @Test
    void testToString() {
        LabelEdge labelEdge = new LabelEdge("type");
        Assertions.assertEquals("type", labelEdge.toString());
    }

    @Test
    void testEquals() {
        LabelEdge labelEdge1 = new LabelEdge("type");
        LabelEdge labelEdge2 = new LabelEdge("type");
        assertEquals(labelEdge1, labelEdge2);

        LabelEdge labelEdge3 = new LabelEdge("name");
        assertNotEquals(labelEdge1, labelEdge3);

        String labelEdge4 = "type";
        assertNotEquals(labelEdge1, labelEdge4);
    }

    @Test
    void getValue() {
        LabelEdge labelEdge = new LabelEdge("type");
        Assertions.assertEquals("type", labelEdge.getValue());
    }
}