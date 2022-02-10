
package io.quarkus.gradle.tasks;

import java.util.List;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;

public class QuarkusBuildConfiguration {

    private final Project project;

    private ListProperty<String> forcedDependencies;
    private MapProperty<String, String> forcedProperties;

    public QuarkusBuildConfiguration(Project project) {
        this.project = project;
        forcedDependencies = project.getObjects().listProperty(String.class);
        forcedProperties = project.getObjects().mapProperty(String.class, String.class);
    }

    public ListProperty<String> getForcedDependencies() {
        return forcedDependencies;
    }

    public void setForcedDependencies(List<String> forcedDependencies) {
        this.forcedDependencies.addAll(forcedDependencies);
    }

    public MapProperty<String, String> getForcedProperties() {
        return forcedProperties;
    }

    public void setForcedProperties(Map<String, String> forcedProperties) {
        this.forcedProperties.putAll(forcedProperties);
    }
}
