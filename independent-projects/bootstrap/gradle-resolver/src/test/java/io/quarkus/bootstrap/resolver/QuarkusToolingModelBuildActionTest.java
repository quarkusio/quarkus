package io.quarkus.bootstrap.resolver;

import static io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecar.Mode.DEVELOPMENT;
import static io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecarMismatchException.Dimension.TARGET;
import static io.quarkus.bootstrap.resolver.QuarkusToolingModelResult.ProviderKind.STANDALONE_APPLICATION;
import static io.quarkus.bootstrap.resolver.QuarkusToolingModelResult.ProviderKind.UNMARKED_COMPATIBILITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Action;
import org.gradle.tooling.BuildController;
import org.gradle.tooling.model.GradleProject;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecar;
import io.quarkus.bootstrap.model.gradle.GradleApplicationModelSidecarMismatchException;
import io.quarkus.bootstrap.model.gradle.GradleModelCorrelationSupport;
import io.quarkus.bootstrap.model.gradle.ModelParameter;
import io.quarkus.bootstrap.model.gradle.impl.DefaultGradleApplicationModelSidecar;
import io.quarkus.bootstrap.model.gradle.impl.DefaultGradleModelCorrelation;
import io.quarkus.bootstrap.model.gradle.impl.DefaultGradleProjectIdentity;
import io.quarkus.bootstrap.model.gradle.impl.ModelParameterImpl;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;

class QuarkusToolingModelBuildActionTest {

    @Test
    void preservesUnmarkedCompatibilityWhenTheSidecarIsAbsent() {
        final var requestedModes = new ArrayList<String>();
        final var result = new QuarkusToolingModelBuildAction("DEVELOPMENT")
                .execute(controller(applicationModel(), null, null, requestedModes));

        assertThat(result.getProviderKind()).isEqualTo(UNMARKED_COMPATIBILITY);
        assertThat(result.getSidecar()).isNull();
        assertThat(requestedModes).containsExactly("DEVELOPMENT", "DEVELOPMENT");
    }

    @Test
    void returnsAndSerializesAValidatedStandalonePair() throws Exception {
        final ApplicationModel model = applicationModel();
        final GradleApplicationModelSidecar sidecar = sidecar(model, ":");

        final QuarkusToolingModelResult result = new QuarkusToolingModelBuildAction("DEVELOPMENT")
                .execute(controller(model, sidecar, ":", new ArrayList<>()));
        final QuarkusToolingModelResult copy = roundTrip(result);

        assertThat(copy.getProviderKind()).isEqualTo(STANDALONE_APPLICATION);
        assertThat(copy.getSidecar()).isNotNull();
        assertThat(copy.getApplicationModel().getAppArtifact().toGACTVString())
                .isEqualTo("org.acme:application::jar:1.0.0");
    }

    @Test
    void validatesTheSidecarAgainstTheToolingRequestTarget() {
        final ApplicationModel model = applicationModel();
        final GradleApplicationModelSidecar sidecar = sidecar(model, ":other");

        assertThatExceptionOfType(GradleApplicationModelSidecarMismatchException.class)
                .isThrownBy(() -> new QuarkusToolingModelBuildAction("DEVELOPMENT")
                        .execute(controller(model, sidecar, ":", new ArrayList<>())))
                .satisfies(failure -> assertThat(failure.getDimension()).isEqualTo(TARGET));
    }

    @Test
    void enforcesProviderAndSidecarInvariants() {
        final ApplicationModel model = applicationModel();
        final GradleApplicationModelSidecar sidecar = sidecar(model, ":");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new QuarkusToolingModelResult(model, STANDALONE_APPLICATION, null))
                .withMessageContaining("requires a Gradle sidecar");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new QuarkusToolingModelResult(model, UNMARKED_COMPATIBILITY, sidecar))
                .withMessageContaining("cannot carry a Gradle sidecar");
    }

    private static ApplicationModel applicationModel() {
        return new ApplicationModelBuilder()
                .setAppArtifact(ResolvedDependencyBuilder.newInstance()
                        .setCoords(ArtifactCoords.jar("org.acme", "application", "1.0.0")))
                .build();
    }

    private static GradleApplicationModelSidecar sidecar(ApplicationModel model, String targetBuildTreePath) {
        final var target = new DefaultGradleProjectIdentity(":", ":", targetBuildTreePath);
        final var correlation = new DefaultGradleModelCorrelation(
                GradleApplicationModelSidecar.CURRENT_SCHEMA_VERSION, DEVELOPMENT, targetBuildTreePath,
                GradleModelCorrelationSupport.canonicalGraphFacts(model));
        return new DefaultGradleApplicationModelSidecar(correlation, target, List.of());
    }

    @SuppressWarnings("unchecked")
    private static BuildController controller(ApplicationModel model, GradleApplicationModelSidecar sidecar,
            String targetBuildTreePath, List<String> requestedModes) {
        return (BuildController) Proxy.newProxyInstance(
                QuarkusToolingModelBuildActionTest.class.getClassLoader(),
                new Class<?>[] { BuildController.class },
                (proxy, method, arguments) -> {
                    if ("getModel".equals(method.getName()) && arguments.length == 3) {
                        requestedModes.add(configuredMode(arguments));
                        return model;
                    }
                    if ("findModel".equals(method.getName()) && arguments.length == 3) {
                        requestedModes.add(configuredMode(arguments));
                        return sidecar;
                    }
                    if ("getModel".equals(method.getName()) && arguments.length == 1
                            && arguments[0] == GradleProject.class) {
                        if (targetBuildTreePath == null) {
                            throw new AssertionError("The unmarked compatibility path queried target-project metadata");
                        }
                        return gradleProject(targetBuildTreePath);
                    }
                    throw new UnsupportedOperationException(method.toString());
                });
    }

    @SuppressWarnings("unchecked")
    private static String configuredMode(Object[] arguments) {
        final ModelParameter parameter = new ModelParameterImpl();
        ((Action<ModelParameter>) arguments[2]).execute(parameter);
        return parameter.getMode();
    }

    private static GradleProject gradleProject(String buildTreePath) {
        return (GradleProject) Proxy.newProxyInstance(
                QuarkusToolingModelBuildActionTest.class.getClassLoader(),
                new Class<?>[] { GradleProject.class },
                (proxy, method, arguments) -> {
                    if ("getBuildTreePath".equals(method.getName())) {
                        return buildTreePath;
                    }
                    throw new UnsupportedOperationException(method.toString());
                });
    }

    private static QuarkusToolingModelResult roundTrip(QuarkusToolingModelResult result)
            throws IOException, ClassNotFoundException {
        final byte[] serialized;
        try (var bytes = new ByteArrayOutputStream(); var output = new ObjectOutputStream(bytes)) {
            output.writeObject(result);
            serialized = bytes.toByteArray();
        }
        try (var input = new ObjectInputStream(new ByteArrayInputStream(serialized))) {
            return (QuarkusToolingModelResult) input.readObject();
        }
    }
}
