package io.quarkus.gradle.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.resolver.json.QuarkusJsonPlatformDescriptorResolver;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;

public class QuarkusPlatformTask extends QuarkusTask {

    QuarkusPlatformTask(String description) {
        super(description);
    }

    protected void setupPlatformDescriptor() {

        if (QuarkusPlatformConfig.hasGlobalDefault()) {
            return;
        }

        final Path currentDir = getProject().getProjectDir().toPath();

        final Path gradlePropsPath = currentDir.resolve("gradle.properties");
        if (Files.exists(gradlePropsPath)) {
            final Properties props = new Properties();
            try (InputStream is = Files.newInputStream(gradlePropsPath)) {
                props.load(is);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load " + gradlePropsPath, e);
            }

            final QuarkusPlatformDescriptor platform = QuarkusJsonPlatformDescriptorResolver.newInstance()
                    .setArtifactResolver(extension().resolveAppModel())
                    .setBomVersion(
                            getRequiredProperty(props, "quarkusPlatformBomGroupId"),
                            getRequiredProperty(props, "quarkusPlatformBomArtifactId"),
                            getRequiredProperty(props, "quarkusPlatformBomVersion"))
                    .setMessageWriter(new GradleMessageWriter(getProject().getLogger()))
                    .resolve();

            QuarkusPlatformConfig.defaultConfigBuilder().setPlatformDescriptor(platform).build();

        } else {
            getProject().getLogger()
                    .warn("Failed to locate " + gradlePropsPath + " to determine the Quarkus Platform BOM coordinates");
        }
    }

    private static String getRequiredProperty(Properties props, String name) {
        final String value = props.getProperty(name);
        if (value == null) {
            throw new IllegalStateException("Required property " + name + " is missing from gradle.properties");
        }
        return value;
    }
}
