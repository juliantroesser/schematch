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
    void pdepTest() {
        FunctionalDependency X_Y = new FunctionalDependency(List.of(X), Y);
        FunctionalDependency Y_X = new FunctionalDependency(List.of(Y), X);

        X_Y.setPdepTriple(MetadataUtils.getPdep(X_Y));
        Y_X.setPdepTriple(MetadataUtils.getPdep(Y_X));

        Assertions.assertEquals(0.8, X_Y.getPdepTriple().pdep, 0.001);
        Assertions.assertEquals(0.533, Y_X.getPdepTriple().pdep, 0.001);
    }

    @Test
    void selfDependencyScoreTest() {
        Assertions.assertEquals(0.36, MetadataUtils.calculateSelfDependencyScore(List.of(X)), 0.001);
        Assertions.assertEquals(0.52, MetadataUtils.calculateSelfDependencyScore(List.of(Y)), 0.001);
    }

    void ngpdepTest() {
        FunctionalDependency X_Y = new FunctionalDependency(List.of(X), Y);
        FunctionalDependency Y_X = new FunctionalDependency(List.of(Y), X);

        X_Y.setPdepTriple(MetadataUtils.getPdep(X_Y));
        Y_X.setPdepTriple(MetadataUtils.getPdep(Y_X));

        Assertions.assertEquals(0.8, X_Y.getPdepTriple().ngpdep, 0.001);
        Assertions.assertEquals(0.533, Y_X.getPdepTriple().ngpdep, 0.001);
    }




}
