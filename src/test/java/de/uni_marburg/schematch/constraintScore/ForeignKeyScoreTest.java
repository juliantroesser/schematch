package de.uni_marburg.schematch.constraintScore;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.data.Table;
import de.uni_marburg.schematch.data.metadata.dependency.InclusionDependency;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

public class ForeignKeyScoreTest {

    static Column t1_customer_id = new Column("customer_id", List.of("1", "2", "3", "4"));
    static Column t1_branch_id = new Column("branch_id", List.of("101", "102", "103", "104"));
    static Column t1_name = new Column("name", List.of("Alice", "Bob", "Charlie", "David"));
    Table customers = new Table("Customers", List.of("customer_id", "branch_id", "name"), List.of(t1_customer_id, t1_branch_id, t1_name), null, null);

    static Column t2_order_id = new Column("order_id", List.of("101", "102", "103", "104", "105"));
    static Column t2_customer_id = new Column("customer_id", List.of("1", "1", "2", "3", "4"));
    static Column t2_branch_id = new Column("branch_id", List.of("101", "101", "102", "103", "104"));
    static Column t2_order_date = new Column("order_date", List.of("2024-03-01", "2024-03-05", "2024-03-10", "2024-03-12", "2024-03-15"));
    Table orders = new Table("Orders", List.of("order_id", "branch_id", "customer_id", "order_date"), List.of(t2_order_id, t2_branch_id, t2_customer_id, t2_order_date), null, null);

    Collection<Column> primaryKey = List.of(t1_customer_id, t1_branch_id);
    Collection<Column> foreignKey = List.of(t2_branch_id, t2_customer_id);


    @Test
    void coverageScoreTest() {
        InclusionDependency ind = new InclusionDependency(primaryKey, foreignKey);
        Assertions.assertEquals(1.0, ind.coverageScore());
    }

    @Test
    void columnSimilarityScoreTest() {
        InclusionDependency ind = new InclusionDependency(primaryKey, foreignKey);
        Assertions.assertEquals(0.6, ind.columnNameSimilarityScore(), 0.001);
    }

    @Test
    void valueLengthDifferenceScoreTest() {
        InclusionDependency ind = new InclusionDependency(primaryKey, foreignKey);
        Assertions.assertEquals(1.0, ind.valueLengthDifferenceScore());
    }

    @Test
    void outOfRangeScoreTest() {
        InclusionDependency ind = new InclusionDependency(primaryKey, foreignKey);
        Assertions.assertEquals(1.0, ind.outOfRangeScore());
    }

    @Test
    void foreignKeyScoreTest() {
        InclusionDependency ind = new InclusionDependency(primaryKey, foreignKey);
        Assertions.assertEquals(0.9, ind.getForeignKeyScore(0.25, 0.25, 0.25, 0.25), 0.001);
    }


}
