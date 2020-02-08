package io.quarkus.gradle.tasks;

import java.nio.file.Path;
import java.util.Map;

import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.testing.Test;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.gradle.QuarkusPluginExtension;
import io.quarkus.runtime.LaunchMode;

public class QuarkusTestConfig extends QuarkusTask {

    public QuarkusTestConfig() {
        super("Sets the necessary system properties for the Quarkus tests to run.");
    }

    @TaskAction
    public void setupTest() {
        final QuarkusPluginExtension quarkusExt = extension();
        try {
            final AppModel deploymentDeps = quarkusExt.getAppModelResolver(LaunchMode.TEST)
                    .resolveModel(quarkusExt.getAppArtifact());
            final String nativeRunner = getProject().getBuildDir().toPath().resolve(quarkusExt.finalName() + "-runner")
                    .toAbsolutePath()
                    .toString();

            final Path serializedModel = QuarkusGradleUtils.serializeAppModel(deploymentDeps);

            for (Test test : getProject().getTasks().withType(Test.class)) {
                final Map<String, Object> props = test.getSystemProperties();
                props.put("native.image.path", nativeRunner);
                props.put(BootstrapConstants.SERIALIZED_APP_MODEL, serializedModel.toString());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve deployment classpath", e);
        }
    }
}
