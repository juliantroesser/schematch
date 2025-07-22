package de.uni_marburg.schematch.constraintScore;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.data.Table;
import de.uni_marburg.schematch.data.metadata.dependency.UniqueColumnCombination;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class PrimaryKeyTest {

    static Column X1 = new Column("X1", List.of("A", "A", "B", "B", "C"));
    static Column object_id = new Column("object_id", List.of("1", "1", "2", "3", "3"));
    static Column X2 = new Column("X2", List.of("true", "true", "false", "false", "true"));
    static Column o_attr = new Column("o_attr", List.of("test1", "test11", "test2", "t3", "t3"));
    Table table = new Table("Test", List.of("X1", "object_id", "X2", "o_attr"), List.of(X1, object_id, X2, o_attr), null, null);

    @Test
    void lengthScoreTest() {
        UniqueColumnCombination ucc = new UniqueColumnCombination(List.of(object_id, o_attr));
        Assertions.assertEquals(0.5, ucc.cardinalityScore());
    }

    @Test
    void valueLengthScoreTest() {
        UniqueColumnCombination ucc = new UniqueColumnCombination(List.of(object_id, o_attr));
        Assertions.assertEquals(1.0, ucc.valueLengthScore());
    }

    @Test
    void positionScoreTest() {
        UniqueColumnCombination ucc = new UniqueColumnCombination(List.of(object_id, o_attr));
        Assertions.assertEquals(0.5, ucc.positionScore());
    }

    @Test
    void nameSuffixScoreTest() {
        UniqueColumnCombination ucc = new UniqueColumnCombination(List.of(object_id, o_attr));
        Assertions.assertEquals(0.5, ucc.nameSuffixScore());
    }
}
