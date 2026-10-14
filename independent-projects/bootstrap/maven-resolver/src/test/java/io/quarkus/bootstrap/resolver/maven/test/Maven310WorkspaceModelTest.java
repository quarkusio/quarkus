package io.quarkus.bootstrap.resolver.maven.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.MavenModelBuilder;

class Maven310WorkspaceModelTest extends BootstrapMavenContextTestBase {

    @Test
    void workspaceDescriptorRequestUsesProjectValidation() throws Exception {
        setSystemProp("test.workspace.version", "3.40.1");

        BootstrapMavenContext context = bootstrapMavenContextForProject(
                "workspace-model-interpolation",
                BootstrapMavenContext.config().setEffectiveModelBuilder(false));

        assertNotNull(context.getWorkspace());
        assertNotNull(context.getCurrentProject());

        ModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setPomFile(context.getCurrentProject().getRawModel().getPomFile());
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        request.setSystemProperties(System.getProperties());
        request.setUserProperties(System.getProperties());

        ModelBuildingResult result = new MavenModelBuilder(context).build(request);

        assertTrue(
                request.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_2_0,
                "ISSUE 57381: workspace model retained dependency-level validation");

        assertEquals("3.40.1",
                result.getEffectiveModel().getDependencies().get(0).getVersion());
    }

    @Test
    void workspaceBuildPreservesHigherValidation() throws Exception {
        setSystemProp("test.workspace.version", "3.40.1");

        BootstrapMavenContext context = bootstrapMavenContextForProject(
                "workspace-model-interpolation",
                BootstrapMavenContext.config().setEffectiveModelBuilder(false));

        ModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setPomFile(context.getCurrentProject().getRawModel().getPomFile());
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0);
        request.setSystemProperties(System.getProperties());
        request.setUserProperties(System.getProperties());

        new MavenModelBuilder(context).build(request);

        assertEquals(ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_0,
                request.getValidationLevel());
    }
}
