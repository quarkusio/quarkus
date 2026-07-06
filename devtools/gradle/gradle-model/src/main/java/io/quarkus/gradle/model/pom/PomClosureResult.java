package io.quarkus.gradle.model.pom;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import io.quarkus.maven.dependency.GAV;

public final class PomClosureResult {

    private final Map<GAV, File> resolvedPoms;
    private final Set<GAV> missingPoms;

    public PomClosureResult(Map<GAV, File> resolvedPoms, Set<GAV> missingPoms) {
        this.resolvedPoms = Map.copyOf(resolvedPoms);
        this.missingPoms = Set.copyOf(missingPoms);
    }

    public static PomClosureResult from(Map<GAV, Optional<File>> pomResults) {
        Map<GAV, File> resolved = new TreeMap<>(PomClosureResult::compare);
        Set<GAV> missing = new TreeSet<>(PomClosureResult::compare);
        pomResults.forEach((gav, file) -> {
            if (file.isPresent()) {
                resolved.put(gav, file.get());
            } else {
                missing.add(gav);
            }
        });
        return new PomClosureResult(resolved, missing);
    }

    public Map<GAV, File> resolvedPoms() {
        return resolvedPoms;
    }

    public Set<GAV> missingPoms() {
        return missingPoms;
    }

    private static int compare(GAV left, GAV right) {
        return left.toString().compareTo(right.toString());
    }
}
