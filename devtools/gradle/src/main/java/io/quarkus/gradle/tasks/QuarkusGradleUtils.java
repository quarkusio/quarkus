package io.quarkus.gradle.tasks;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.bootstrap.model.AppModel;

public class QuarkusGradleUtils {

    public static Path serializeAppModel(final AppModel appModel) throws IOException {
        final Path serializedModel = Files.createTempFile("quarkus-", "-gradle-test");
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(serializedModel))) {
            out.writeObject(appModel);
        }
        return serializedModel;
    }
}
