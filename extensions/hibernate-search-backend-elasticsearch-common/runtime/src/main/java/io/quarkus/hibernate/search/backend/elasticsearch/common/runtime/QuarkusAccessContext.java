package io.quarkus.hibernate.search.backend.elasticsearch.common.runtime;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;

import org.hibernate.accessor.spi.AccessContext;

public class QuarkusAccessContext implements AccessContext {
    private final MethodHandles.Lookup lookup;

    public QuarkusAccessContext() {
        this.lookup = MethodHandles.lookup();
    }

    @Override
    public MethodHandles.Lookup lookup() {
        return lookup;
    }

    @Override
    public void ensureReads(Module target) {
        lookup.lookupClass().getModule().addReads(target);
    }

    @Override
    public void makeAccessible(AccessibleObject member) {
        member.setAccessible(true);
    }
}
