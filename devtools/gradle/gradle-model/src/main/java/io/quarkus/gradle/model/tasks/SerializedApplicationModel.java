package io.quarkus.gradle.model.tasks;

import java.io.IOException;
import java.nio.file.Path;

import io.quarkus.bootstrap.app.ApplicationModelSerializer;
import io.quarkus.bootstrap.model.ApplicationModel;

public final class SerializedApplicationModel {

    private SerializedApplicationModel() {
    }

    public static Path write(ApplicationModel appModel, Path serializedModelPath) throws IOException {
        ApplicationModelSerializer.serialize(appModel, serializedModelPath);
        return serializedModelPath;
    }
}
