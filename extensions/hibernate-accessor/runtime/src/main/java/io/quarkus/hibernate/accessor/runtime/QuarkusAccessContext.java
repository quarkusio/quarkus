package io.quarkus.hibernate.accessor.runtime;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;

import org.hibernate.accessor.spi.AccessContext;

/**
 * Framework-owned access context used when accessors have to fall back to reflection.
 * <p>
 * The accessor library requires the framework to perform the module and reflection access operations itself,
 * so that they are executed with the framework's own identity, which also works in native executables.
 */
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
