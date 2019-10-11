package io.quarkus.it.infinispan.embedded;

import java.util.concurrent.Executor;

import org.infinispan.persistence.spi.AdvancedLoadWriteStore;
import org.infinispan.persistence.spi.InitializationContext;

// Here to test a custom cache loader configured via XML
public class TestCacheLoader implements AdvancedLoadWriteStore {
    @Override
    public int size() {
        return 0;
    }

    @Override
    public void clear() {

    }

    @Override
    public void purge(Executor threadPool, PurgeListener listener) {

    }

    @Override
    public void init(InitializationContext ctx) {

    }

    @Override
    public boolean delete(Object key) {
        return false;
    }

    @Override
    public boolean contains(Object key) {
        return false;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}
