package de.uni_marburg.schematch.data.metadata.dependency;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.data.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UniqueColumnCombination implements Dependency{
    Collection<Column> columnCombination;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UniqueColumnCombination that = (UniqueColumnCombination) o;
        return Objects.equals(columnCombination, that.columnCombination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columnCombination);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (Column column : columnCombination) {
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
