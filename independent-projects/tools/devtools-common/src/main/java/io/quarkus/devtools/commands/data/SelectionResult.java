package io.quarkus.devtools.commands.data;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import io.quarkus.registry.catalog.Extension;

public class SelectionResult implements Iterable<Extension> {

    private final Collection<Extension> extensions;
    private final boolean matches;

    public SelectionResult(Collection<Extension> extensions, boolean matches) {
        this.extensions = extensions;
        this.matches = matches;
    }

    public Collection<Extension> getExtensions() {
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
