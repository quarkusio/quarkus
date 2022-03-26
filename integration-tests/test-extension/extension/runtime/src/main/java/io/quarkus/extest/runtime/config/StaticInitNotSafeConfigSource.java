package io.quarkus.extest.runtime.config;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import io.smallrye.config.common.MapBackedConfigSource;

public class StaticInitNotSafeConfigSource extends MapBackedConfigSource {
    public static AtomicInteger counter = new AtomicInteger(0);

    public StaticInitNotSafeConfigSource() {
        super(StaticInitNotSafeConfigSource.class.getName(), Collections.emptyMap());
        counter.incrementAndGet();
    }
}
