package io.quarkus.gradle.model.tasks;

import java.io.IOException;
import java.nio.file.Path;

import io.quarkus.bootstrap.app.ApplicationModelSerializer;
import io.quarkus.bootstrap.model.ApplicationModel;

/**
 * Shared writer for the Quarkus bootstrap application-model serialization.
 * <p>
 * The output contains resolved local paths and is a task-to-task hand-off within a build. Its bytes and location are
 * not a public Gradle publication or long-term storage format.
 */
public final class SerializedApplicationModel {

    private SerializedApplicationModel() {
    }

    /**
     * Serializes {@code appModel} to {@code serializedModelPath}.
     *
     * @param appModel application model to serialize
     * @param serializedModelPath output path
     * @return {@code serializedModelPath}, for use in provider transformations
     * @throws IOException if serialization fails
     */
    public static Path write(ApplicationModel appModel, Path serializedModelPath) throws IOException {
        ApplicationModelSerializer.serialize(appModel, serializedModelPath);
        return serializedModelPath;
    }
}
