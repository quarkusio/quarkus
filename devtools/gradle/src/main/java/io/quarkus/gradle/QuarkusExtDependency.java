package io.quarkus.gradle;

import org.gradle.api.artifacts.Dependency;
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
    public ExternalModuleDependency copy() {
        return new QuarkusExtDependency(group, name, version, configuration);
    }

    @Override
    public boolean contentEquals(Dependency arg0) {
        return true;
    }
}
