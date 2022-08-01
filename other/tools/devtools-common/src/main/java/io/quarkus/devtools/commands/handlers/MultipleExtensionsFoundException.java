package io.quarkus.devtools.commands.handlers;

import io.quarkus.registry.catalog.Extension;
import java.util.Collection;
import java.util.Objects;

/**
 * Thrown when multiple extensions are found for a given installation plan
 */
public class MultipleExtensionsFoundException extends RuntimeException {

    private final String keyword;
    private final Collection<Extension> extensions;

    public MultipleExtensionsFoundException(String keyword, Collection<Extension> extensions) {
        this.keyword = Objects.requireNonNull(keyword, "Keyword must not be null");
        this.extensions = Objects.requireNonNull(extensions, "Extensions should not be null");
    }

    public String getKeyword() {
        return keyword;
    }

    public Collection<Extension> getExtensions() {
        return extensions;
    }
}
