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
 * Serializable Tooling API action that fetches an application model and its standalone-plugin sidecar in one
 * invocation.
 * <p>
 * The application model is required. The sidecar is requested optionally so providers that predate the standalone
 * application plugin remain usable. When a sidecar is present, this action also obtains the Tooling API target project,
 * validates that the two models belong to the same request, and returns them as one paired result.
 */
public final class QuarkusToolingModelBuildAction implements BuildAction<QuarkusToolingModelResult>, Serializable {

    private static final long serialVersionUID = -1584626513393139052L;

    private final String mode;

    /**
     * Creates a paired-model action.
     *
     * @param mode mode sent verbatim to both model providers; a standalone sidecar provider requires the exact name of
     *        a {@link GradleApplicationModelSidecar.Mode} value
     */
    public QuarkusToolingModelBuildAction(String mode) {
        this.mode = mode;
    }

    /**
     * Fetches and correlates the model pair.
     * <p>
     * If no sidecar provider is present, this method returns the application model as
     * {@link ProviderKind#UNMARKED_COMPATIBILITY} without requesting target-project metadata.
     *
     * @param controller controller for the current Tooling API invocation
     * @return application model together with its validated provider classification and optional sidecar
     * @throws io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecarMismatchException if a returned sidecar
     *         does not match the request or application model
     * @throws IllegalArgumentException if a standalone provider is used with an unsupported mode name
     */
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
