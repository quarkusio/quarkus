package io.quarkus.extension.deployment.gradle;

import java.util.Collections;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.process.CommandLineArgumentProvider;

import io.quarkus.bootstrap.BootstrapConstants;

/**
 * Supplies a generated serialized Quarkus test application model to a Gradle test JVM.
 * <p>
 * Public visibility and an abstract managed property are required for Gradle decoration. The deployment plugin creates
 * instances and attaches them to plugin-configured {@code Test} tasks; this type is not a user configuration surface.
 */
public abstract class SerializedTestApplicationModelArgumentProvider implements CommandLineArgumentProvider {

    /**
     * Returns the serialized test application model passed to the test JVM.
     *
     * @return the application-model file
     */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getApplicationModel();

    /**
     * Returns the system-property argument that identifies the serialized application model by absolute path.
     *
     * @return one JVM system-property argument
     */
    @Override
    public Iterable<String> asArguments() {
        return Collections.singletonList("-D" + BootstrapConstants.SERIALIZED_TEST_APP_MODEL + "="
                + getApplicationModel().get().getAsFile().getAbsolutePath());
    }
}
