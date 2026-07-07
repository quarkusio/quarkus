package io.quarkus.gradle.application;

import java.io.Serializable;

import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildController;
import org.gradle.tooling.model.GradleProject;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecar;
import io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecarValidator;
import io.quarkus.bootstrap.model.gradle.ModelParameter;

record ToolingApplicationModelAction(String mode) implements BuildAction<ApplicationModel>, Serializable {

    @Override
    public ApplicationModel execute(BuildController controller) {
        return controller.getModel(ApplicationModel.class, ModelParameter.class,
                parameter -> parameter.setMode(mode));
    }
}

record ToolingPairedModels(ApplicationModel applicationModel, GradleApplicationModelSidecar sidecar)
        implements
            Serializable {
}

record ToolingPairedModelsAction(String mode) implements BuildAction<ToolingPairedModels>, Serializable {

    @Override
    public ToolingPairedModels execute(BuildController controller) {
        ApplicationModel applicationModel = controller.getModel(ApplicationModel.class, ModelParameter.class,
                parameter -> parameter.setMode(mode));
        GradleApplicationModelSidecar sidecar = controller.getModel(GradleApplicationModelSidecar.class,
                ModelParameter.class, parameter -> parameter.setMode(mode));
        GradleProject targetProject = controller.getModel(GradleProject.class);
        GradleApplicationModelSidecarValidator.validate(
                sidecar,
                GradleApplicationModelSidecar.CURRENT_SCHEMA_VERSION,
                GradleApplicationModelSidecar.Mode.valueOf(mode),
                targetProject.getBuildTreePath(),
                applicationModel);
        return new ToolingPairedModels(applicationModel, sidecar);
    }
}

record ToolingSidecarAction(String mode) implements BuildAction<GradleApplicationModelSidecar>, Serializable {

    @Override
    public GradleApplicationModelSidecar execute(BuildController controller) {
        return controller.getModel(GradleApplicationModelSidecar.class,
                ModelParameter.class, parameter -> parameter.setMode(mode));
    }
}
