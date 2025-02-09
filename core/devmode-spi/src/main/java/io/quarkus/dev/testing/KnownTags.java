package io.quarkus.dev.testing;

import java.util.Collections;
import java.util.Set;

public class KnownTags {

    private static volatile Set<String> knownTags = Set.of();

    public static Set<String> getKnownTags() {
        return knownTags;
    }

    public static void setKnownTags(Set<String> knownTag) {
        knownTags = Collections.unmodifiableSet(knownTag);
    }

    private KnownTags() {
    }
}
