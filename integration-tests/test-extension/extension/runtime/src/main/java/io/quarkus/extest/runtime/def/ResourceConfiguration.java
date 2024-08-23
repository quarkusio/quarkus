package io.quarkus.extest.runtime.def;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public interface ResourceConfiguration {

    default Set<String> getNamespaces() {
        HashSet<String> set = new HashSet<>();
        set.add("foo");
        set.add("bar");
        return set;
    }

    default Optional<Long> getInformerListLimit() {
        return Optional.empty();
    }
}
