package de.uni_marburg.schematch.constraintScore;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.data.Table;
import de.uni_marburg.schematch.data.metadata.dependency.FunctionalDependency;
import de.uni_marburg.schematch.utils.MetadataUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ProbabilisticDependencyTest {

    public static Column X = new Column("X", List.of("1", "1", "2", "2", "3"));
    public static Column Y = new Column("Y", List.of("1", "1", "1", "2", "2"));
    Table table = new Table("Test", List.of("X", "Y"), List.of(X, Y), null, null);

    @Test
    void selfDependencyScoreTest() {
//        Assertions.assertEquals(0.36, FunctionalDependency.getSelfDependencyScore(List.of(X)), 0.001);
//        Assertions.assertEquals(0.52, FunctionalDependency.getSelfDependencyScore(List.of(Y)), 0.001);
    }

    @Test
    void pdepTest() {
        FunctionalDependency X_Y = new FunctionalDependency(List.of(X), Y);
        FunctionalDependency Y_X = new FunctionalDependency(List.of(Y), X);

//        Assertions.assertEquals(0.8, X_Y.getPDEPScore(), 0.001);
//        Assertions.assertEquals(0.533, Y_X.getPDEPScore(), 0.001);
    }

    @Test
    void gpdepTest() {
        FunctionalDependency X_Y = new FunctionalDependency(List.of(X), Y);
        FunctionalDependency Y_X = new FunctionalDependency(List.of(Y), X);

//        Assertions.assertEquals(0.04, X_Y.getGPDEPScore(), 0.001);
//        Assertions.assertEquals(0.013, Y_X.getGPDEPScore(), 0.001);
    }

    @Test
    void ngpdepTest() {
        FunctionalDependency X_Y = new FunctionalDependency(List.of(X), Y);
        FunctionalDependency Y_X = new FunctionalDependency(List.of(Y), X);

//        Assertions.assertEquals(0.1666, X_Y.getNGPDEPScore(), 0.001);
//        Assertions.assertEquals(0.0277, Y_X.getNGPDEPScore(), 0.001);
    }




}
