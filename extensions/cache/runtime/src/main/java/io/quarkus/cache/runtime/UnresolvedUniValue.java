package io.quarkus.cache.runtime;

/**
 * This value acts as a placeholder in the cache. It will be eventually replaced by the item emitted by the
 * {@link io.smallrye.mutiny.Uni Uni} when it has been resolved.
 */
public class UnresolvedUniValue {

    public static final UnresolvedUniValue INSTANCE = new UnresolvedUniValue();

    private UnresolvedUniValue() {
    }
}
