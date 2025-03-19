package de.uni_marburg.schematch.data.metadata.dependency;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.data.Table;
import lombok.Data;

import java.util.*;

@Data
public class FunctionalDependency implements Dependency{
    Collection<Column> determinant;
    Column dependant;

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

}
