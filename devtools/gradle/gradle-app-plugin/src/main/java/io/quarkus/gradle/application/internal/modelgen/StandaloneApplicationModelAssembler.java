package io.quarkus.gradle.application.internal.modelgen;

import org.gradle.api.Project;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.runtime.LaunchMode;

/**
 * Narrow Gradle-facing facade over the pure standalone-plugin application
 * model assembler.
 */
public final class StandaloneApplicationModelAssembler {

    private StandaloneApplicationModelAssembler() {
    }

    public static ApplicationModel assemble(Project project, ApplicationModelResolutionViews resolutionViews,
            LaunchMode mode, boolean workspaceDiscovery) {
        ApplicationModelInputs inputs = new GradleApplicationModelInputCollector()
                .collect(project, resolutionViews, mode, workspaceDiscovery);
        return new ApplicationModelAssembler().assemble(inputs).build();
    }
}
