package io.quarkus.deployment.builditem;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.deployment.BootstrapConfig.IncompatibleExtensions;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;

public class AppModelProviderBuildItemTest {

    @Test
    public void testIncompatibleExtension() {
        final AppModelProviderBuildItem item = new AppModelProviderBuildItem(model("[3.38,3.40)"));

        /* Quarkus core version below the declared range */
        final RuntimeException error = assertThrows(RuntimeException.class,
                () -> item.validateExtensionCompatibility(IncompatibleExtensions.ERROR, "3.36.1"));
        assertTrue(error.getMessage().contains("org.acme:acme-extension:1.0"), error.getMessage());
        assertTrue(error.getMessage().contains("requires Quarkus [3.38,3.40)"), error.getMessage());

        /* Quarkus core version above the declared range */
        assertThrows(RuntimeException.class,
                () -> item.validateExtensionCompatibility(IncompatibleExtensions.ERROR, "3.40.0"));

        /* WARN and IGNORE do not throw */
        assertDoesNotThrow(() -> item.validateExtensionCompatibility(IncompatibleExtensions.WARN, "3.36.1"));
        assertDoesNotThrow(() -> item.validateExtensionCompatibility(IncompatibleExtensions.IGNORE, "3.36.1"));
    }

    @Test
    public void testCompatibleExtension() {
        final AppModelProviderBuildItem item = new AppModelProviderBuildItem(model("[3.38,3.40)"));
        assertDoesNotThrow(() -> item.validateExtensionCompatibility(IncompatibleExtensions.ERROR, "3.38.1"));
        assertDoesNotThrow(() -> item.validateExtensionCompatibility(IncompatibleExtensions.ERROR, "3.39.5"));

        final AppModelProviderBuildItem openEnded = new AppModelProviderBuildItem(model("[3.38,)"));
        assertDoesNotThrow(() -> openEnded.validateExtensionCompatibility(IncompatibleExtensions.ERROR, "4.0.0"));
    }

    @Test
    public void testMainBranchVersionSkipsCheck() {
        final AppModelProviderBuildItem item = new AppModelProviderBuildItem(model("[3.38,3.40)"));
        assertDoesNotThrow(() -> item.validateExtensionCompatibility(IncompatibleExtensions.ERROR, "999-SNAPSHOT"));
    }

    @Test
    public void testNoRangeSkipsCheck() {
        /* A plain version instead of a range is not enforced */
        final AppModelProviderBuildItem plainVersion = new AppModelProviderBuildItem(model("3.38.0"));
        assertDoesNotThrow(() -> plainVersion.validateExtensionCompatibility(IncompatibleExtensions.ERROR, "3.36.1"));

        /* No requires-quarkus-version property at all */
        final AppModelProviderBuildItem noProperty = new AppModelProviderBuildItem(model(null));
        assertDoesNotThrow(() -> noProperty.validateExtensionCompatibility(IncompatibleExtensions.ERROR, "3.36.1"));
    }

    private ApplicationModel model(String requiresQuarkusVersion) {
        final ResolvedDependencyBuilder extension = ResolvedDependencyBuilder.newInstance()
                .setGroupId("org.acme")
                .setArtifactId("acme-extension")
                .setVersion("1.0")
                .setFlags(DependencyFlags.RUNTIME_CP | DependencyFlags.DEPLOYMENT_CP
                        | DependencyFlags.RUNTIME_EXTENSION_ARTIFACT);
        final ApplicationModelBuilder builder = new ApplicationModelBuilder()
                .setAppArtifact(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("org.acme")
                        .setArtifactId("acme-app")
                        .setVersion("1.0"))
                .addDependency(extension)
                .addDependency(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("org.acme")
                        .setArtifactId("plain-jar")
                        .setVersion("1.0")
                        .setFlags(DependencyFlags.RUNTIME_CP | DependencyFlags.DEPLOYMENT_CP));
        final Properties props = new Properties();
        props.setProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT, "org.acme:acme-extension-deployment:1.0");
        if (requiresQuarkusVersion != null) {
            props.setProperty(BootstrapConstants.PROP_REQUIRES_QUARKUS_VERSION, requiresQuarkusVersion);
        }
        builder.handleExtensionProperties(props, extension.getKey());
        return builder.build();
    }
}
