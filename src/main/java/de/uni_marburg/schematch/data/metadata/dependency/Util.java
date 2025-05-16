package de.uni_marburg.schematch.data.metadata.dependency;

import de.uni_marburg.schematch.data.Column;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class Util {
    public static ArrayList<String> getDistinctValues(Collection<Column> columnCombination, int N) {
        if (columnCombination == null || columnCombination.isEmpty()) {
            throw new IllegalArgumentException("The column collection must not be empty.");
        }

        ArrayList<String> values = new ArrayList<>();

        for (int i = 0; i < N; i++) {
            StringBuilder stringBuilder = new StringBuilder();

            for (Column column : columnCombination) {
                stringBuilder.append(column.getValues().get(i));
                stringBuilder.append(",");
            }

            stringBuilder.setLength(stringBuilder.length() - 1);
            values.add(stringBuilder.toString());
        }

        return values;
    }

    // Formats columns as [Table.Column, ...]
    public static String columnsToString(Collection<Column> columns) {
        StringBuilder sb = new StringBuilder("[");
        Iterator<Column> iterator = columns.iterator();
        while (iterator.hasNext()) {
            Column column = iterator.next();
            sb.append(column.getTable().getName())
                    .append(".")
                    .append(column.getLabel());
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    // Formats a single column as Table.Column
    public static String columnToString(Column column) {
        return column.getTable().getName() + "." + column.getLabel();
    }
}
