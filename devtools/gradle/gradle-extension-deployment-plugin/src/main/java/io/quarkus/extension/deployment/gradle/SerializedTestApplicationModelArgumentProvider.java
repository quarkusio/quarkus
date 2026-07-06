package io.quarkus.extension.deployment.gradle;

import java.util.Collections;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.process.CommandLineArgumentProvider;

import io.quarkus.bootstrap.BootstrapConstants;

public abstract class SerializedTestApplicationModelArgumentProvider implements CommandLineArgumentProvider {

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getApplicationModel();

    @Override
    public Iterable<String> asArguments() {
        return Collections.singletonList("-D" + BootstrapConstants.SERIALIZED_TEST_APP_MODEL + "="
                + getApplicationModel().get().getAsFile().getAbsolutePath());
    }
}
