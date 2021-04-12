package io.quarkus.bootstrap.model.gradle.impl;

import io.quarkus.bootstrap.model.gradle.SourceSet;
import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SourceSetImpl implements SourceSet, Serializable {

    private Set<File> sourceDirectories = new HashSet<>();
    private File resourceDirectory;

    public SourceSetImpl(Set<File> sourceDirectories, File resourceDirectory) {
        this.sourceDirectories.addAll(sourceDirectories);
        this.resourceDirectory = resourceDirectory;
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
