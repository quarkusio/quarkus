package io.quarkus.bootstrap.model.gradle.impl;

import io.quarkus.bootstrap.model.gradle.SourceSet;
import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SourceSetImpl implements SourceSet, Serializable {

    private final Set<File> sourceDirectories = new HashSet<>();
    // Use a LinkedHashSet to keep the original order
    private final Set<File> resourceDirectories = new LinkedHashSet<>();

    public SourceSetImpl(Set<File> sourceDirectories, Set<File> resourceDirectories) {
        this.sourceDirectories.addAll(sourceDirectories);
        this.resourceDirectories.addAll(resourceDirectories);
    }

    public SourceSetImpl(Set<File> sourceDirectories) {
        this.sourceDirectories.addAll(sourceDirectories);
    }

    public void addSourceDirectories(Set<File> files) {
        sourceDirectories.addAll(files);
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
                "sourceDirectories=" + sourceDirectories.stream().map(File::getPath).collect(Collectors.joining(":")) +
                ", resourceDirectories=" + resourceDirectories.stream().map(File::getPath).collect(Collectors.joining(":")) +
                '}';
    }
}
