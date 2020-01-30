package io.quarkus.gradle.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.testing.Test;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.gradle.QuarkusPluginExtension;

public class QuarkusTestConfig extends QuarkusTask {

    public QuarkusTestConfig() {
        super("Sets the necessary system properties for the Quarkus tests to run.");
    }

    @TaskAction
    public void setupTest() {
        final QuarkusPluginExtension quarkusExt = extension();
        try {
            final AppModel deploymentDeps = quarkusExt.resolveAppModel().resolveModel(quarkusExt.getAppArtifact());
            final String nativeRunner = getProject().getBuildDir().toPath().resolve(quarkusExt.finalName() + "-runner")
                    .toAbsolutePath()
                    .toString();

            SourceSetContainer sourceSets = getProject().getConvention().getPlugin(JavaPluginConvention.class)
                    .getSourceSets();
            SourceSet testSourceSet = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);
            File classesDir = testSourceSet.getOutput().getClassesDirs().getFiles().iterator().next();
            classesDir.mkdirs();
            try (ObjectOutputStream out = new ObjectOutputStream(
                    new FileOutputStream(new File(classesDir, BootstrapConstants.SERIALIZED_APP_MODEL)))) {
                out.writeObject(deploymentDeps);
            }

            for (Test test : getProject().getTasks().withType(Test.class)) {
                final Map<String, Object> props = test.getSystemProperties();
                props.put("native.image.path", nativeRunner);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve deployment classpath", e);
        }
    }
}
