package io.quarkus.gradle;

import java.util.Set;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier;
import org.gradle.api.internal.artifacts.dependencies.AbstractExternalModuleDependency;

public class QuarkusExtDependency extends AbstractExternalModuleDependency {

    private final String group;
    private final String name;
    private final String version;
    private final String configuration;

    public QuarkusExtDependency(String group, String name, String version, String configuration) {
        super(DefaultModuleIdentifier.newId(group, name), version, configuration);
        this.group = group;
        this.name = name;
        this.version = version;
        this.configuration = configuration;
    }

    @Override
    public Set<DependencyArtifact> getArtifacts() {
        return super.getArtifacts();
    }

    @Override
    public ExternalModuleDependency copy() {
        QuarkusExtDependency copy = new QuarkusExtDependency(group, name, version, configuration);
        final Set<DependencyArtifact> artifacts = getArtifacts();
        for (DependencyArtifact a : artifacts) {
            copy.addArtifact(a);
        }
        return copy;
    }

    @Override
    public boolean contentEquals(Dependency arg0) {
        new Exception("contentEquals " + arg0).printStackTrace();
        return true;
    }
}
