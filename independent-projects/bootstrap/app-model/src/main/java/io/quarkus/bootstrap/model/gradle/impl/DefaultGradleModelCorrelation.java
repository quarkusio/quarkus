package io.quarkus.bootstrap.model.gradle.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecar;
import io.quarkus.bootstrap.model.gradle.GradleModelCorrelation;

public final class DefaultGradleModelCorrelation implements GradleModelCorrelation {

    private static final long serialVersionUID = -4767943016645702070L;

    private final int schemaVersion;
    private final GradleApplicationModelSidecar.Mode mode;
    private final String targetBuildTreePath;
    private final List<String> canonicalGraphFacts;

    public DefaultGradleModelCorrelation(int schemaVersion, GradleApplicationModelSidecar.Mode mode,
            String targetBuildTreePath, Collection<String> canonicalGraphFacts) {
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("schemaVersion must be greater than zero");
        }
        this.schemaVersion = schemaVersion;
        this.mode = Objects.requireNonNull(mode, "mode");
        this.targetBuildTreePath = Objects.requireNonNull(targetBuildTreePath, "targetBuildTreePath");
        final List<String> facts = new ArrayList<>(canonicalGraphFacts);
        for (String fact : facts) {
            Objects.requireNonNull(fact, "canonicalGraphFacts contains null");
        }
        facts.sort(Comparator.naturalOrder());
        this.canonicalGraphFacts = List.copyOf(facts);
    }

    @Override
    public int getSchemaVersion() {
        return schemaVersion;
    }

    @Override
    public GradleApplicationModelSidecar.Mode getMode() {
        return mode;
    }

    @Override
    public String getTargetBuildTreePath() {
        return targetBuildTreePath;
    }

    @Override
    public List<String> getCanonicalGraphFacts() {
        return canonicalGraphFacts;
    }
}
