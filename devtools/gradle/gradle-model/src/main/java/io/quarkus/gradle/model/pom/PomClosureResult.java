package io.quarkus.gradle.model.pom;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import io.quarkus.maven.dependency.GAV;

/**
 * Snapshot of all POM lookup results discovered while preparing effective Maven models.
 * <p>
 * When produced by the POM-closure workflow, the resolved map includes selected module POMs and any parents or imported
 * BOMs requested while building their effective models. Missing coordinates are retained separately so later consumers
 * do not repeatedly attempt the same lookups. The snapshot is local build data: resolved entries refer to concrete
 * files and are not relocatable.
 */
public final class PomClosureResult {

    private final Map<GAV, File> resolvedPoms;
    private final Set<GAV> missingPoms;

    /**
     * Creates a defensive snapshot of resolved and missing POMs.
     *
     * @param resolvedPoms resolved local POM files by coordinates
     * @param missingPoms coordinates known to be unavailable
     */
    public PomClosureResult(Map<GAV, File> resolvedPoms, Set<GAV> missingPoms) {
        this.resolvedPoms = Map.copyOf(resolvedPoms);
        this.missingPoms = Set.copyOf(missingPoms);
    }

    /**
     * Splits cached optional lookup results into resolved and missing collections.
     *
     * @param pomResults resolved and known-missing lookup results
     * @return a defensive closure snapshot
     */
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

    /** @return the known resolved POM files by coordinates */
    public Map<GAV, File> resolvedPoms() {
        return resolvedPoms;
    }

    /** @return coordinates known to be missing */
    public Set<GAV> missingPoms() {
        return missingPoms;
    }

    private static int compare(GAV left, GAV right) {
        return left.toString().compareTo(right.toString());
    }
}
