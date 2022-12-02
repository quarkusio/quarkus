package io.quarkus.devtools.codestarts;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class CodestartsSelection {
    private final Set<String> names;

    public CodestartsSelection() {
        this.names = new HashSet<>();
    }

    public void addName(final String codestartName) {
        this.names.add(codestartName);
    }

    public void addNames(final Collection<String> codestartNames) {
        this.names.addAll(codestartNames);
    }

    public Set<String> getNames() {
        return names;
    }

}
