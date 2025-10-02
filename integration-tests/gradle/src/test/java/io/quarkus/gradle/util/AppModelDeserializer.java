package io.quarkus.gradle.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import io.quarkus.bootstrap.model.ApplicationModel;

public class AppModelDeserializer {

    /**
     * Copied from ToolingUtils
     *
     * @param path application model dat file
     * @return deserialized ApplicationModel
     * @throws IOException in case of a failure to read the model
     */
    public static ApplicationModel deserializeAppModel(Path path) throws IOException {
        try (ObjectInputStream out = new ObjectInputStream(Files.newInputStream(path))) {
            return (ApplicationModel) out.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
