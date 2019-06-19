package io.quarkus.cli.commands;

import java.util.Set;

import io.quarkus.dependencies.Extension;

public class SelectionResult {

    private final Set<Extension> extensions;
    private final boolean matches;

    public SelectionResult(Set<Extension> extensions, boolean matches) {
        this.extensions = extensions;
        this.matches = matches;
    }

    public Set<Extension> getExtensions() {
        return extensions;
    }

    public boolean matches() {
        return matches;
    }

    public Extension getMatch() {
        if (matches) {
            if (extensions.isEmpty() || extensions.size() > 1) {
                throw new IllegalStateException("Invalid selection result");
            }
            return extensions.iterator().next();
        }
        return null;
    }
}
