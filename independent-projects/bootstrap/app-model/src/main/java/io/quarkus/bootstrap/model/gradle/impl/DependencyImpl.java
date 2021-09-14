package io.quarkus.bootstrap.model.gradle.impl;

import io.quarkus.bootstrap.model.gradle.Dependency;
import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class DependencyImpl implements Dependency, Serializable {

    private final String name;
    private final String groupId;
    private final String version;
    private final String classifier;
    private final Set<File> paths = new HashSet<>();
    private final String scope;
    private final String type;

    public DependencyImpl(String name, String groupId, String version, File path, String scope, String type,
            String classifier) {
        this(name, groupId, version, scope, type, classifier);
        this.paths.add(path);
    }

    public DependencyImpl(String name, String groupId, String version, String scope, String type, String classifier) {
        this.name = name;
        this.groupId = groupId;
        this.version = version;
        this.scope = scope;
        this.type = type;
        this.classifier = classifier;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    @Override
    public Set<File> getPaths() {
        return paths;
    }

    public void addPath(File path) {
        this.paths.add(path);
    }

    @Override
    public String getScope() {
        return scope;
    }

    @Override
    public String toString() {
        return "DependencyImpl{" +
                "name='" + name + '\'' +
                ", groupId='" + groupId + '\'' +
                ", version='" + version + '\'' +
                ", type='" + type + '\'' +
                ", path=" + paths +
                ", classifier= " + classifier +
                ", scope='" + scope + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DependencyImpl that = (DependencyImpl) o;
        return name.equals(that.name) &&
                groupId.equals(that.groupId) &&
                version.equals(that.version) &&
                paths.equals(that.paths) &&
                scope.equals(that.scope) &&
                type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, groupId, version, paths, scope, type);
    }
}
