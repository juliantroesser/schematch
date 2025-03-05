package de.uni_marburg.schematch.data.metadata.dependency;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.data.Table;
import de.uni_marburg.schematch.data.metadata.PdepTriple;
import lombok.Data;

import java.util.*;

@Data
public class FunctionalDependency implements Dependency{
    Collection<Column> determinant;
    Column dependant;
    PdepTriple pdepTriple;

    //{left1, left2,...} -> {right}
    public FunctionalDependency(Collection<Column> left, Column right){
        this.determinant = left;
        this.dependant = right;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        if(determinant.isEmpty()){
            sb.append("[]");
        } else {
            for (Column column : determinant) { //[tableName1.columnName1, tableName2.columnName2, ... ]
                sb.append(column.getTable().getName());
                sb.append(".");
                sb.append(column.getLabel());
                sb.append(", ");
            }
        }

        sb.delete(sb.length() - 2, sb.length()); // Remove the trailing ", "
        sb.append("]");
        sb.append(" --> ");
        sb.append(dependant.getTable().getName()).append(".").append(dependant.getLabel()); // [tableName.columnName]

        return sb.toString();
    }

    public double getRedundancyMeasure() {

        Map<String, Integer> valueOccurrenceCount = new HashMap<>();
        Table table = dependant.getTable();
        int numberRows = table.getColumns().get(0).getValues().size();
        
        for(int i = 0; i < numberRows; i++){ //iterate through all tuples
            
            StringBuilder valuesInDeterminant = new StringBuilder();
            
            for(Column column : determinant){ //Has no order?
                String valueInDeterminant = column.getValues().get(i);
                
                valuesInDeterminant.append(valueInDeterminant);
                valuesInDeterminant.append(", ");
            }

            valuesInDeterminant.delete(valuesInDeterminant.length() - 2, valuesInDeterminant.length()); //Deletes last ", "
            String key = valuesInDeterminant.toString();
            valueOccurrenceCount.put(key, valueOccurrenceCount.getOrDefault(key, 0) + 1); //count occurrences
        }
        
        Optional<Integer> count = valueOccurrenceCount.values().stream().reduce(Integer::sum);

        return count.map(integer -> (double) (integer - valueOccurrenceCount.keySet().size()) / (double) numberRows).orElse(0.0); //If no values are present in table, lowest possible score
    }
}
