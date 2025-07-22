package de.uni_marburg.schematch.utils;

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
