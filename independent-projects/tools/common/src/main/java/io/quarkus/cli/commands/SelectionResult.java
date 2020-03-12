package io.quarkus.cli.commands;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import io.quarkus.dependencies.Extension;

public class SelectionResult implements Iterable<Extension> {

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

    @Override
    public Iterator<Extension> iterator() {
        if (matches) {
            return extensions.iterator();
        }
        return Collections.emptyIterator();
    }
}
