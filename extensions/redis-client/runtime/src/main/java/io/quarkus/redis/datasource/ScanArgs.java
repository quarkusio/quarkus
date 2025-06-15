package io.quarkus.redis.datasource;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the {@code scan} commands flags.
 */
public class ScanArgs {
    private long count = -1;
    private String match;

    /**
     * Sets the max number of items in each batch. The default value is 10.
     *
     * @param count
     *        the number of item, must be strictly positive
     *
     * @return the current {@code ScanArgs}
     */
    public ScanArgs count(long count) {
        if (count <= 0) {
            throw new IllegalArgumentException("`count` must be strictly positive");
        }
        this.count = count;
        return this;
    }

    /**
     * Sets a {@code MATCH} pattern
     *
     * @param pattern
     *        the pattern, must not be {@code null}
     *
     * @return the current {@code ScanArgs}
     */
    public ScanArgs match(String pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("`pattern` must not be `null`");
        }
        this.match = pattern;
        return this;
    }

    public List<String> toArgs() {
        List<String> args = new ArrayList<>();
        if (this.count != -1) {
            args.add("COUNT");
            args.add(Long.toString(this.count));
        }
        if (this.match != null) {
            args.add("MATCH");
            args.add(this.match);
        }
        return args;
    }
}
