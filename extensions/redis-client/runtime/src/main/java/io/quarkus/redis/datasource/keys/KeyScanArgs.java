package io.quarkus.redis.datasource.keys;

import java.util.List;

import io.quarkus.redis.datasource.ScanArgs;

/**
 * Represents the {@code scan} commands flags.
 */
public class KeyScanArgs extends ScanArgs {

    private RedisValueType type;

    /**
     * You can use the TYPE option to ask SCAN to only return objects that match a given type, allowing you to iterate
     * through the database looking for keys of a specific type.
     *
     * @param type
     *        the type value
     *
     * @return the current {@code KeyScanArgs}
     **/
    public KeyScanArgs type(RedisValueType type) {
        this.type = type;
        return this;
    }

    /**
     * Sets the max number of items in each batch. The default value is 10.
     *
     * @param count
     *        the number of item, must be strictly positive
     *
     * @return the current {@code ScanArgs}
     */
    public KeyScanArgs count(long count) {
        super.count(count);
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
    public KeyScanArgs match(String pattern) {
        super.match(pattern);
        return this;
    }

    public List<String> toArgs() {
        List<String> list = super.toArgs();
        if (type != null) {
            list.add("TYPE");
            list.add(type.name());
        }
        return list;
    }
}
