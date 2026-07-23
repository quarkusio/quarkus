package io.quarkus.bootstrap.resolver;

import java.io.Serializable;

import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildController;
import org.gradle.tooling.model.GradleProject;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecar;
import io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecarValidator;
import io.quarkus.bootstrap.model.gradle.ModelParameter;
import io.quarkus.bootstrap.resolver.QuarkusToolingModelResult.ProviderKind;

/**
 * Fetches an application model and the standalone-plugin ownership marker in
 * one Tooling API invocation.
 */
public final class QuarkusToolingModelBuildAction implements BuildAction<QuarkusToolingModelResult>, Serializable {

    private static final long serialVersionUID = -1584626513393139052L;

    private final String mode;

    public QuarkusToolingModelBuildAction(String mode) {
        this.mode = mode;
    }

    @Override
    public QuarkusToolingModelResult execute(BuildController controller) {
        ApplicationModel applicationModel = controller.getModel(ApplicationModel.class, ModelParameter.class,
                parameter -> parameter.setMode(mode));
        GradleApplicationModelSidecar sidecar = controller.findModel(GradleApplicationModelSidecar.class,
                ModelParameter.class, parameter -> parameter.setMode(mode));
        if (sidecar == null) {
            return new QuarkusToolingModelResult(applicationModel, ProviderKind.UNMARKED_COMPATIBILITY, null);
        }

        final GradleProject targetProject = controller.getModel(GradleProject.class);
        GradleApplicationModelSidecarValidator.validate(sidecar,
                GradleApplicationModelSidecar.CURRENT_SCHEMA_VERSION,
                GradleApplicationModelSidecar.Mode.valueOf(mode),
                targetProject.getBuildTreePath(),
                applicationModel);
        return new QuarkusToolingModelResult(applicationModel, ProviderKind.STANDALONE_APPLICATION, sidecar);
    }
}
