package io.quarkus.gradle.application.internal.tooling;

import java.util.Objects;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.tooling.provider.model.ParameterizedToolingModelBuilder;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.gradle.ModelParameter;
import io.quarkus.bootstrap.model.gradle.impl.ModelParameterImpl;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelResolutionViews;
import io.quarkus.gradle.application.internal.modelgen.StandaloneApplicationModelAssembler;
import io.quarkus.runtime.LaunchMode;

/**
 * Standalone application plugin's Tooling API provider for the Quarkus
 * application model.
 */
public final class GradleApplicationModelBuilder implements ParameterizedToolingModelBuilder<ModelParameter> {

    private static final String ENABLE_DECLARED_DEPENDENCY_COLLECTOR = "enableDeclaredDependencyCollector";

    private final ApplicationModelResolutionViews resolutionViews;

    public GradleApplicationModelBuilder(ApplicationModelResolutionViews resolutionViews) {
        this.resolutionViews = Objects.requireNonNull(resolutionViews, "resolutionViews");
    }

    @Override
    public boolean canBuild(String modelName) {
        return ApplicationModel.class.getName().equals(modelName);
    }

    @Override
    public Class<ModelParameter> getParameterType() {
        return ModelParameter.class;
    }

    @Override
    public Object buildAll(String modelName, Project project) {
        ModelParameterImpl parameter = new ModelParameterImpl();
        parameter.setMode(LaunchMode.DEVELOPMENT.name());
        return buildAll(modelName, parameter, project);
    }

    @Override
    public Object buildAll(String modelName, ModelParameter parameter, Project project) {
        if (!canBuild(modelName)) {
            throw new IllegalArgumentException("Unsupported tooling model " + modelName);
        }
        LaunchMode mode = launchMode(parameter);
        return build(project, mode);
    }

    public ApplicationModel build(Project project, LaunchMode mode) {
        rejectDeclaredDependencyCollector(project);
        return StandaloneApplicationModelAssembler.assemble(project, resolutionViews, mode,
                workspaceDiscovery(project, mode));
    }

    private static LaunchMode launchMode(ModelParameter parameter) {
        Objects.requireNonNull(parameter, "parameter");
        String mode = Objects.requireNonNull(parameter.getMode(), "ModelParameter.mode");
        try {
            return LaunchMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported Quarkus launch mode " + mode, e);
        }
    }

    private static void rejectDeclaredDependencyCollector(Project project) {
        String value = project.getProviders().gradleProperty(ENABLE_DECLARED_DEPENDENCY_COLLECTOR).getOrNull();
        if (value != null && Boolean.parseBoolean(value)) {
            throw new GradleException("The standalone 'io.quarkus.application' Tooling API provider does not support "
                    + "'enableDeclaredDependencyCollector=true'. Remove that property or use the legacy 'io.quarkus' "
                    + "plugin when effective-POM declared-dependency enrichment is required.");
        }
    }

    private static boolean workspaceDiscovery(Project project, LaunchMode mode) {
        if (mode == LaunchMode.DEVELOPMENT || mode == LaunchMode.TEST) {
            return true;
        }
        String systemProperty = project.getProviders()
                .systemProperty(BootstrapConstants.QUARKUS_BOOTSTRAP_WORKSPACE_DISCOVERY)
                .getOrNull();
        if (systemProperty != null && Boolean.parseBoolean(systemProperty)) {
            return true;
        }
        String gradleProperty = project.getProviders()
                .gradleProperty(BootstrapConstants.QUARKUS_BOOTSTRAP_WORKSPACE_DISCOVERY)
                .getOrNull();
        return gradleProperty != null && Boolean.parseBoolean(gradleProperty);
    }
}
