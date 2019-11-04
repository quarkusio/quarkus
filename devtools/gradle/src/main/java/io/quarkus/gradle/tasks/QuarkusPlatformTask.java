package io.quarkus.gradle.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.resolver.json.QuarkusJsonPlatformDescriptorResolver;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;

public abstract class QuarkusPlatformTask extends QuarkusTask {

    QuarkusPlatformTask(String description) {
        super(description);
    }

    protected void execute() {
        try {
            setupPlatformDescriptor();
            doExecute();
        } finally {
            QuarkusPlatformConfig.clearThreadLocal();
        }
    }

    protected abstract void doExecute();

    protected void setupPlatformDescriptor() {

        if (QuarkusPlatformConfig.hasThreadLocal()) {
            getProject().getLogger().debug("Quarkus platform descriptor has already been initialized");
            return;
        } else {
            getProject().getLogger().debug("Initializing Quarkus platform descriptor");
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
                    .setMessageWriter(new GradleMessageWriter(getProject().getLogger()))
                    .resolveFromBom(
                            getRequiredProperty(props, "quarkusPlatformGroupId"),
                            getRequiredProperty(props, "quarkusPlatformArtifactId"),
                            getRequiredProperty(props, "quarkusPlatformVersion"));

            QuarkusPlatformConfig.threadLocalConfigBuilder().setPlatformDescriptor(platform).build();

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
