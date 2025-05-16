package de.uni_marburg.schematch.matching.similarityFlooding;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.data.metadata.Datatype;
import de.uni_marburg.schematch.data.metadata.dependency.FunctionalDependency;
import de.uni_marburg.schematch.data.metadata.dependency.InclusionDependency;
import de.uni_marburg.schematch.data.metadata.dependency.UniqueColumnCombination;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class DependencyFilter {
    private static final Logger log = LogManager.getLogger(DependencyFilter.class);

    private static final double FD_FILTER_THRESHOLD = 0.95;
    private final String uccFilterThreshold;
    private final String indFilterThreshold;


    DependencyFilter(String uccFilterThreshold, String indFilterThreshold) {
        this.uccFilterThreshold = uccFilterThreshold;
        this.indFilterThreshold = indFilterThreshold;
    }

    Collection<FunctionalDependency> filterFunctionalDependencies(Collection<FunctionalDependency> functionalDependencies) {

        Collection<FunctionalDependency> filteredFDs = new ArrayList<>();

        for (FunctionalDependency fd : functionalDependencies) {

            //Determinant should have at least one attribute
            //Maximum determinant size of 3 (because large determinant often appear by chance)
            if (!fd.getDeterminant().isEmpty() && fd.getDeterminant().size() <= 3 && fd.getAltNGPDEPSumScore() >= FD_FILTER_THRESHOLD) filteredFDs.add(fd);
        }

        log.info("Filtered FDs: " + functionalDependencies.size() + "->" + filteredFDs.size());

        return filteredFDs;
    }

    Collection<UniqueColumnCombination> filterUniqueColumnCombinations(Collection<UniqueColumnCombination> uniqueColumnCombinations) {

        Collection<UniqueColumnCombination> filteredUCCs = new HashSet<>();

        double threshold = Double.parseDouble(this.uccFilterThreshold);

        for (UniqueColumnCombination ucc : uniqueColumnCombinations) {

            if (!ucc.getColumnCombination().isEmpty() && ucc.getColumnCombination().size() <= 3) { //(Primary) Keys very rarely consist of more than 3 columns
                double score = ucc.getPrimaryKeyScore();

                if (score >= threshold) {
                    filteredUCCs.add(ucc);
                }
            }
        }

        log.info("Reduced AUCCs: " + uniqueColumnCombinations.size() + " -> " + filteredUCCs.size());

        return filteredUCCs;
    }

    Collection<InclusionDependency> filterInclusionDependencies(Collection<InclusionDependency> inclusionDependencies) {

        Collection<InclusionDependency> filteredINDs = new HashSet<>();

        double threshold = Double.parseDouble(this.indFilterThreshold);

        for (InclusionDependency ind : inclusionDependencies) {

            if (!ind.getDependant().isEmpty() && ind.getDependant().size() <= 3) { //Foreign Key Constraints very rarely consist of more than 3 attributes

                Set<Datatype> datatypesInIND = ind.getReferenced().stream().map(Column::getDatatype).collect(Collectors.toSet());

                if (!datatypesInIND.contains(Datatype.BOOLEAN)) { //No boolean column should be part of the ind
                    double score = ind.getForeignKeyScore();

                    if (score >= threshold) {
                        filteredINDs.add(ind);
                    }
                }
            }
        }

        return filteredINDs;
    }

}
