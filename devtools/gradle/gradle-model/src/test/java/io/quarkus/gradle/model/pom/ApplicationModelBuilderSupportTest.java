package io.quarkus.gradle.model.pom;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.quarkus.paths.PathList;

class ApplicationModelBuilderSupportTest {

    @TempDir
    Path directory;

    @Test
    void findsExtensionDescriptorInLaterResolvedDirectory() throws Exception {
        Path classes = Files.createDirectory(directory.resolve("classes"));
        Path resources = Files.createDirectories(directory.resolve("resources").resolve(BootstrapConstants.META_INF));
        Files.writeString(resources.resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME),
                "deployment-artifact=org.acme\\:example-deployment\\:1.0\n");
        ResolvedDependencyBuilder dependency = ResolvedDependencyBuilder.newInstance()
                .setGroupId("org.acme")
                .setArtifactId("example")
                .setVersion("1.0")
                .setResolvedPaths(PathList.of(classes, directory.resolve("resources")));

        boolean extension = ApplicationModelBuilderSupport.processQuarkusDependency(
                dependency, new ApplicationModelBuilder());

        assertThat(extension).isTrue();
        assertThat(dependency.isRuntimeExtensionArtifact()).isTrue();
    }
}
