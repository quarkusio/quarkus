package io.quarkus.bootstrap.resolver.maven.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenModelBuilder;

/**
 * Maven restricts property interpolation for models it builds while resolving artifact descriptors of
 * dependencies, parents and imported BOMs. Those requests arrive with
 * {@link ModelBuildingRequest#VALIDATION_LEVEL_MINIMAL} and, as of Maven 3.10, user and system properties
 * are not interpolated in them unless {@code maven.model.dependencyInterpolation.full} is enabled.
 * <p>
 * Models of projects that belong to the current workspace are not external dependency models though, so
 * they must keep being interpolated the way the Maven project build would interpolate them.
 */
public class WorkspaceModelInterpolationTest extends BootstrapMavenContextTestBase {

    @Test
    public void userPropertyInManagedDependencyVersion() throws Exception {
        setSystemProp("acme.cli.version", "1.2.3");
        var mvn = bootstrapMavenContextForProject("workspace-cli-property/root");

        var project = mvn.getCurrentProject();
        var descriptor = mvn.getRepositorySystem().readArtifactDescriptor(mvn.getRepositorySystemSession(),
                new ArtifactDescriptorRequest()
                        .setArtifact(new DefaultArtifact(project.getGroupId(), project.getArtifactId(), "pom",
                                project.getVersion()))
                        .setRepositories(mvn.getRemoteRepositories()));

        assertThat(descriptor.getManagedDependencies()).hasSize(1);
        assertThat(descriptor.getManagedDependencies().get(0).getArtifact().getVersion()).isEqualTo("1.2.3");
    }

    @Test
    public void minimalValidationLevelIsRaisedForWorkspaceProjects() throws Exception {
        var mvn = bootstrapMavenContextForProject("workspace-cli-property/root");
        var request = newArtifactDescriptorModelRequest(mvn, mvn.getCurrentProject().getRawModel().getPomFile());

        new MavenModelBuilder(mvn).build(request);

        assertThat(request.getValidationLevel()).isEqualTo(ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0);
    }

    @Test
    public void higherValidationLevelIsPreservedForWorkspaceProjects() throws Exception {
        var mvn = bootstrapMavenContextForProject("workspace-cli-property/root");
        var request = newArtifactDescriptorModelRequest(mvn, mvn.getCurrentProject().getRawModel().getPomFile());
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_STRICT);

        new MavenModelBuilder(mvn).build(request);

        assertThat(request.getValidationLevel()).isEqualTo(ModelBuildingRequest.VALIDATION_LEVEL_STRICT);
    }

    @Test
    public void minimalValidationLevelIsKeptForExternalModels() throws Exception {
        var mvn = bootstrapMavenContextForProject("workspace-cli-property/root");
        var request = newArtifactDescriptorModelRequest(mvn,
                getProjectLocation("workspace-multiple/root").resolve("pom.xml").toFile());

        new MavenModelBuilder(mvn).build(request);

        assertThat(request.getValidationLevel()).isEqualTo(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
    }

    /**
     * Replicates the request Maven's artifact descriptor reader builds for an artifact POM.
     */
    private static ModelBuildingRequest newArtifactDescriptorModelRequest(BootstrapMavenContext mvn, File pomFile)
            throws Exception {
        return new DefaultModelBuildingRequest()
                .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL)
                .setProcessPlugins(false)
                .setTwoPhaseBuilding(false)
                .setSystemProperties(System.getProperties())
                .setModelSource(new FileModelSource(pomFile))
                .setPomFile(pomFile)
                .setModelResolver(BootstrapModelResolver.newInstance(mvn, mvn.getWorkspace()));
    }
}
