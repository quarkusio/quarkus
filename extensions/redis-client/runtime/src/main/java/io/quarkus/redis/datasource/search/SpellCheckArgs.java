package io.quarkus.redis.datasource.search;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;
import io.smallrye.mutiny.helpers.ParameterValidation;

public class SpellCheckArgs implements RedisCommandExtraArguments {

    private int distance;

    private final List<String> includes = new ArrayList<>();
    private final List<String> excludes = new ArrayList<>();
    private int dialect = -1;

    /**
     * Sets the maximum Levenshtein distance for spelling suggestions (default: 1, max: 4).
     *
     * @param distance
     *        the distance
     *
     * @return the current {@code SpellCheckArgs}
     */
    public SpellCheckArgs distance(int distance) {
        if (distance < 1 || distance > 4) {
            throw new IllegalArgumentException("`distance` must be in [1,4]");
        }
        this.distance = distance;
        return this;
    }

    /**
     * Specifies an inclusion of a custom dictionary named {@code dict}
     *
     * @param dict
     *        the dictionaries
     *
     * @return the current {@code SpellCheckArgs}
     */
    public SpellCheckArgs includes(String... dict) {
        ParameterValidation.doesNotContainNull(notNullOrEmpty(dict, "dict"), "dict");
        Collections.addAll(includes, dict);
        return this;
    }

    /**
     * Specifies an exclusion of a custom dictionary named {@code dict}
     *
     * @param dict
     *        the dictionaries
     *
     * @return the current {@code SpellCheckArgs}
     */
    public SpellCheckArgs excludes(String... dict) {
        ParameterValidation.doesNotContainNull(notNullOrEmpty(dict, "dict"), "dict");
        Collections.addAll(excludes, dict);
        return this;
    }

    /**
     * Selects the dialect version under which to execute the query. If not specified, the query will execute under the
     * default dialect version set during module initial loading.
     *
     * @param dialect
     *        the dialect
     *
     * @return the current {@code SpellCheckArgs}
     */
    public SpellCheckArgs dialect(int dialect) {
        this.dialect = dialect;
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> list = new ArrayList<>();
        if (distance != 0) {
            list.add("DISTANCE");
            list.add(Integer.toString(distance));
        }

        if (!includes.isEmpty() && !excludes.isEmpty()) {
            throw new IllegalArgumentException("Cannot specify both `includes` and `excludes` terms");
        }
        if (!includes.isEmpty()) {
            list.add("TERMS");
            list.add("INCLUDE");
            list.addAll(includes);
        } else if (!excludes.isEmpty()) {
            list.add("TERMS");
            list.add("EXCLUDE");
            list.addAll(excludes);
        }

        if (dialect != -1) {
            list.add("DIALECT");
            list.add(Integer.toString(dialect));
        }

        return list;
    }
}
