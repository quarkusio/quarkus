package io.quarkus.gradle.application.internal.tooling;

import java.util.Objects;
import java.util.function.BiFunction;

import org.gradle.api.Project;
import org.gradle.tooling.provider.model.ParameterizedToolingModelBuilder;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecar;
import io.quarkus.bootstrap.model.gradle.ModelParameter;
import io.quarkus.bootstrap.model.gradle.impl.ModelParameterImpl;
import io.quarkus.gradle.application.internal.modelgen.ApplicationModelResolutionViews;
import io.quarkus.runtime.LaunchMode;

/**
 * Tooling API provider for the Gradle-specific sidecar that accompanies the
 * standalone application plugin's {@link ApplicationModel}.
 */
public final class GradleApplicationModelSidecarBuilder implements ParameterizedToolingModelBuilder<ModelParameter> {

    private final ApplicationModelResolutionViews resolutionViews;
    private final BiFunction<Project, LaunchMode, ApplicationModel> applicationModelProvider;

    public GradleApplicationModelSidecarBuilder(ApplicationModelResolutionViews resolutionViews,
            BiFunction<Project, LaunchMode, ApplicationModel> applicationModelProvider) {
        this.resolutionViews = Objects.requireNonNull(resolutionViews, "resolutionViews");
        this.applicationModelProvider = Objects.requireNonNull(applicationModelProvider, "applicationModelProvider");
    }

    @Override
    public boolean canBuild(String modelName) {
        return GradleApplicationModelSidecar.class.getName().equals(modelName);
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
        ApplicationModel applicationModel = applicationModelProvider.apply(project, mode);
        return new GradleApplicationModelSidecarCollector(project, resolutionViews.forMode(mode))
                .collect(mode, applicationModel);
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
}
