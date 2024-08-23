package io.quarkus.redis.datasource.search;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrBlank;
import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

/**
 * Allows customizing the highlighting.
 */
public class HighlightArgs implements RedisCommandExtraArguments {

    private String[] fields;

    private String openTag;
    private String closeTag;

    /**
     * Each passed field is highlighted. If no {@code FIELDS} directive is passed, then all fields returned are
     * highlighted.
     *
     * @param fields the fields
     * @return the current {@code HighlightArgs}
     */
    public HighlightArgs fields(String... fields) {
        doesNotContainNull(notNullOrEmpty(fields, "fields"), "fields");
        this.fields = fields;
        return this;
    }

    /**
     * Configure the tags wrapping the highlighted words.
     * {@code open} is prepended to each term match, {@code close} is appended to it.
     * If no {@code TAGS} are specified, a built-in tag value is appended and prepended.
     *
     * @param open the open tag
     * @param close the close tag
     * @return the current {@code HighlightArgs}
     */
    public HighlightArgs tags(String open, String close) {
        this.openTag = notNullOrBlank(open, "open");
        this.closeTag = notNullOrBlank(close, "close");
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> list = new ArrayList<>();
        list.add("HIGHLIGHT");
        if (fields != null && fields.length > 0) {
            list.add("FIELDS");
            list.add(Integer.toString(fields.length));
            list.addAll(Arrays.asList(fields));
        }
        if (openTag != null && closeTag != null) {
            list.add("TAGS");
            list.add(openTag);
            list.add(closeTag);
        }
        return list;
    }
}
