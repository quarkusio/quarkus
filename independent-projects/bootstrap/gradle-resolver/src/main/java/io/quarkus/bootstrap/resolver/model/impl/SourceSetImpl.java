package io.quarkus.bootstrap.resolver.model.impl;

import io.quarkus.bootstrap.resolver.model.SourceSet;
import java.io.File;
import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

public class SourceSetImpl implements SourceSet, Serializable {

    private final Set<File> sourceDirectories;
    private final File resourceDirectory;

    public SourceSetImpl(Set<File> sourceDirectories, File resourceDirectory) {
        this.sourceDirectories = sourceDirectories;
        this.resourceDirectory = resourceDirectory;
    }

    @Override
    public Set<File> getSourceDirectories() {
        return sourceDirectories;
    }

    @Override
    public File getResourceDirectory() {
        return resourceDirectory;
    }

    @Override
    public String toString() {
        return "SourceSetImpl{" +
                "sourceDirectories=" + sourceDirectories.stream().map(File::getPath).collect(Collectors.joining(":")) +
                ", resourceDirectory=" + resourceDirectory +
                '}';
    }
}
