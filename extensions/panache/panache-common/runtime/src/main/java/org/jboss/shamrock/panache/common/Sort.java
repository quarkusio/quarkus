package io.quarkus.panache.common;

import java.util.ArrayList;
import java.util.List;

public class Sort {
    private enum Direction {
        Ascending, Descending;
    }

    private class Column {
        private String name;
        private Direction direction;

        public Column(String name) {
            this(name, Direction.Ascending);
        }

        public Column(String name, Direction direction) {
            this.name = name;
            this.direction = direction;
        }
    }

    private List<Column> columns = new ArrayList<>();

    private Sort() {
    }

    public static Sort by(String column) {
        return new Sort().and(column);
    }

    public static Sort by(String column, Direction direction) {
        return new Sort().and(column, direction);
    }

    public static Sort by(String... columns) {
        Sort sort = new Sort();
        for (String column : columns) {
            sort.and(column);
        }
        return sort;
    }

    public static Sort ascending(String... columns) {
        return by(columns);
    }

    public static Sort descending(String... columns) {
        Sort sort = new Sort();
        for (String column : columns) {
            sort.and(column, Direction.Descending);
        }
        return sort;
    }

    public Sort descending() {
        return direction(Direction.Descending);
    }

    public Sort ascending() {
        return direction(Direction.Ascending);
    }

    public Sort direction(Direction direction) {
        for (Column column : columns) {
            column.direction = direction;
        }
        return this;
    }

    public Sort and(String name) {
        columns.add(new Column(name));
        return this;
    }

    public Sort and(String name, Direction direction) {
        columns.add(new Column(name, direction));
        return this;
    }

    public String toOrderBy() {
        StringBuilder sb = new StringBuilder(" ORDER BY ");
        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            if (i > 0)
                sb.append(" , ");
            sb.append(column.name);
            if (column.direction != Direction.Ascending)
                sb.append(" DESC");
        }
        return sb.toString();
    }
}