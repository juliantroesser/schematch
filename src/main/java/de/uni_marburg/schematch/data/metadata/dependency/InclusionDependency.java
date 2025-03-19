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
}
