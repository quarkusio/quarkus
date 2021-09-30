package io.quarkus.panache.common;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Utility class to build and represent SQL sorting specifications. A {@link Sort} instance represents
 * a list of columns to sort on, each with a direction to use for sorting.
 * </p>
 * 
 * <p>
 * Usage:
 * </p>
 * 
 * <code><pre>
 * Sort sort = Sort.by("name").and("age", Direction.Descending);
 * Sort sort2 = Sort.ascending("name", "age");
 * Sort sort3 = Sort.descending("name", "age");
 * </pre></code>
 *
 * @author Stéphane Épardaud
 * @see Direction
 */
public class Sort {

    /**
     * Represents an SQL direction in which to sort results.
     *
     * @author Stéphane Épardaud
     */
    public enum Direction {
        /**
         * Sort in ascending order (the default).
         */
        Ascending,
        /**
         * Sort in descending order (opposite from the default).
         */
        Descending;
    }

    public static class Column {
        private String name;
        private Direction direction;

        public Column(String name) {
            this(name, Direction.Ascending);
        }

        public Column(String name, Direction direction) {
            this.name = name;
            this.direction = direction;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Direction getDirection() {
            return direction;
        }

        public void setDirection(Direction direction) {
            this.direction = direction;
        }
    }

    private List<Column> columns = new ArrayList<>();

    private Sort() {
    }

    /**
     * Sort by the given column, in ascending order.
     * 
     * @param column the column to sort on, in ascending order.
     * @return a new Sort instance which sorts on the given column in ascending order.
     * @see #by(String, Direction)
     * @see #by(String...)
     */
    public static Sort by(String column) {
        return new Sort().and(column);
    }

    /**
     * Sort by the given column, in the given order.
     * 
     * @param column the column to sort on, in the given order.
     * @param direction the direction to sort on
     * @return a new Sort instance which sorts on the given column in the given order.
     * @see #by(String)
     * @see #by(String...)
     */
    public static Sort by(String column, Direction direction) {
        return new Sort().and(column, direction);
    }

    /**
     * Sort by the given columns, in ascending order. Equivalent to {@link #ascending(String...)}.
     * 
     * @param columns the columns to sort on, in ascending order.
     * @return a new Sort instance which sorts on the given columns in ascending order.
     * @see #by(String, Direction)
     * @see #by(String)
     * @see #ascending(String...)
     * @see #descending(String...)
     */
    public static Sort by(String... columns) {
        Sort sort = new Sort();
        for (String column : columns) {
            sort.and(column);
        }
        return sort;
    }

    /**
     * Sort by the given columns, in ascending order. Equivalent to {@link #by(String...)}.
     * 
     * @param columns the columns to sort on, in ascending order.
     * @return a new Sort instance which sorts on the given columns in ascending order.
     * @see #by(String, Direction)
     * @see #by(String)
     * @see #by(String...)
     * @see #descending(String...)
     */
    public static Sort ascending(String... columns) {
        return by(columns);
    }

    /**
     * Sort by the given columns, in descending order.
     * 
     * @param columns the columns to sort on, in descending order.
     * @return a new Sort instance which sorts on the given columns in descending order.
     * @see #by(String, Direction)
     * @see #by(String)
     * @see #descending(String...)
     */
    public static Sort descending(String... columns) {
        Sort sort = new Sort();
        for (String column : columns) {
            sort.and(column, Direction.Descending);
        }
        return sort;
    }

    /**
     * Sets the order to descending for all current sort columns.
     * 
     * @return this instance, modified.
     * @see #ascending()
     * @see #direction(Direction)
     */
    public Sort descending() {
        return direction(Direction.Descending);
    }

    /**
     * Sets the order to ascending for all current sort columns.
     * 
     * @return this instance, modified.
     * @see #descending()
     * @see #direction(Direction)
     */
    public Sort ascending() {
        return direction(Direction.Ascending);
    }

    /**
     * Sets the order to all current sort columns.
     * 
     * @param direction the direction to use for all current sort columns.
     * @return this instance, modified.
     * @see #descending()
     * @see #ascending()
     */
    public Sort direction(Direction direction) {
        for (Column column : columns) {
            column.direction = direction;
        }
        return this;
    }

    /**
     * Adds a sort column, in ascending order.
     * 
     * @param name the new column to sort on, in ascending order.
     * @return this instance, modified.
     * @see #and(String, Direction)
     */
    public Sort and(String name) {
        columns.add(new Column(name));
        return this;
    }

    /**
     * Adds a sort column, in the given order.
     * 
     * @param name the new column to sort on, in the given order.
     * @return this instance, modified.
     * @see #and(String)
     */
    public Sort and(String name, Direction direction) {
        columns.add(new Column(name, direction));
        return this;
    }

    /**
     * Get the sort columns
     *
     * @return the sort columns
     */
    public List<Column> getColumns() {
        return columns;
    }

    /**
     * Creates an Empty Sort instance. Equivalent to {@link #by()}.
     *
     * @return a new empty Sort instance
     * @see #by(String[])
     */
    public static Sort empty() {
        return by();
    }
}
