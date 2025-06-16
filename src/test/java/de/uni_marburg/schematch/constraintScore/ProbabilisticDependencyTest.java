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
    public static Column A = new Column("Y", List.of("1", "1", "1", "2", "2"));
    Table table = new Table("Test", List.of("X", "A"), List.of(X, A), null, null);

    @Test
    void gpdepTest() {
        FunctionalDependency X_A = new FunctionalDependency(List.of(X), A);
        FunctionalDependency A_X = new FunctionalDependency(List.of(A), X);

        Assertions.assertEquals(0.04, X_A.calculateGPDEPScore(), 0.001);
        Assertions.assertEquals(0.013, A_X.calculateGPDEPScore(), 0.001);
    }

}
