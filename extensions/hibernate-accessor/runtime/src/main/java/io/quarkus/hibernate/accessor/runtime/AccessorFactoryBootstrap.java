package io.quarkus.hibernate.accessor.runtime;

import org.hibernate.accessor.AccessorFactory;

/**
 * Runtime-visible entry point implemented alongside the generated application accessor factory.
 * The generated implementation calls the factory directly and preserves its singleton initialization semantics.
 */
public interface AccessorFactoryBootstrap {
    /**
     * Returns the generated factory singleton. Only the first call initializes it and selects its fallback.
     *
     * @param fallback reflection fallback for the fallback strategy; ignored by the generated-only strategy
     * @return the factory initialized by this or an earlier call
     */
    AccessorFactory create(AccessorFactory fallback);
}
