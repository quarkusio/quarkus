package io.quarkus.redis.datasource.search;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrBlank;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;
import static io.smallrye.mutiny.helpers.ParameterValidation.validate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

/**
 * Represents the extra arguments of the {@code ft.aggregate} command.
 */
public class AggregateArgs implements RedisCommandExtraArguments {

    private boolean verbatim;

    private final Map<String, String> fields = new HashMap<>();
    private boolean loadAllFields;
    private Duration timeout;

    private final List<Clause> clauses = new ArrayList<>();
    private int offset = -1;

    private int count = -1;

    private final Map<String, Object> params = new HashMap<>();

    private int dialect = -1;
    private int cursorCount = -1;
    private Duration cursorMaxIdle;
    private boolean cursor;

    /**
     * If set, does not try to use stemming for query expansion but searches the query terms verbatim.
     *
     * @return the current {@code AggregateArgs}
     */
    public AggregateArgs verbatim() {
        this.verbatim = true;
        return this;
    }

    /**
     * Loads all the document attributes from the source documents.
     *
     * @return the current {@code AggregateArgs}
     */
    public AggregateArgs allFields() {
        loadAllFields = true;
        return this;
    }

    /**
     * Adds the given attribute to the list of attribute loaded from the document source.
     * You can pass the list of identifier, being either an attribute name for hashes and JSON or a JSON Path expression
     * for JSON.
     * The alias is the optional name used in the result. If it is not provided, the identifier is used. This should be avoided.
     * <p>
     * Attributes needed for aggregations should be stored as {@code SORTABLE}, where they are available to the
     * aggregation pipeline with very low latency. LOAD hurts the performance of aggregate queries considerably because
     * every processed record needs to execute the equivalent of {@code HMGET} against a Redis key, which when executed
     * over millions of keys, amounts to high processing times.
     *
     * @param field the field
     * @param alias the alias (optional but recommended)
     * @return the current {@code AggregateArgs}
     */
    public AggregateArgs field(String field, String alias) {
        fields.put(notNullOrBlank(field, "field"), alias);
        return this;
    }

    /**
     * Adds the given attribute to the list of attribute loaded from the document source.
     * You can pass the list of identifier, being either an attribute name for hashes and JSON or a JSON Path expression
     * for JSON.
     * <p>
     * Attributes needed for aggregations should be stored as {@code SORTABLE}, where they are available to the
     * aggregation pipeline with very low latency. LOAD hurts the performance of aggregate queries considerably because
     * every processed record needs to execute the equivalent of {@code HMGET} against a Redis key, which when executed
     * over millions of keys, amounts to high processing times.
     *
     * @param field the field
     * @return the current {@code AggregateArgs}
     */
    public AggregateArgs field(String field) {
        fields.put(notNullOrBlank(field, "field"), null);
        return this;
    }

    /**
     * Overrides the timeout parameter of the module.
     *
     * @param timeout the timeout
     * @return the current {@code AggregateArgs}
     */
    public AggregateArgs timeout(Duration timeout) {
        this.timeout = validate(timeout, "timeout");
        return this;
    }

    /**
     * Groups the results in the pipeline based on one or more properties.
     * Each group should have at least one reducer, a function that handles the group entries, either counting them,
     * or performing multiple aggregate operations.
     *
     * @param groupBy the group by clause
     * @return the current {@code AggregateArgs}
     */
    public AggregateArgs groupBy(GroupBy groupBy) {
        clauses.add(nonNull(groupBy, "groupBy"));
        return this;
    }

    /**
     * Sorts the pipeline up until the point of {@code SORTBY}, using the given property and the ascending order.
     *
     * @param sortBy the sort by clause
     * @return the current {@code AggregateArgs}
     */
    public AggregateArgs sortBy(SortBy sortBy) {
        clauses.add(nonNull(sortBy, "sortBy"));
        return this;
    }

    /**
     * Applies a 1-to-1 transformation on one or more properties and either stores the result as a new property down
     * the pipeline or replaces any property using this transformation.
     * <p>
     * {@code expression} is an expression that can be used to perform arithmetic operations on numeric properties,
     * or functions that can be applied on properties depending on their types, or any combination thereof.
     * <p>
     * For example, APPLY "sqrt(@foo)/log(@bar) + 5" AS baz evaluates this expression dynamically for each record in
     * the pipeline and store the result as a new property called baz, which can be referenced by further
     * APPLY/SORTBY/GROUPBY/REDUCE operations down the pipeline.
     *
     * @param apply the Apply clause
     * @return the current {@code AggregateArgs}
     */
    public AggregateArgs apply(Apply apply) {
        clauses.add(nonNull(apply, "apply"));
        return this;
    }

    /**
     * Limits the number of results to return just num results starting at index offset (zero-based).
     * It is much more efficient to use {@code SORTBY … MAX} if you are interested in just limiting the output of a
     * sort operation.
     *
     * @param offset the offset
     * @param count the count
     * @return the current {@code AggregateArgs}
     */
    public AggregateArgs limit(int offset, int count) {
        this.offset = positive(offset, "offset");
        this.count = positive(count, "count");
        return this;
    }

    /**
     * Filters the results using predicate expressions relating to values in each result.
     * They are applied post query and relate to the current state of the pipeline.
     *
     * @param filter the filter
     * @return the current {@code AggregateArgs}
     */
    public AggregateArgs filter(String filter) {
        clauses.add(new Filter(notNullOrBlank(filter, "filter")));
        return this;
    }

    /**
     * Defines one or more value parameters. Each parameter has a name and a value.
     * You can reference parameters in the query by a $, followed by the parameter name, for example, $user.
     * Each such reference in the search query to a parameter name is substituted by the corresponding parameter value.
     * For example, with parameter definition PARAMS 4 lon 29.69465 lat 34.95126, the expression @loc:[$lon $lat 10 km]
     * is evaluated to @loc:[29.69465 34.95126 10 km]. You cannot reference parameters in the query string where concrete
     * values are not allowed, such as in field names, for example, @loc.
     * <p>
     * To use PARAMS, set DIALECT to 2.
     *
     * @param name the parameter name
     * @param value the parameter value
     * @return the current {@code AggregateArgs}
     */
    public AggregateArgs param(String name, Object value) {
        this.params.put(notNullOrBlank(name, "name"), nonNull(value, "value"));
        return this;
    }

    /**
     * Selects the dialect version under which to execute the query. If not specified, the query will execute under
     * the default dialect version set during module initial loading.
     *
     * @param version the version
     * @return the current {@code AggregateArgs}
     */
    public AggregateArgs dialect(int version) {
        this.dialect = version;
        return this;
    }

    /**
     * Scan part of the results with a quicker alternative than {@code LIMIT}.
     *
     * @return the current {@code AggregateArgs}
     */
    public AggregateArgs withCursor() {
        this.cursor = true;
        return this;
    }

    /**
     * When using a cursor, configure the number of result.
     *
     * @param count the number of result to fetch in one go.
     * @return the current {@code AggregateArgs}
     */
    public AggregateArgs cursorCount(int count) {
        this.cursorCount = positive(count, "count");
        return this;
    }

    /**
     * When using a cursor, configure the max idle duration.
     *
     * @param maxIdleDuration the max idle duration of the cursor.
     * @return the current {@code AggregateArgs}
     */
    public AggregateArgs cursorMaxIdleTime(Duration maxIdleDuration) {
        this.cursorMaxIdle = validate(maxIdleDuration, "maxIdleDuration");
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> list = new ArrayList<>();
        if (verbatim) {
            list.add("VERBATIM");
        }

        if (loadAllFields) {
            list.add("LOAD");
            list.add("*");
        }
        if (!fields.isEmpty()) {
            list.add("LOAD");
            List<String> f = new ArrayList<>();
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                f.add(entry.getKey());
                if (entry.getValue() != null) {
                    f.add("AS");
                    f.add(entry.getValue());
                }
            }
            list.add(Integer.toString(f.size()));
            list.addAll(f);
        }
        if (timeout != null) {
            list.add("TIMEOUT");
            list.add(Long.toString(timeout.toMillis()));
        }

        if (!params.isEmpty()) {
            list.add("PARAMS");
            list.add(Integer.toString(params.size() * 2));
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                list.add(entry.getKey());
                list.add(entry.getValue().toString());
            }
        }

        for (Clause c : clauses) {
            list.addAll(c.toArgs());
        }

        if (offset != -1) {
            list.add("LIMIT");
            list.add(Integer.toString(offset));
            list.add(Integer.toString(count));
        }

        if (dialect != -1) {
            list.add("DIALECT");
            list.add(Integer.toString(dialect));
        }

        if (cursor) {
            list.add("WITHCURSOR");
        }
        if (cursorCount != -1) {
            list.add("COUNT");
            list.add(Integer.toString(cursorCount));
        }
        if (cursorMaxIdle != null) {
            list.add("MAXIDLE");
            list.add(Long.toString(cursorMaxIdle.toMillis()));
        }
        return list;

    }

    public boolean hasCursor() {
        return cursor;
    }

    public static class SortBy implements Clause {
        private final Map<String, String> properties = new LinkedHashMap<>();

        private int max = -1;

        public SortBy() {

        }

        public SortBy ascending(String property) {
            properties.put(property, "ASC");
            return this;
        }

        public SortBy descending(String property) {
            properties.put(property, "DESC");
            return this;
        }

        public SortBy max(int max) {
            this.max = max;
            return this;
        }

        @Override
        public List<String> toArgs() {
            if (properties.isEmpty() && max == -1) {
                return Collections.emptyList();
            }
            List<String> list = new ArrayList<>();
            list.add("SORTBY");
            list.add(Integer.toString(properties.size() * 2));
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                list.add(entry.getKey());
                list.add(entry.getValue());
            }
            if (max != -1) {
                list.add("MAX");
                list.add(Integer.toString(max));
            }
            return list;
        }
    }

    private interface Clause {
        List<String> toArgs();
    }

    public static class Apply implements Clause {
        private final String expr;
        private final String alias;

        public Apply(String expr, String alias) {
            this.expr = notNullOrBlank(expr, expr);
            this.alias = notNullOrBlank(alias, "alias");
        }

        @Override
        public List<String> toArgs() {
            return List.of("APPLY", expr, "AS", alias);
        }
    }

    public static class GroupBy implements Clause {

        private final List<String> properties;
        private final List<ReduceFunction> functions;

        public GroupBy() {
            properties = new ArrayList<>();
            functions = new ArrayList<>();
        }

        /**
         * Adds a property to the {@code GROUPBY} clause.
         *
         * @param property the property
         * @return the current {@code GroupBy}
         */
        public GroupBy addProperty(String property) {
            properties.add(nonNull(property, "property"));
            return this;
        }

        /**
         * Adds a reduce function to the {@code GROUPBY} clause.
         *
         * @param function the name of the function
         * @param alias the alias used in the returned document
         * @param args the arguments
         * @return the current {@code GroupBy}
         */
        public GroupBy addReduceFunction(String function, String alias, Object... args) {
            functions.add(new ReduceFunction(function, alias, args));
            return this;
        }

        /**
         * Adds a reduce function to the {@code GROUPBY} clause.
         *
         * @param function the name of the function
         * @param args the arguments
         * @return the current {@code GroupBy}
         */
        public GroupBy addReduceFunction(String function, Object... args) {
            functions.add(new ReduceFunction(function, null, args));
            return this;
        }

        @Override
        public List<String> toArgs() {
            List<String> list = new ArrayList<>();
            list.add("GROUPBY");
            list.add(Integer.toString(properties.size()));
            list.addAll(properties);
            for (ReduceFunction function : functions) {
                list.addAll(function.toArgs());
            }

            return list;
        }
    }

    private static class ReduceFunction {
        private final String function;
        private final Object[] args;
        private final String alias;

        public ReduceFunction(String function, String alias, Object... args) {
            this.function = notNullOrBlank(function, "function");
            this.alias = alias;
            this.args = doesNotContainNull(args, "args");
        }

        public List<String> toArgs() {
            List<String> list = new ArrayList<>();
            list.add("REDUCE");
            list.add(function);
            if (args != null && args.length > 0) {
                list.add(Integer.toString(args.length));
                for (Object arg : args) {
                    list.add(arg.toString());
                }
            } else {
                list.add("0");
            }
            if (alias != null) {
                list.add("AS");
                list.add(alias);
            }
            return list;
        }
    }

    private static class Filter implements Clause {
        private final String filter;

        private Filter(String filter) {
            this.filter = filter;
        }

        @Override
        public List<String> toArgs() {
            return List.of("FILTER", filter);
        }
    }
}
