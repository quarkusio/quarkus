package io.quarkus.extest.runtime.config;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.smallrye.config.common.MapBackedConfigSource;

@StaticInitSafe
public class StaticInitSafeConfigSource extends MapBackedConfigSource {
    public static AtomicInteger counter = new AtomicInteger(0);

    public StaticInitSafeConfigSource() {
        super(StaticInitSafeConfigSource.class.getName(), Collections.emptyMap());
        counter.incrementAndGet();
    }
}
