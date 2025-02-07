package de.uni_marburg.schematch.utils;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.data.metadata.PdepTriple;
import de.uni_marburg.schematch.data.metadata.dependency.Dependency;
import de.uni_marburg.schematch.data.metadata.dependency.FunctionalDependency;
import de.uni_marburg.schematch.data.metadata.dependency.InclusionDependency;
import de.uni_marburg.schematch.data.metadata.dependency.UniqueColumnCombination;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MetadataUtils {

//    public static PdepTriple getPdep(FunctionalDependency fd){
//        Collection<Column> determinant = fd.getDeterminant();
//        Column dependant = fd.getDependant();
//        int N = dependant.getValues().size();
//        Map<String, Integer> frequencyMapDep = createFrequencyMap(dependant);
//        Map<String, Integer> frequencyMapDet = createFrequencyMap(determinant, N);
//
//        double pdep = 1.0;
//        double gpdep = gpdep(frequencyMapDet, frequencyMapDep,N);
//        return new PdepTriple(pdep, gpdep, -1);
//    }

    public static PdepTriple getPdep(FunctionalDependency fd){
        double pdep = calculatePDEPScore(fd);
        double gpdep = 0.0;
        double ngpdep = calculateNGPDEPScore(fd);
        return new PdepTriple(pdep, gpdep, ngpdep);
    }


    public static double epdep(int dA, Map<String, Integer> valuesB, int N) { // Equals E[pdep(X->Y,R)]
        double pdepB = pdep(valuesB, N);

        return pdepB + (dA - 1.0) / (N - 1.0) * (1.0 - pdepB);
    }

    private static double pdep(Map<String, Integer> valuesB, int N) { // Equals pdep(Y,R)
        double result = 0;
        for (Integer count : valuesB.values()) {
            result += (count*count);
        }
        return result / (N*N);
    }

    public static double gpdep(Map<String, Integer> valuesA, Map<String, Integer> valuesB, int N) { // Equals gpdep(X->Y,R) = pdep(X->Y,R) - E[pdep(X->Y,R)] (In case of strict FDs pdep(X->Y,R) becomes 1)
        double pdepAB = 1;
        double epdepAB = epdep(valuesA.size(), valuesB, N);

        return pdepAB - epdepAB;
    }

    public static Map<String, Integer> createFrequencyMap(Column column) { //Frequency Map for Dependent (Y)
        Map<String, Integer> frequencyMap = new HashMap<>();

        for (String value : column.getValues()) {
            frequencyMap.put(value, frequencyMap.getOrDefault(value, 0) + 1);
        }

        return frequencyMap;
    }

    //size is for amount of tuples to consider, starting from first
    public static Map<String, Integer> createFrequencyMap(Collection<Column> columns, int size) { //Frequency Map for Determinant (X)
        Map<String, Integer> frequencyMap = new HashMap<>();

        for (int i = 0; i < size; i++) {
            StringBuilder concatenatedValue = new StringBuilder();
            for (Column col : columns) {
                concatenatedValue.append(col.getValues().get(i));
            }
            String key = concatenatedValue.toString();
            frequencyMap.put(key, frequencyMap.getOrDefault(key, 0) + 1);
        }

        return frequencyMap;
    }

    public static double calculatePDEPScore(FunctionalDependency fd){

        int N = fd.getDependant().getValues().size();
        Collection<Column> X = fd.getDeterminant();
        Column Y = fd.getDependant();

        Set<String> distinctXValues = getDistinctValues(X, N);
        Set<String> distinctYValues = getDistinctValues(List.of(Y), N);
        Map<String, Integer> frequencyCountForX = getFrequencyCount(X, N);

        double score = 0.0;

        for(String xValue : distinctXValues){
            for(String yValue : distinctYValues){

                int jointCount = countNumberOfRecordsWithGivenXandY(xValue, yValue, fd.getDeterminant(), fd.getDependant(), N);
                int frequency = frequencyCountForX.get(xValue);

                score = score + ((jointCount * jointCount) / (double) frequency);
            }
        }

        return score / (double) N;
    }

    public static double calculateSelfDependencyScore(Collection<Column> columnCombination) {

        int N = columnCombination.iterator().next().getValues().size();
        Set<String> distinctValues = getDistinctValues(columnCombination, N);
        Map<String, Integer> frequencyCount = getFrequencyCount(columnCombination, N);

        double score = 0.0;

        for(String value : distinctValues){
            int frequency = frequencyCount.get(value);
            score = score + (double) (frequency * frequency);
        }

        return score / (double) (N * N);
    }

    public static double calculateNGPDEPScore(FunctionalDependency fd) {

        Collection<Column> X = fd.getDeterminant();
        Column Y = fd.getDependant();

        double pdepXY = calculatePDEPScore(fd);
        double pdepY = calculateSelfDependencyScore(List.of(Y));
        int N = fd.getDependant().getValues().size();
        int K = getDistinctValues(X, N).size();

        double score = 1.0 - (((1.0 - pdepXY) / (1.0 - pdepY)) * ((N - 1.0) / (N - K)));

        return Math.max(score, 0.0);
    }

    public static Set<String> getDistinctValues(Collection<Column> columnCombination, int N) {

        Set<String> values = new HashSet<>();

        for(int i = 0; i < N; i++){

            StringBuilder stringBuilder = new StringBuilder();

            for(Column column : columnCombination){
                stringBuilder.append(column.getValues().get(i));
                stringBuilder.append(",");
            }

            stringBuilder.setLength(stringBuilder.length() - 1); //remove last ","
            values.add(stringBuilder.toString());
        }

        return values;
    }

    public static int countNumberOfRecordsWithGivenXandY(String x, String y, Collection<Column> columnCombinationForX, Column columnForY, int N) {

        int count = 0;

        for(int i = 0; i < N; i++){

            StringBuilder stringBuilder = new StringBuilder();

            for(Column column : columnCombinationForX){
                stringBuilder.append(column.getValues().get(i));
                stringBuilder.append(",");
            }
            stringBuilder.setLength(stringBuilder.length() - 1);

            if(stringBuilder.toString().equals(x) && columnForY.getValues().get(i).equals(y)){
                count++;
            }
        }
        return count;
    }

    public static Map<String, Integer> getFrequencyCount(Collection<Column> columnCombination, int N) {

        Map<String, Integer> frequencyCount = new HashMap<>();

        for(int i = 0; i < N; i++) {
            StringBuilder stringBuilder = new StringBuilder();

            for(Column column : columnCombination) {
                stringBuilder.append(column.getValues().get(i));
                stringBuilder.append(",");
            }

            stringBuilder.setLength(stringBuilder.length() - 1);
            String valuesForX = stringBuilder.toString();

            frequencyCount.put(valuesForX, frequencyCount.getOrDefault(valuesForX, 0) + 1);
        }

        return frequencyCount;
    }

    public static boolean metadataExists(String filePath, String dep) {
        Path path = Paths.get(filePath);
        String fileNameWithoutExtension = path.getFileName().toString().replaceFirst("[.][^.]+$", "");

        Path parentDirectory = path.getParent();

        if (parentDirectory != null) {

            String folderName = parentDirectory.getFileName().toString();
            Path metadataFolder = parentDirectory.resolve("metadata");
            Path stFolder = metadataFolder.resolve(folderName);

            if (Files.exists(stFolder) && Files.isDirectory(stFolder)) {
                Path targetFolder = stFolder.resolve(fileNameWithoutExtension);
                Path indPath  = stFolder.resolve("inds.txt");
                Path fdPath = targetFolder.resolve("FD_results.txt");
                Path uccPath = targetFolder.resolve("UCC_results.txt");

                return switch (dep) {
                    case "UCC" -> fileContainsContent(uccPath);
                    case "FD" -> fileContainsContent(fdPath);
                    case "IND" -> fileContainsContent(indPath);
                    default -> false;
                };
            }
        }

        return false;
    }

    private static boolean fileContainsContent(Path filePath) {
        try {
            BufferedReader reader = Files.newBufferedReader(filePath);
            String line = reader.readLine();
            reader.close();
            return line != null && !line.trim().isEmpty();
        } catch (IOException e) {
            return false;
        }
    }

    public static Path getMetadataPathFromTable(Path path) {
        return getMetadataPath(path, true);
    }

    public static Path getMetadataRootPathFromTable(Path path) {
        return getMetadataPath(path, false);
    }

    public static Path getMetadataPath(Path path, boolean includeFileName) {
        String fileNameWithoutExtension = path.getFileName().toString().replaceFirst("[.][^.]+$", "");

        Path parentDirectory = path.getParent();

        if (parentDirectory != null) {
            String folderName = parentDirectory.getFileName().toString();

            Path metadataFolder = parentDirectory.getParent().resolve("metadata");

            Path stFolder = metadataFolder.resolve(folderName);

            if (Files.exists(stFolder) && Files.isDirectory(stFolder)) {
                return includeFileName ? stFolder.resolve(fileNameWithoutExtension) : stFolder;
            }
        }
        return null;
    }

    public static void saveDeps(Path path, Collection<? extends Dependency> objects, String fileName) {
        Path filePath = path.resolve(fileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
            for (Dependency object : objects) {
                writer.write(object.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveINDs(Path path, Collection<? extends InclusionDependency> inds) {
        saveDeps(path, inds, "inds.txt");
    }

    public static void saveUCCs(Path path, Collection<? extends UniqueColumnCombination> uccs) {
        saveDeps(path, uccs, "UCC_results.txt");
    }

    public static void saveFDs(Path path, Collection<? extends FunctionalDependency> fDs) {
        saveDeps(path, fDs, "FD_results.txt");
    }
}
