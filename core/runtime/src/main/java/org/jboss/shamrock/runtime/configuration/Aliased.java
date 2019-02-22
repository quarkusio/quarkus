package io.quarkus.runtime.configuration;

import java.util.Collection;

/**
 * An enum that has multiple possible textual representations. The representation used for output
 * will always be the result of {@link Object#toString()}, but these additional aliases will be allowed
 * on input as alternative spellings of the enum that implements the method.
 */
public interface Aliased {
    /**
     * Get the aliases for this value.
     *
     * @return the collection of aliases (must not be {@code null})
     */
    Collection<String> getAliases();
}
