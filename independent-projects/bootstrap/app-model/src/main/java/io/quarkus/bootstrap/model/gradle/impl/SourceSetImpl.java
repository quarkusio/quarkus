package io.quarkus.bootstrap.model.gradle.impl;

import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.model.gradle.SourceSet;
import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SourceSetImpl implements SourceSet, Serializable {

    private final PathsCollection sourceDirectories;
    private final PathsCollection resourceDirectories;

    public SourceSetImpl(Set<File> sourceDirectories, Set<File> resourceDirectories) {
        this.sourceDirectories = PathsCollection
                .from(sourceDirectories.stream().map(File::toPath).collect(Collectors.toCollection(LinkedHashSet::new)));
        this.resourceDirectories = PathsCollection
                .from(resourceDirectories.stream().map(File::toPath).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    public SourceSetImpl(Set<File> sourceDirectories) {
        this(sourceDirectories, Collections.emptySet());
    }

    @Override
    public PathsCollection getSourceDirectories() {
        return sourceDirectories;
    }

    @Override
    public PathsCollection getResourceDirectories() {
        return resourceDirectories;
    }

    @Override
    public String toString() {
        return "SourceSetImpl{" +
                "sourceDirectories=" + sourceDirectories.toList().stream().map(Path::toString).collect(Collectors.joining(":"))
                +
                ", resourceDirectories="
                + resourceDirectories.toList().stream().map(Path::toString).collect(Collectors.joining(":")) +
                '}';
    }
}
