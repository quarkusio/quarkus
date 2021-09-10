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
    private int flags;

    public DependencyImpl(String name, String groupId, String version, File path, String scope, String type,
            String classifier, int... flags) {
        this(name, groupId, version, scope, type, classifier, flags);
        this.paths.add(path);
    }

    public DependencyImpl(String name, String groupId, String version, String scope, String type, String classifier,
            int... flags) {
        this.name = name;
        this.groupId = groupId;
        this.version = version;
        this.scope = scope;
        this.type = type;
        this.classifier = classifier;
        int allFlags = 0;
        for (int flag : flags) {
            allFlags |= flag;
        }
        this.flags = allFlags;
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
    public int getFlags() {
        return flags;
    }

    public void setFlag(int flag) {
        flags |= flag;
    }

    public boolean isFlagSet(int flag) {
        return (flags & flag) > 0;
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
    public int hashCode() {
        return Objects.hash(classifier, flags, groupId, name, paths, scope, type, version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DependencyImpl other = (DependencyImpl) obj;
        return Objects.equals(classifier, other.classifier) && flags == other.flags
                && Objects.equals(groupId, other.groupId) && Objects.equals(name, other.name)
                && Objects.equals(paths, other.paths) && Objects.equals(scope, other.scope)
                && Objects.equals(type, other.type) && Objects.equals(version, other.version);
    }
}
