package de.uni_marburg.schematch.constraintScore;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.data.Database;
import de.uni_marburg.schematch.data.Table;
import de.uni_marburg.schematch.data.metadata.dependency.FunctionalDependency;
import de.uni_marburg.schematch.utils.MetadataUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

public class ProbabilisticDependencyTest {

    public static Column postCode = new Column("post code", List.of("35037", "35037", "42331", "56894", "37291", "37291"));

    public static Column city = new Column("city", List.of("Marburg", "Marburg", "Düsseldorf", "Frankfurt", "Köln", "Köln"));
    public static Column city2 = new Column("city2", List.of("Marburg", "Frankfurt", "Düsseldorf", "Frankfurt", "Köln", "Frankfurt"));
    public static Column city3 = new Column("city2", List.of("Frankfurt", "Marburg", "Düsseldorf", "Marburg", "Köln", "Marburg"));

    public static Column random = new Column("Z", List.of("1", "2", "3", "3", "4", "d"));
    public static Column random2 = new Column("Z", List.of("F", "F", "F", "F", "F", "F"));
    

    Table table = new Table("Test", List.of("post Code", "city"), List.of(postCode, city), null,null);
    Table table2 = new Table("Test", List.of("post Code", "city2"), List.of(postCode, city2), null, null);
    Table table3 = new Table("Test", List.of("post Code", "city3"), List.of(postCode, city3), null, null);
    Table table4 = new Table("Test", List.of("post Code", "random"), List.of(postCode, random), null, null);
    Table table5 = new Table("Test", List.of("post Code", "rando2"), List.of(postCode, random2), null, null);


    @Test
    void selfDependencyScoreTest() {
        Assertions.assertEquals(0.5, FunctionalDependency.getSelfDependencyScore(List.of(postCode)), 0.001);
        Assertions.assertEquals(0.5, FunctionalDependency.getSelfDependencyScore(List.of(city)), 0.001);
        Assertions.assertEquals(0.833, FunctionalDependency.getSelfDependencyScore(List.of(city2)), 0.001);
        Assertions.assertEquals(0.833, FunctionalDependency.getSelfDependencyScore(List.of(city3)), 0.001);
        Assertions.assertEquals(0.333, FunctionalDependency.getSelfDependencyScore(List.of(random)), 0.001);
    }

    @Test
    void pdepTest() {
        FunctionalDependency fd = new FunctionalDependency(List.of(postCode), city);
        FunctionalDependency fd2 = new FunctionalDependency(List.of(postCode), city2);
        FunctionalDependency fd3 = new FunctionalDependency(List.of(postCode), city3);
        FunctionalDependency fd4 = new FunctionalDependency(List.of(postCode),random);
        FunctionalDependency fd5 = new FunctionalDependency(List.of(postCode),random2);

        Assertions.assertTrue(fd.getPDEPScore() > fd2.getPDEPScore());
        Assertions.assertTrue(fd2.getPDEPScore() == fd3.getPDEPScore());
        Assertions.assertTrue(fd3.getPDEPScore() > fd4.getPDEPScore());
        System.out.println(fd5.getPDEPScore());
//        Assertions.assertEquals(0.8, fd.getPDEPScore(), 0.001);
//        Assertions.assertEquals(0.533, fd2.getPDEPScore(), 0.001);
    }

    @Test
    void gpdepTest() {
        FunctionalDependency fd = new FunctionalDependency(List.of(postCode), city);
        FunctionalDependency fd2 = new FunctionalDependency(List.of(postCode), city2);
        FunctionalDependency fd3 = new FunctionalDependency(List.of(postCode), city3);
        FunctionalDependency fd4 = new FunctionalDependency(List.of(postCode),random);
        FunctionalDependency fd5 = new FunctionalDependency(List.of(postCode),random2);

        Assertions.assertTrue(fd.getGPDEPScore() > fd2.getGPDEPScore());
        Assertions.assertTrue(fd2.getGPDEPScore() == fd3.getGPDEPScore());
        Assertions.assertTrue(fd3.getGPDEPScore() > fd4.getGPDEPScore());
        System.out.println(fd5.getGPDEPScore());
//        Assertions.assertEquals(0.04, X_Y.getGPDEPScore(), 0.001);
//        Assertions.assertEquals(0.013, Y_X.getGPDEPScore(), 0.001);
    }

    @Test
    void ngpdepTest() {
        //TODO
//        FunctionalDependency fd = new FunctionalDependency(List.of(postCode), city);
//        System.out.println(fd.getAltNGPDEPSumScore());
//        FunctionalDependency fd2 = new FunctionalDependency(List.of(postCode), city2);
//        FunctionalDependency fd3 = new FunctionalDependency(List.of(postCode), city3);
//        FunctionalDependency fd4 = new FunctionalDependency(List.of(postCode),random);
//        FunctionalDependency fd5 = new FunctionalDependency(List.of(postCode),random2);
//
//        Assertions.assertTrue(fd.getPDEPScore() > fd2.getAltNGPDEPSumScore());
//        Assertions.assertTrue(fd2.getPDEPScore() == fd3.getAltNGPDEPSumScore());
//        Assertions.assertTrue(fd3.getPDEPScore() > fd4.getAltNGPDEPSumScore());
//        System.out.println(fd5.getAltNGPDEPSumScore());
//        Assertions.assertEquals(0.1666, X_Y.getNGPDEPScore(), 0.001);
//        Assertions.assertEquals(0.0277, Y_X.getNGPDEPScore(), 0.001);
    }




}
