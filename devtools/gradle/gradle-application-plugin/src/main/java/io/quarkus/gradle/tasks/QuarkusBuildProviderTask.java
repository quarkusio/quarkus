
package io.quarkus.gradle.tasks;

import java.util.Map;

import javax.inject.Inject;

import org.gradle.api.tasks.TaskAction;

public abstract class QuarkusBuildProviderTask extends QuarkusTask {

    private final QuarkusBuildConfiguration buildConfiguration;

    @Inject
    public QuarkusBuildProviderTask(QuarkusBuildConfiguration buildConfiguration, String description) {
        super(description);
        this.buildConfiguration = buildConfiguration;
    }

    public abstract Map<String, String> forcedProperties();

    @TaskAction
    public void configureBuild() {
        buildConfiguration.setForcedProperties(forcedProperties());
    }
}
