package io.quarkus.redis.datasource.search;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrBlank;
import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;
import io.smallrye.mutiny.helpers.ParameterValidation;

/**
 * Allows configuring the summarizing.
 */
public class SummarizeArgs implements RedisCommandExtraArguments {

    private String[] fields;
    private int frags;
    private int len;
    private String separator;

    /**
     * Each field passed in {@code fields} is summarized.
     * If no {@code FIELDS} directive is passed, then all fields returned are summarized.
     *
     * @param fields the fields
     * @return the current {@code SummarizeArgs}
     */
    public SummarizeArgs fields(String... fields) {
        this.fields = ParameterValidation.doesNotContainNull(notNullOrEmpty(fields, "fields"), "fields");
        return this;
    }

    /**
     * How many fragments should be returned. If not specified, a default of 3 is used.
     *
     * @param fragments the number of fragment
     * @return the current {@code SummarizeArgs}
     */
    public SummarizeArgs fragments(int fragments) {
        positive(fragments, "fragments");
        this.frags = fragments;
        return this;
    }

    /**
     * The number of context words each fragment should contain. Context words surround the found term.
     * A higher value will return a larger block of text. If not specified, the default value is 20.
     *
     * @param length the length of each fragment
     * @return the current {@code SummarizeArgs}
     */
    public SummarizeArgs length(int length) {
        positive(length, "length");
        this.len = length;
        return this;
    }

    /**
     * The string used to divide between individual summary snippets. The default is {@code ...} which is common among
     * search engines; but you may override this with any other string if you desire to programmatically divide them
     * later on. You may use a newline sequence, as newlines are stripped from the result body anyway
     * (thus, it will not be conflated with an embedded newline in the text)
     *
     * @param separator the separator
     * @return the current {@code SummarizeArgs}
     */
    public SummarizeArgs separator(String separator) {
        notNullOrBlank(separator, "separator");
        this.separator = separator;
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> list = new ArrayList<>();
        list.add("SUMMARIZE");
        if (fields != null && fields.length > 0) {
            list.add("FIELDS");
            list.add(Integer.toString(fields.length));
            list.addAll(Arrays.asList(fields));
        }
        if (frags > 0) {
            list.add("FRAGS");
            list.add(Integer.toString(frags));
        }
        if (len > 0) {
            list.add("LEN");
            list.add(Integer.toString(len));
        }
        if (separator != null) {
            list.add("SEPARATOR");
            list.add(separator);
        }
        return list;
    }
}
