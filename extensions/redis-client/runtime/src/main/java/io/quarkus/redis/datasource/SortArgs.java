package io.quarkus.redis.datasource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SortArgs implements RedisCommandExtraArguments {

    /**
     * The direction (ASC or DESC)
     */
    private String direction;
    private String by;
    private Limit limit = Limit.noLimit();
    private boolean alpha;
    private List<String> get;

    /**
     * Use {@code ASC} order (from small to large).
     *
     * @return the current {@code SortArgs}
     **/
    public SortArgs ascending() {
        this.direction = "ASC";
        return this;
    }

    /**
     * Use {@code DESC} order (from large to small).
     *
     * @return the current {@code SortArgs}
     **/
    public SortArgs descending() {
        this.direction = "DESC";
        return this;
    }

    /**
     * Sets the limit option.
     *
     * @param limit the limit value
     * @return the current {@code SortArgs}
     **/
    public SortArgs limit(Limit limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Sets the limit option using the offset and count.
     *
     * @param offset the offset (zero-based)
     * @param count the number of element
     * @return the current {@code SortArgs}
     **/
    public SortArgs limit(long offset, long count) {
        this.limit = new Limit(offset, count);
        return this;
    }

    /**
     * Sets the limit option with only the number of elements.
     *
     * @param count the number of elements
     * @return the current {@code SortArgs}
     **/
    public SortArgs limit(long count) {
        this.limit = new Limit(count);
        return this;
    }

    /**
     * When the list contains string values and you want to sort them lexicographically, use the ALPHA modifier.
     *
     * @return the current {@code SortArgs}
     **/
    public SortArgs alpha() {
        this.alpha = true;
        return this;
    }

    /**
     * Sets the {@code BY} pattern.
     *
     * @param by the by value
     * @return the current {@code SortArgs}
     **/
    public SortArgs by(String by) {
        this.by = by;
        return this;
    }

    /**
     * Sets the {@code GET} option.
     *
     * Retrieving external keys based on the elements in a list, set or sorted set can be done using the {code GET}
     * options.
     *
     * The {@code GET} option can be used multiple times in order to get more keys for every element of the original
     * list, set or sorted set.
     *
     * It is also possible to {@code GET} the element itself using the special pattern {@code #}
     *
     * @param get the get value
     * @return the current {@code SortArgs}
     **/
    public SortArgs get(List<String> get) {
        this.get = get;
        return this;
    }

    /**
     * Adds a {@code GET} option.
     *
     * Retrieving external keys based on the elements in a list, set or sorted set can be done using the {code GET}
     * options.
     *
     * The {@code GET} option can be used multiple times in order to get more keys for every element of the original
     * list, set or sorted set.
     *
     * It is also possible to {@code GET} the element itself using the special pattern {@code #}
     *
     * @param get the get value
     * @return the current {@code SortArgs}
     **/
    public SortArgs get(String get) {
        if (this.get == null) {
            this.get = new ArrayList<>();
        }
        this.get.add(get);
        return this;
    }

    public List<Object> toArgs() {
        List<Object> args = new ArrayList<>();
        if (by != null && !by.isBlank()) {
            args.add("BY");
            args.add(by);
        }

        if (limit != null) {
            args.addAll(limit.toArgs());
        }

        if (get != null && !get.isEmpty()) {
            for (String s : get) {
                if (s != null && !s.isBlank()) {
                    args.add("GET");
                    args.add(s);
                }
            }
        }

        if (direction != null) {
            args.add(direction);
        }

        if (alpha) {
            args.add("ALPHA");
        }

        return args;
    }

    /**
     * Represent a limit.
     * From the Redis SORT command:
     * The number of returned elements can be limited using the {@code LIMIT} modifier. This modifier takes the
     * offset argument, specifying the number of elements to skip and the count argument, specifying the number of
     * elements to return from starting at offset. The following example will return 10 elements of the sorted version
     * of mylist, starting at element 0 (offset is zero-based): {@code SORT mylist LIMIT 0 10}
     *
     */
    public static class Limit {

        private static final Limit NO_LIMIT = new Limit(-1, -1);

        private final long offset;

        private final long count;

        public static Limit of(long offset, long count) {
            return new Limit(offset, count);
        }

        /**
         * Creates a {@link Limit} object.
         *
         * @param offset the offset (0 based)
         * @param count the limit count.
         */
        public Limit(long offset, long count) {
            this.offset = offset;
            this.count = count;
        }

        /**
         * Creates a {@link Limit} object with just a count (offset is 0)
         *
         * @param count the limit count.
         */

        public Limit(long count) {
            this(0L, count);
        }

        public static Limit noLimit() {
            return NO_LIMIT;
        }

        public List<String> toArgs() {
            if (offset == -1 && count == -1) {
                // no limit
                return Collections.emptyList();
            } else {
                List<String> args = new ArrayList<>();
                args.add("LIMIT");
                if (offset != -1) {
                    args.add(Long.toString(offset));
                }
                args.add(Long.toString(count));
                return args;
            }
        }
    }
}