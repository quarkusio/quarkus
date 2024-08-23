package io.quarkus.redis.datasource.cuckoo;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.RedisCommandExtraArguments;

public class CfInsertArgs implements RedisCommandExtraArguments {

    private long capacity;

    private boolean nocreate;

    /**
     * Specifies the desired capacity of the new filter, if this filter does not exist yet. If the filter already exists,
     * then this parameter is ignored. If the filter does not exist yet and this parameter is not specified, then the
     * filter is created with the module-level default capacity which is 1024.
     *
     * @param capacity the capacity
     * @return the current {@link CfInsertArgs}
     */
    public CfInsertArgs capacity(long capacity) {
        this.capacity = capacity;
        return this;
    }

    /**
     * If specified, prevents automatic filter creation if the filter does not exist. Instead, an error is returned
     * if the filter does not already exist. This option is mutually exclusive with {@code CAPACITY}.
     *
     * @return the current {@link CfInsertArgs}
     */
    public CfInsertArgs nocreate() {
        this.nocreate = true;
        return this;
    }

    @Override
    public List<Object> toArgs() {
        List<Object> list = new ArrayList<>();
        if (capacity > 0) {
            list.add("CAPACITY");
            list.add(Long.toString(capacity));
        }
        if (nocreate) {
            list.add("NOCREATE");
        }

        return list;
    }
}
