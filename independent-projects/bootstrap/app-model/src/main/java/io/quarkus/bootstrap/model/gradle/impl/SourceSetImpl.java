package io.quarkus.bootstrap.model.gradle.impl;

import io.quarkus.bootstrap.model.gradle.SourceSet;
import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class SourceSetImpl implements SourceSet, Serializable {

    private final Set<File> sourceDirectories;
    private final Set<File> resourceDirectories;

    public SourceSetImpl(Set<File> sourceDirectories, Set<File> resourceDirectories) {
        this.sourceDirectories = sourceDirectories;
        this.resourceDirectories = resourceDirectories;
    }

    public SourceSetImpl(Set<File> sourceDirectories) {
        this(sourceDirectories, Collections.emptySet());
    }

    @Override
    public Set<File> getSourceDirectories() {
        return sourceDirectories;
    }

    @Override
    public Set<File> getResourceDirectories() {
        return resourceDirectories;
    }

    @Override
    public String toString() {
        return "SourceSetImpl{" +
                "sourceDirectories=" + sourceDirectories.stream().map(File::toString).collect(Collectors.joining(":"))
                +
                ", resourceDirectories="
                + resourceDirectories.stream().map(File::toString).collect(Collectors.joining(":")) +
                '}';
    }
}
