package io.quarkus.gradle.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.resolver.json.QuarkusJsonPlatformDescriptorResolver;

public abstract class QuarkusPlatformTask extends QuarkusTask {

    QuarkusPlatformTask(String description) {
        super(description);
    }

    protected QuarkusPlatformDescriptor platformDescriptor() {
        final Path currentDir = getProject().getProjectDir().toPath();

        final Path gradlePropsPath = currentDir.resolve("gradle.properties");
        if (!Files.exists(gradlePropsPath)) {
            getProject().getLogger()
                    .warn("Failed to locate " + gradlePropsPath + " to determine the Quarkus Platform BOM coordinates");
            return null;
        }
        final Properties props = new Properties();
        try (InputStream is = Files.newInputStream(gradlePropsPath)) {
            props.load(is);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + gradlePropsPath, e);
        }

        return QuarkusJsonPlatformDescriptorResolver.newInstance()
                .setArtifactResolver(extension().getAppModelResolver())
                .setMessageWriter(new GradleMessageWriter(getProject().getLogger()))
                .resolveFromBom(
                        getRequiredProperty(props, "quarkusPlatformGroupId"),
                        getRequiredProperty(props, "quarkusPlatformArtifactId"),
                        getRequiredProperty(props, "quarkusPlatformVersion"));
    }

    private static String getRequiredProperty(Properties props, String name) {
        final String value = props.getProperty(name);
        if (value == null) {
            throw new IllegalStateException("Required property " + name + " is missing from gradle.properties");
        }
        return value;
    }
}
