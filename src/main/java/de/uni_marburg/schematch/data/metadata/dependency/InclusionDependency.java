package de.uni_marburg.schematch.data.metadata.dependency;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.data.metadata.Datatype;
import de.uni_marburg.schematch.similarity.string.JaroWinkler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InclusionDependency implements Dependency{
    Collection<Column> dependant; //Untermenge //In FKC ist Foreign Key
    Collection<Column> referenced; //Übermenge //dependent ist enthalten in referenced //In FKC ist Primary Key

    public Collection<Column> getSubset(){
        return dependant;
    }

    public Collection<Column> getSuperset(){
        return referenced;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (Column column : dependant) {
            sb.append(column.getTable().getName());
            sb.append(".");
            sb.append(column.getLabel());
            sb.append(", ");
        }
        sb.delete(sb.length() - 2, sb.length()); //delete trailing ", "
        sb.append("]");
        sb.append(" [= ");
        sb.append("[");
        for (Column column : referenced) {
            sb.append(column.getTable().getName());
            sb.append(".");
            sb.append(column.getLabel());
            sb.append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append("]");
        return sb.toString();
    }

    //Check if IND could represent Foreign Key Constraint
    //IND(A,B)

    public Object[] calculateFeatureVectorFKC() {

        if(this.getDependant().size() == 1 && this.getReferenced().size() == 1) {
            Column foreignKey = this.getDependant().iterator().next(); //dependent //A
            Column primaryKey = this.getReferenced().iterator().next(); //referenced //B

            Set<String> distinctForeignKeyValues = new HashSet<>(foreignKey.getValues());
            Set<String> distinctPrimaryKeyValues = new HashSet<>(primaryKey.getValues());

            String foreignKeyName = foreignKey.getLabel();
            String primaryKeyName = primaryKey.getLabel();

            int f1 = distinctDependentValues(distinctForeignKeyValues);
            double f2 = coverage(distinctForeignKeyValues, distinctPrimaryKeyValues);
            int f3 = dependentAndReferenced(foreignKey);
            int f4 = multiDependent(foreignKey);
            int f5 = multiReferenced(primaryKey);
            double f6 = columnNameSimilarity(foreignKeyName, primaryKeyName + "_" + primaryKey.getTable().getName());
            double f7 = valueLengthDifference(foreignKey, primaryKey);
            double f8 = outOfRange(distinctForeignKeyValues, distinctPrimaryKeyValues);
            boolean f9 = typicalNameSuffix(foreignKeyName);
            double f10 = tableSizeRatio(foreignKey, primaryKey);

            return new Object[]{f1, f2, f3, f4, f5, f6, f7, f8, f9, f10};
        } else {
            return new Object[]{-1};
        }
    }

    private int distinctDependentValues(Set<String> distinctForeignKeyValues) {
        return distinctForeignKeyValues.size();
    } //The higher the better

    private double coverage(Set<String> distinctForeignKeyValues, Set<String> distinctPrimaryKeyValues) {
        Set<String> commonValues = new HashSet<>(distinctForeignKeyValues);
        commonValues.retainAll(distinctPrimaryKeyValues);

        return (double) commonValues.size() / (double) distinctPrimaryKeyValues.size();
    } //The higher the better

    private int dependentAndReferenced(Column foreignKey) {
        Collection<InclusionDependency> inds = foreignKey.getTable().getDatabase().getMetadata().getInds();

        int count = 0;

        for(InclusionDependency ind : inds) {
            if(ind.getReferenced().size() == 1) {
                Column referenced = ind.getReferenced().iterator().next(); //referenced

                if(referenced.equals(foreignKey)) {
                    count++;
                }
            }
        }

        return count;
    } //The lower the better

    private int multiDependent(Column foreignKey) {
        Collection<InclusionDependency> inds = foreignKey.getTable().getDatabase().getMetadata().getInds();

        int count = 0;

        for(InclusionDependency ind : inds) {
            if(ind.getReferenced().size() == 1) {
                Column dependent = ind.getDependant().iterator().next(); //referenced

                if(dependent.equals(foreignKey)) {
                    count++;
                }
            }
        }

        return count;
    } //The lower the better

    private int multiReferenced(Column primaryKey) {
        Collection<InclusionDependency> inds = primaryKey.getTable().getDatabase().getMetadata().getInds();

        int count = 0;

        for(InclusionDependency ind : inds) {
            if(ind.getReferenced().size() == 1) {
                Column referenced = ind.getReferenced().iterator().next(); //referenced

                if(referenced.equals(primaryKey)) {
                    count++;
                }
            }
        }

        return count;
    } //The higher the better

    private double columnNameSimilarity(String foreignKeyName, String primaryKeyName) {
        JaroWinkler jw = new JaroWinkler();
        return jw.compare(foreignKeyName, primaryKeyName);
    } //The higher the better

    private double valueLengthDifference(Column foreignKey, Column primaryKey) {

        OptionalDouble avgValueLengthForeignKey = foreignKey.getValues().stream().mapToInt(String::length).average();
        OptionalDouble avgValueLengthPrimaryKey = primaryKey.getValues().stream().mapToInt(String::length).average();

        if(avgValueLengthForeignKey.isPresent() && avgValueLengthPrimaryKey.isPresent()) {
            return Math.abs(avgValueLengthForeignKey.getAsDouble() - avgValueLengthPrimaryKey.getAsDouble());
        } else {
            //throw new IllegalArgumentException("Columns do not contain values. Heuristic cannot be used.");
            return -1.0;
        }
    } //The lower the better

    private double outOfRange(Set<String> distinctForeignKeyValues, Set<String> distinctPrimaryKeyValues) {

        Set<String> valuesOnlyContainedInPrimaryKey = new HashSet<>(distinctPrimaryKeyValues);
        valuesOnlyContainedInPrimaryKey.removeAll(distinctForeignKeyValues);

        return (double) valuesOnlyContainedInPrimaryKey.size() / (double) distinctPrimaryKeyValues.size();
    } //The lower the better

    private boolean typicalNameSuffix(String foreignKeyName) {

        String[] suffixes = {"id", "key", "nr", "no"};

        for(String suffix : suffixes) {
            if(foreignKeyName.endsWith(suffix)) {
                return true;
            }
        }

        return false;
    } //True is better

    private double tableSizeRatio(Column foreignKey, Column primaryKey) {
        return (double) foreignKey.getValues().size() / (double) primaryKey.getValues().size();
    } //The higher the better

}
