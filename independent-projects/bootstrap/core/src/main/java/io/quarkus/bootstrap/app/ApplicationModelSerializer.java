package io.quarkus.bootstrap.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.json.Json;
import io.quarkus.bootstrap.json.JsonArray;
import io.quarkus.bootstrap.json.JsonObject;
import io.quarkus.bootstrap.json.JsonReader;
import io.quarkus.bootstrap.json.JsonValue;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.bootstrap.model.MappableCollectionFactory;

/**
 * Utility class providing various serializing and deserializing methods for {@link ApplicationModel}
 */
public class ApplicationModelSerializer {

    private static final MappableCollectionFactory JSON_CONTAINER_FACTORY = new JsonCollectionFactory();

    private static final String QUARKUS_APPLICATION_MODEL_SERIALIZATION_FORMAT_PROP = "quarkus.bootstrap.application-model.serialization.format";
    // whether to use Java Object Serialization as a format
    private static final boolean JOS;
    static {
        final String serializationFormat = System.getProperty(QUARKUS_APPLICATION_MODEL_SERIALIZATION_FORMAT_PROP);
        if (serializationFormat == null) {
            JOS = false;
        } else {
            if (serializationFormat.equalsIgnoreCase("jos")) {
                JOS = true;
            } else {
                JOS = false;
                if (!serializationFormat.equalsIgnoreCase("json")) {
                    throw new IllegalStateException("Unsupported serialization format: " + serializationFormat);
                }
            }
        }
    }

    /**
     * Creates a temporary file to serialize an application model.
     *
     * @param test whether it's for a test model
     * @return temporary file
     * @throws IOException in case of a failure
     */
    private static Path getTempFile(boolean test) throws IOException {
        return Files.createTempFile(
                "quarkus-" + (test ? "test-" : "") + "app-model",
                ".dat");
    }

    /**
     * Serializes an {@link ApplicationModel} to a file and sets a system property
     * {@link BootstrapConstants#SERIALIZED_TEST_APP_MODEL}
     * to the value of the file path the model was serialized in.
     * <p/>
     * This method will make sure the serialization will work for Gradle proxies of {@link ApplicationModel}.
     *
     * @param model application model to serialize
     * @param test whether it's a test application model
     * @throws IOException in case of a failure
     */
    public static void exportGradleModel(ApplicationModel model, boolean test) throws IOException {
        System.setProperty(test ? BootstrapConstants.SERIALIZED_TEST_APP_MODEL : BootstrapConstants.SERIALIZED_APP_MODEL,
                serializeGradleModel(model, test).toString());
    }

    /**
     * Serializes an {@link ApplicationModel} and returns a path to the file in which the application model was serialized.
     * <p/>
     * This method will make sure the serialization will work for Gradle proxies of {@link ApplicationModel}.
     *
     * @param model application model to serialize
     * @param test whether it's a test application model
     * @return file in which the application model was serialized
     * @throws IOException in case of a failure
     */
    public static Path serializeGradleModel(ApplicationModel model, boolean test) throws IOException {
        final Path serializedModel = getTempFile(test);
        if (JOS) {
            serializeWithJos(model, serializedModel);
        } else {
            writeJson(toJsonObjectBuilder(model.asMap()), serializedModel);
        }
        return serializedModel;
    }

    /**
     * Serializes an {@link ApplicationModel} and returns a path to the file in which the application model was serialized.
     *
     * @param model application model to serialize
     * @param test whether it's a test application model
     * @return file in which the application model was serialized
     * @throws IOException in case of a failure
     */
    public static Path serialize(ApplicationModel model, boolean test) throws IOException {
        final Path serializedModel = getTempFile(test);
        serialize(model, serializedModel);
        return serializedModel;
    }

    /**
     * Serializes an {@link ApplicationModel} to a file.
     *
     * @param appModel application model to serialize
     * @param file target file
     * @throws IOException in case of a failure
     */
    public static void serialize(ApplicationModel appModel, Path file) throws IOException {
        Files.createDirectories(file.getParent());
        if (JOS) {
            serializeWithJos(appModel, file);
        } else {
            toJson(appModel, file);
        }
    }

    /**
     * Deserializes an {@link ApplicationModel} from a given file.
     *
     * @param file file to deserialize the application model from
     * @return deserialized application model
     * @throws IOException in case of a failure
     */
    public static ApplicationModel deserialize(Path file) throws IOException {
        return JOS ? deserializeWithJos(file) : fromJson(file);
    }

    /**
     * Serialize an {@link ApplicationModel} with Java Object Serialization (the legacy way).
     *
     * @param appModel application model to serialize
     * @param file target file
     * @throws IOException in case of a failure
     */
    private static void serializeWithJos(ApplicationModel appModel, Path file) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
            out.writeObject(appModel);
        }
    }

    /**
     * Deserializes an {@link ApplicationModel} from a file with Java Object Serialization.
     *
     * @param file file to read an application model from
     * @return deserialized application model
     * @throws IOException in case of a failure
     */
    private static ApplicationModel deserializeWithJos(Path file) throws IOException {
        try (InputStream existing = Files.newInputStream(file)) {
            return (ApplicationModel) new ObjectInputStream(existing).readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Serialize an {@link ApplicationModel} to a JSON file.
     *
     * @param appModel application model to serialize
     * @param file target file
     * @throws IOException in case of a failure
     */
    private static void toJson(ApplicationModel appModel, Path file) throws IOException {
        writeJson((Json.JsonObjectBuilder) appModel.asMap(JSON_CONTAINER_FACTORY), file);
    }

    /**
     * Serializes a {@link io.quarkus.bootstrap.json.Json.JsonObjectBuilder} to a JSON file.
     *
     * @param jsonObject JSON object builder
     * @param file target file
     * @throws IOException in case of a failure
     */
    private static void writeJson(Json.JsonObjectBuilder jsonObject, Path file) throws IOException {
        try (Writer writer = Files.newBufferedWriter(file)) {
            jsonObject.appendTo(writer);
        }
    }

    /**
     * Deserializes an {@link ApplicationModel} from a JSON file.
     *
     * @param file JSON file
     * @return deserialized application model
     * @throws IOException in case of a failure
     */
    private static ApplicationModel fromJson(Path file) throws IOException {
        final Map<String, Object> modelMap = asMap(JsonReader.of(Files.readString(file)).read());
        return ApplicationModelBuilder.fromMap(modelMap);
    }

    /**
     * Transforms a {@link JsonObject} to a {@link Map} with {@link String} keys and {@link Object} values.
     *
     * @param jsonObject JSON object to transform
     * @return map representation of the JSON object
     */
    private static Map<String, Object> asMap(JsonObject jsonObject) {
        var members = jsonObject.members();
        final Map<String, Object> map = new HashMap<>(members.size());
        for (var member : members) {
            map.put(member.attribute().value(), asObject(member.value()));
        }
        return map;
    }

    /**
     * Transforms a {@link JsonArray} to a {@link Collection} of objects.
     *
     * @param jsonArray JSON array to transform
     * @return collection representation of the JSON array
     */
    private static Collection<Object> asCollection(JsonArray jsonArray) {
        final Collection<Object> col = new ArrayList<>(jsonArray.size());
        jsonArray.stream().map(ApplicationModelSerializer::asObject).forEach(col::add);
        return col;
    }

    /**
     * Transforms a {@link JsonValue} to the corresponding {@link Map}, {@link Collection} or {@link String}
     * value, depending on the actual JSON type.
     *
     * @param jsonValue JSON value to transform
     * @return object
     */
    private static Object asObject(JsonValue jsonValue) {
        if (jsonValue instanceof JsonObject jsonObject) {
            return asMap(jsonObject);
        }
        if (jsonValue instanceof JsonArray jsonArray) {
            return asCollection(jsonArray);
        }
        return jsonValue.toString();
    }

    /**
     * Converts a {@link Map} instance to a {@link io.quarkus.bootstrap.json.Json.JsonObjectBuilder}.
     *
     * @param map map to convert
     * @return JSON object builder
     */
    private static Json.JsonObjectBuilder toJsonObjectBuilder(Map<String, Object> map) {
        final Json.JsonObjectBuilder result = Json.object(map.size());
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result.put(entry.getKey(), toJsonTypeBuilder(entry.getValue()));
        }
        return result;
    }

    /**
     * Converts a {@link Collection} instance to a {@link io.quarkus.bootstrap.json.Json.JsonArrayBuilder}.
     *
     * @param col collection to convert
     * @return JSON array builder
     */
    private static Json.JsonArrayBuilder toJsonArrayBuilder(Collection<Object> col) {
        final Json.JsonArrayBuilder result = Json.array(col.size());
        for (Object o : col) {
            result.add(toJsonTypeBuilder(o));
        }
        return result;
    }

    /**
     * Converts an {@link Object} to a JSON type builder.
     *
     * @param o object to convert
     * @return JSON type builder
     */
    private static Object toJsonTypeBuilder(Object o) {
        if (o instanceof Map map) {
            return toJsonObjectBuilder(map);
        }
        if (o instanceof Collection col) {
            return toJsonArrayBuilder(col);
        }
        return o;
    }

    private static void logMap(Map<String, Object> map, int offset) {
        final String offsetString = " ".repeat(Math.max(0, offset));
        for (var entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                System.out.println(offsetString + entry.getKey());
                logMap((Map<String, Object>) entry.getValue(), offset + 2);
            } else if (entry.getValue() instanceof Collection) {
                System.out.println(offsetString + entry.getKey());
                logCollection((Collection<Object>) entry.getValue(), offset + 2);
            } else {
                System.out.println(offsetString + entry.getKey() + ": " + entry.getValue());
            }
        }
    }

    private static void logCollection(Collection<Object> col, int offset) {
        final String offsetString = " ".repeat(Math.max(0, offset));
        for (var entry : col) {
            if (entry instanceof Map) {
                logMap((Map<String, Object>) entry, offset + 2);
            } else if (entry instanceof Collection) {
                logCollection((Collection<Object>) entry, offset + 2);
            } else {
                System.out.println(offsetString + entry);
            }
        }
    }

    private static class JsonCollectionFactory implements MappableCollectionFactory {
        @Override
        public Map<String, Object> newMap() {
            return Json.object();
        }

        @Override
        public Map<String, Object> newMap(int initialCapacity) {
            return Json.object(initialCapacity);
        }

        @Override
        public Collection<Object> newCollection() {
            return Json.array();
        }

        @Override
        public Collection<Object> newCollection(int initialCapacity) {
            return Json.array(initialCapacity);
        }
    }
}
