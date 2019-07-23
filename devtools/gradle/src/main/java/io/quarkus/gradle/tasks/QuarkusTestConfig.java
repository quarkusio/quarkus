package io.quarkus.gradle.tasks;

import java.util.List;
import java.util.Map;

import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.testing.Test;

import io.quarkus.bootstrap.BootstrapClassLoaderFactory;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.gradle.QuarkusPluginExtension;

public class QuarkusTestConfig extends QuarkusTask {

    public QuarkusTestConfig() {
        super("Sets the necessary system properties for the Quarkus tests to run.");
    }

    @TaskAction
    public void setupTest() {
        final QuarkusPluginExtension quarkusExt = extension();
        try {
            final List<AppDependency> deploymentDeps = quarkusExt.resolveAppModel().resolveModel(quarkusExt.getAppArtifact())
                    .getDeploymentDependencies();
            final StringBuilder buf = new StringBuilder();
            for (AppDependency dep : deploymentDeps) {
                buf.append(dep.getArtifact().getPath().toUri().toURL().toExternalForm());
                buf.append(' ');
            }
            final String deploymentCp = buf.toString();
            final String nativeRunner = getProject().getBuildDir().toPath().resolve(quarkusExt.finalName() + "-runner")
                    .toAbsolutePath()
                    .toString();

            for (Test test : getProject().getTasks().withType(Test.class)) {
                final Map<String, Object> props = test.getSystemProperties();
                props.put(BootstrapClassLoaderFactory.PROP_DEPLOYMENT_CP, deploymentCp);
                props.put("native.image.path", nativeRunner);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve deployment classpath", e);
        }
    }
}
