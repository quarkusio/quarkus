package io.quarkus.redis.datasource.search;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrBlank;
import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;
import static io.smallrye.mutiny.helpers.ParameterValidation.positiveOrZero;
import static io.smallrye.mutiny.helpers.ParameterValidation.validate;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;
import io.quarkus.redis.datasource.codecs.Codec;

/**
 * Represents the extra arguments of the {@code ft.search} command
 */
public class QueryArgs implements RedisCommandExtraArguments {

    public boolean nocontent;
    private boolean verbatim;

    private boolean withScores;
    private boolean withPayloads;

    private boolean withSortKeys;

    private final List<NumericFilter> filters = new ArrayList<>();
    private final List<GeoFilter> geoFilters = new ArrayList<>();
    private String[] inKeys;
    private String[] inFields;

    private final List<ReturnClause> returns = new ArrayList<>();
    private SummarizeArgs summarize;
    private HighlightArgs highlight;
    private int slop = -1;
    private boolean inOrder;
    private String lang;
    private String expander;
    private String scorer;
    private boolean explainScore;

    private String asc;
    private String desc;
    private int offset = -1;
    private int count = -1;
    private Duration timeout;
    private final Map<String, String> params = new HashMap<>();
    private final Map<String, byte[]> byteArrayParams = new HashMap<>();
    private int dialect = -1;

    /**
     * Returns the document ids and not the content. This is useful if RedisSearch is only an index on an external
     * document collection.
     *
     * @return the current {@code QueryArgs}
     */
    public QueryArgs nocontent() {
        this.nocontent = true;
        return this;
    }

    /**
     * Does not try to use stemming for query expansion but searches the query terms verbatim.
     *
     * @return the current {@code QueryArgs}
     */
    public QueryArgs verbatim() {
        this.verbatim = true;
        return this;
    }

    /**
     * Also returns the relative internal score of each document.
     * This can be used to merge results from multiple instances.
     *
     * @return the current {@code QueryArgs}
     */
    public QueryArgs withScores() {
        this.withScores = true;
        return this;
    }

    /**
     * Retrieves optional document payloads.
     *
     * @return the current {@code QueryArgs}
     */
    public QueryArgs withPayloads() {
        this.withPayloads = true;
        return this;
    }

    /**
     * returns the value of the sorting key, right after the id and score and/or payload, if requested.
     * This is usually not needed, and exists for distributed search coordination purposes.
     * This option is relevant only if used in conjunction with {@code SORTBY}.
     *
     * @return the current {@code QueryArgs}
     */
    public QueryArgs withSortKeys() {
        this.withSortKeys = true;
        return this;
    }

    /**
     * Limits results to those having numeric values ranging between min and max, if {@code numberFilter} is defined as
     * a numeric attribute in {@code FT.CREATE}.
     * Min and max follow {@code ZRANGE} syntax, and can be {@code -inf}, {@code +inf}, and use ( for exclusive ranges. Multiple
     * numeric filters for different attributes are supported in one query.
     *
     * @param filter the filter
     * @return the current {@code QueryArgs}
     */
    public QueryArgs filter(NumericFilter filter) {
        nonNull(filter, "filter");
        this.filters.add(filter);
        return this;
    }

    /**
     * Filters the results to a given radius from lon and lat.
     * Radius is given as a number and units. See {@code GEORADIUS} for more details.
     *
     * @param filter the filter
     * @return the current {@code QueryArgs}
     */
    public QueryArgs geoFilter(GeoFilter filter) {
        nonNull(filter, "filter");
        this.geoFilters.add(filter);
        return this;
    }

    /**
     * Limits the result to a given set of keys specified in the list.
     * Non-existent keys are ignored, unless all the keys are non-existent.
     *
     * @param keys the list of keys
     * @return the current {@code QueryArgs}
     */
    public final QueryArgs inKeys(String... keys) {
        doesNotContainNull(notNullOrEmpty(keys, "keys"), "keys");
        this.inKeys = keys;
        return this;
    }

    /**
     * Filters the results to those appearing only in specific attributes of the document, like title or URL.
     *
     * @param fields the list of fields
     * @return the current {@code QueryArgs}
     */
    public QueryArgs inFields(String... fields) {
        doesNotContainNull(notNullOrEmpty(fields, "fields"), "fields");
        this.inFields = fields;
        return this;
    }

    /**
     * Limits the attributes returned from the document.
     * If no return clauses are passed, it acts like {@code NOCONTENT}.
     * {@code field} is either an attribute name (for hashes and JSON) or a JSON Path expression (for JSON).
     * {@code alias} is the optional name used in the result. If not provided, the {@code field} is used in the result.
     *
     * @param field the field
     * @param alias the alias
     * @return the current {@code QueryArgs}
     */
    public QueryArgs returnAttribute(String field, String alias) {
        notNullOrBlank(field, "field");
        this.returns.add(new ReturnClause(field, alias));
        return this;
    }

    /**
     * Limits the attributes returned from the document.
     * If no return clauses are passed, it acts like {@code NOCONTENT}.
     * {@code field} is either an attribute name (for hashes and JSON) or a JSON Path expression (for JSON).
     * {@code alias} is the name used in the result. As it is not provided, the {@code field} is used in the result.
     *
     * @param field the field
     * @return the current {@code QueryArgs}
     */
    public QueryArgs returnAttribute(String field) {
        this.returns.add(new ReturnClause(notNullOrBlank(field, "field"), null));
        return this;
    }

    /**
     * Returns only the sections of the attribute that contain the matched text.
     *
     * @param args the summarize argument
     * @return the current {@code QueryArgs}
     */
    public QueryArgs summarize(SummarizeArgs args) {
        this.summarize = nonNull(args, "args");
        return this;
    }

    /**
     * formats occurrences of matched text.
     *
     * @param args the summarize argument
     * @return the current {@code QueryArgs}
     */
    public QueryArgs highlight(HighlightArgs args) {
        this.highlight = nonNull(args, "args");
        return this;
    }

    /**
     * Allows a maximum of {@code slop} intervening number of unmatched offsets between phrase terms.
     * In other words, the slop for exact phrases is 0.
     *
     * @param slop the slop
     * @return the current {@code QueryArgs}
     */
    public QueryArgs slop(int slop) {
        this.slop = positive(slop, "slop");
        return this;
    }

    /**
     * Puts the query terms in the same order in the document as in the query, regardless of the offsets between them.
     * Typically used in conjunction with {@code SLOP}.
     *
     * @return the current {@code QueryArgs}
     */
    public QueryArgs inOrder() {
        this.inOrder = true;
        return this;
    }

    /**
     * Use a stemmer for the supplied language during search for query expansion.
     * If querying documents in Chinese, set to chinese to properly tokenize the query terms.
     * Defaults to English.
     * If an unsupported language is sent, the command returns an error.
     *
     * @param lang the language
     * @return the current {@code QueryArgs}
     */
    public QueryArgs language(String lang) {
        this.lang = notNullOrBlank(lang, "lang");
        return this;
    }

    /**
     * Uses a custom query expander instead of the stemmer.
     *
     * @param expander the expander
     * @return the current {@code QueryArgs}
     */
    public QueryArgs expander(String expander) {
        this.expander = notNullOrBlank(expander, "expander");
        return this;
    }

    /**
     * Uses a custom scoring function you define
     *
     * @param scorer the scorer
     * @return the current {@code QueryArgs}
     */
    public QueryArgs scorer(String scorer) {
        this.scorer = notNullOrBlank(scorer, "scorer");
        return this;
    }

    /**
     * Returns a textual description of how the scores were calculated. Using this options requires
     * the {@code sCORES} option.
     *
     * @return the current {@code QueryArgs}
     */
    public QueryArgs explainScore() {
        this.explainScore = true;
        return this;
    }

    /**
     * Orders the results by the value of this attribute.
     * Use ascending order.
     * This applies to both text and numeric attributes.
     * Attributes needed for {@code SORTBY} should be declared as {@code SORTABLE} in the index, in order to be
     * available with very low latency. Note that this adds memory overhead.
     *
     * @param field the field
     * @return the current {@code QueryArgs}
     */
    public QueryArgs sortByAscending(String field) {
        this.asc = notNullOrBlank(field, "field");
        return this;
    }

    /**
     * Orders the results by the value of this attribute.
     * Use descending order.
     * This applies to both text and numeric attributes.
     * Attributes needed for {@code SORTBY} should be declared as {@code SORTABLE} in the index, in order to be
     * available with very low latency. Note that this adds memory overhead.
     *
     * @param field the field
     * @return the current {@code QueryArgs}
     */
    public QueryArgs sortByDescending(String field) {
        this.desc = notNullOrBlank(field, "field");
        return this;
    }

    /**
     * Limits the results to the offset and number of results given.
     * Note that the offset is zero-indexed. The default is 0 10, which returns 10 items starting from the first result.
     * You can use {@code LIMIT 0 0} to count the number of documents in the result set without actually returning them.
     *
     * @param offset the offset
     * @param count the count
     * @return the current {@code QueryArgs}
     */
    public QueryArgs limit(int offset, int count) {
        this.offset = positiveOrZero(offset, "offset");
        this.count = positiveOrZero(count, "count");
        return this;
    }

    /**
     * Overrides the timeout parameter of the module.
     *
     * @param timeout the timeout
     * @return the current {@code QueryArgs}
     */
    public QueryArgs timeout(Duration timeout) {
        this.timeout = validate(timeout, "timeout");
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
     * @param value the parameter value as String
     * @return the current {@code QueryArgs}
     */
    public QueryArgs param(String name, String value) {
        this.params.put(notNullOrBlank(name, "name"), notNullOrBlank(value, "value"));
        return this;
    }

    /**
     * Defines a parameter with a byte array value.
     *
     * @param name the parameter name
     * @param value the parameter value as byte array
     * @return the current {@code QueryArgs}
     */
    public QueryArgs param(String name, byte[] value) {
        this.byteArrayParams.put(notNullOrBlank(name, "name"), notNullOrEmpty(value, "value"));
        return this;
    }

    /**
     * Defines a parameter with a float array value.
     *
     * @param name the parameter name
     * @param value the parameter value as array of floats
     * @return the current {@code QueryArgs}
     */
    public QueryArgs param(String name, float[] value) {
        this.byteArrayParams.put(notNullOrBlank(name, "name"), toByteArray(notNullOrEmpty(value, "value")));
        return this;
    }

    /**
     * Defines a parameter with a double array value.
     *
     * @param name the parameter name
     * @param value the parameter value as array of doubles
     * @return the current {@code QueryArgs}
     */
    public QueryArgs param(String name, double[] value) {
        this.byteArrayParams.put(notNullOrBlank(name, "name"), toByteArray(notNullOrEmpty(value, "value")));
        return this;
    }

    /**
     * Defines a parameter with an int array value.
     *
     * @param name the parameter name
     * @param value the parameter value as array of ints
     * @return the current {@code QueryArgs}
     */
    public QueryArgs param(String name, int[] value) {
        this.byteArrayParams.put(notNullOrBlank(name, "name"), toByteArray(notNullOrEmpty(value, "value")));
        return this;
    }

    /**
     * Defines a parameter with a long array value.
     *
     * @param name the parameter name
     * @param value the parameter value as array of longs
     * @return the current {@code QueryArgs}
     */
    public QueryArgs param(String name, long[] value) {
        this.byteArrayParams.put(notNullOrBlank(name, "name"), toByteArray(notNullOrEmpty(value, "value")));
        return this;
    }

    /**
     * Selects the dialect version under which to execute the query.
     * If not specified, the query will execute under the default dialect version set during module initial loading.
     *
     * @param version the version
     * @return the current {@code QueryArgs}
     */
    public QueryArgs dialect(int version) {
        this.dialect = version;
        return this;
    }

    @Override
    public List<Object> toArgs(Codec encoder) {
        List<Object> list = new ArrayList<>();

        if (nocontent) {
            list.add("NOCONTENT");
        }
        if (verbatim) {
            list.add("VERBATIM");
        }
        if (withScores) {
            list.add("WITHSCORES");
        }
        if (withPayloads) {
            list.add("WITHPAYLOADS");
        }
        if (withSortKeys) {
            list.add("WITHSORTKEYS");
        }

        for (NumericFilter filter : filters) {
            list.add("FILTER");
            list.add(filter.getField());
            list.add(filter.getLowerBound());
            list.add(filter.getUpperBound());
        }
        for (GeoFilter filter : geoFilters) {
            list.add(filter.toString());
        }

        if (inKeys != null && inKeys.length > 0) {
            list.add(Integer.toString(inKeys.length));
            list.addAll(Arrays.asList(inKeys));
        }

        if (!returns.isEmpty()) {
            list.add("RETURN");
            List<String> clauses = new ArrayList<>();
            for (ReturnClause clause : returns) {
                clauses.addAll(clause.toArgs());
            }
            list.add(Integer.toString(clauses.size()));
            list.addAll(clauses);
        }

        if (inFields != null && inFields.length > 0) {
            list.add(Integer.toString(inFields.length));
            Collections.addAll(list, inFields);
        }

        if (summarize != null) {
            list.addAll(summarize.toArgs());
        }
        if (highlight != null) {
            list.addAll(highlight.toArgs());
        }

        if (slop > -1) {
            list.add("SLOP");
            list.add(Integer.toString(slop));
        }

        if (inOrder) {
            list.add("INORDER");
        }

        if (lang != null) {
            list.add("LANGUAGE");
            list.add(lang);
        }
        if (expander != null) {
            list.add("EXPANDER");
            list.add(expander);
        }
        if (scorer != null) {
            list.add("SCORER");
            list.add(scorer);
        }

        if (explainScore) {
            list.add("EXPLAINSCORE");
        }

        if (asc != null || desc != null) {
            if (asc != null && desc != null) {
                throw new IllegalArgumentException("Cannot use descending and ascending order at the same time");
            }
            list.add("SORTBY");
            if (asc != null) {
                list.add(asc);
                list.add("ASC");
            }
            if (desc != null) {
                list.add(desc);
                list.add("DESC");
            }
        }

        if (offset != -1) {
            list.add("LIMIT");
            list.add(Integer.toString(offset));
            list.add(Integer.toString(count));
        }

        if (timeout != null) {
            list.add("TIMEOUT");
            list.add(Long.toString(timeout.toMillis()));
        }

        if (!params.isEmpty() || !byteArrayParams.isEmpty()) {
            list.add("PARAMS");
            list.add(Integer.toString((params.size() + byteArrayParams.size()) * 2));
            for (Map.Entry<String, byte[]> entry : byteArrayParams.entrySet()) {
                list.add(entry.getKey());
                list.add(entry.getValue());
            }
            for (Map.Entry<String, String> entry : params.entrySet()) {
                list.add(entry.getKey());
                list.add(entry.getValue());
            }
        }

        if (dialect != -1) {
            list.add("DIALECT");
            list.add(Integer.toString(dialect));
        }
        return list;
    }

    private final static class ReturnClause {
        private final String field;
        private final String alias;

        public ReturnClause(String field, String alias) {
            this.field = field;
            this.alias = alias;
        }

        public List<String> toArgs() {
            List<String> list = new ArrayList<>();
            list.add(field);
            if (alias != null) {
                list.add("AS");
                list.add(alias);
            }
            return list;
        }
    }

    public boolean containsScore() {
        return withScores;
    }

    public boolean containsPayload() {
        return withPayloads;
    }

    public boolean containsSortKeys() {
        return withSortKeys;
    }

    private byte[] toByteArray(float[] input) {
        byte[] bytes = new byte[Float.BYTES * input.length];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().put(input);
        return bytes;
    }

    private byte[] toByteArray(double[] input) {
        byte[] bytes = new byte[Double.BYTES * input.length];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asDoubleBuffer().put(input);
        return bytes;
    }

    private byte[] toByteArray(int[] input) {
        byte[] bytes = new byte[Integer.BYTES * input.length];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(input);
        return bytes;
    }

    private byte[] toByteArray(long[] input) {
        byte[] bytes = new byte[Long.BYTES * input.length];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asLongBuffer().put(input);
        return bytes;
    }

}
