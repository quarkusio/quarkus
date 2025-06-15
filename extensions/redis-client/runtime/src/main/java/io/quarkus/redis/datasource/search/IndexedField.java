package io.quarkus.redis.datasource.search;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrBlank;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

/**
 * Represents an indexed field.
 */
public class IndexedField implements RedisCommandExtraArguments {
    private final String field;
    private final String alias;
    private final FieldType type;
    private final FieldOptions options;

    public static IndexedField from(String field, String alias, FieldType type, FieldOptions options) {
        return new IndexedField(field, alias, type, options);
    }

    public static IndexedField from(String field, FieldType type, FieldOptions options) {
        return new IndexedField(field, null, type, options);
    }

    public static IndexedField from(String field, FieldType type) {
        return new IndexedField(field, null, type, null);
    }

    public static IndexedField from(String field, String alias, FieldType type) {
        return new IndexedField(field, alias, type, null);
    }

    IndexedField(String field, String alias, FieldType type, FieldOptions options) {
        this.field = notNullOrBlank(field, "field");
        this.alias = alias;
        this.type = nonNull(type, "type");
        this.options = options;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> list = new ArrayList<>();
        list.add(field);
        if (alias != null) {
            list.add("AS");
            list.add(alias);
        }
        list.add(type.toString());
        if (options != null) {
            list.addAll(options.toArgs());
        }
        return list;
    }
}
