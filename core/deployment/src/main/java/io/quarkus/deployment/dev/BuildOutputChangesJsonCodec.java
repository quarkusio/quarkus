package io.quarkus.deployment.dev;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.bootstrap.json.Json;
import io.quarkus.bootstrap.json.JsonArray;
import io.quarkus.bootstrap.json.JsonBoolean;
import io.quarkus.bootstrap.json.JsonInteger;
import io.quarkus.bootstrap.json.JsonObject;
import io.quarkus.bootstrap.json.JsonReader;
import io.quarkus.bootstrap.json.JsonString;
import io.quarkus.bootstrap.json.JsonValue;

final class BuildOutputChangesJsonCodec {

    private static final String SEQUENCE = "sequence";
    private static final String STATUS = "status";
    private static final String MAIN_CLASS_CHANGES = "mainClassChanges";
    private static final String MAIN_RESOURCE_CHANGES = "mainResourceChanges";
    private static final String TEST_CLASS_CHANGES = "testClassChanges";
    private static final String TEST_RESOURCE_CHANGES = "testResourceChanges";
    private static final String FAILURE_SUMMARY = "failureSummary";
    private static final String DIAGNOSTICS_PATH = "diagnosticsPath";
    private static final String USER_INITIATED = "userInitiated";
    private static final String FORCE_RESTART = "forceRestart";
    private static final String OUTPUT_ROOT = "outputRoot";
    private static final String CHANGED_PATH = "changedPath";
    private static final String KIND = "kind";

    private BuildOutputChangesJsonCodec() {
    }

    static String encode(BuildOutputChanges changes) {
        requireNonNull(changes, "changes");
        var root = Json.object()
                .put(SEQUENCE, changes.sequence())
                .put(STATUS, changes.status().name())
                .put(MAIN_CLASS_CHANGES, pathChanges(changes.mainClassChanges()))
                .put(MAIN_RESOURCE_CHANGES, pathChanges(changes.mainResourceChanges()))
                .put(TEST_CLASS_CHANGES, pathChanges(changes.testClassChanges()))
                .put(TEST_RESOURCE_CHANGES, pathChanges(changes.testResourceChanges()))
                .put(USER_INITIATED, changes.userInitiated())
                .put(FORCE_RESTART, changes.forceRestart());
        if (changes.failureSummary() != null) {
            root.put(FAILURE_SUMMARY, changes.failureSummary());
        }
        if (changes.diagnosticsPath() != null) {
            root.put(DIAGNOSTICS_PATH, changes.diagnosticsPath().toString());
        }
        StringBuilder result = new StringBuilder();
        try {
            root.appendTo(result);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result.toString();
    }

    static BuildOutputChanges decode(String json) {
        requireNonNull(json, "json");
        JsonObject root = requireType(JsonReader.of(json).read(), JsonObject.class, "root");
        return new BuildOutputChanges(
                requiredLong(root, SEQUENCE),
                requiredEnum(root, STATUS, BuildOutputChangeStatus.class),
                pathChanges(root, MAIN_CLASS_CHANGES),
                pathChanges(root, MAIN_RESOURCE_CHANGES),
                pathChanges(root, TEST_CLASS_CHANGES),
                pathChanges(root, TEST_RESOURCE_CHANGES),
                optionalString(root, FAILURE_SUMMARY),
                optionalPath(root, DIAGNOSTICS_PATH),
                optionalBoolean(root, USER_INITIATED),
                optionalBoolean(root, FORCE_RESTART));
    }

    private static Json.JsonArrayBuilder pathChanges(List<BuildOutputPathChange> changes) {
        var array = Json.array();
        for (BuildOutputPathChange change : changes) {
            array.add(Json.object()
                    .put(OUTPUT_ROOT, change.outputRoot().toString())
                    .put(CHANGED_PATH, change.changedPath().toString())
                    .put(KIND, change.kind().name()));
        }
        return array;
    }

    private static List<BuildOutputPathChange> pathChanges(JsonObject root, String name) {
        JsonValue value = root.get(name);
        if (value == null) {
            return List.of();
        }
        JsonArray array = requireType(value, JsonArray.class, name);
        List<BuildOutputPathChange> result = new ArrayList<>(array.size());
        for (JsonValue element : array.value()) {
            JsonObject change = requireType(element, JsonObject.class, name + "[]");
            result.add(new BuildOutputPathChange(
                    Path.of(requiredString(change, OUTPUT_ROOT)),
                    Path.of(requiredString(change, CHANGED_PATH)),
                    requiredEnum(change, KIND, BuildOutputChangeKind.class)));
        }
        return result;
    }

    private static String requiredString(JsonObject object, String name) {
        String value = optionalString(object, name);
        if (value == null) {
            throw new IllegalArgumentException("Missing required string field: " + name);
        }
        return value;
    }

    private static String optionalString(JsonObject object, String name) {
        JsonValue value = object.get(name);
        if (value == null) {
            return null;
        }
        return requireType(value, JsonString.class, name).value();
    }

    private static Path optionalPath(JsonObject object, String name) {
        String value = optionalString(object, name);
        return value == null ? null : Path.of(value);
    }

    private static long requiredLong(JsonObject object, String name) {
        JsonValue value = object.get(name);
        if (value == null) {
            throw new IllegalArgumentException("Missing required number field: " + name);
        }
        return requireType(value, JsonInteger.class, name).longValue();
    }

    private static boolean optionalBoolean(JsonObject object, String name) {
        JsonValue value = object.get(name);
        if (value == null) {
            return false;
        }
        if (value == JsonBoolean.TRUE) {
            return true;
        }
        if (value == JsonBoolean.FALSE) {
            return false;
        }
        throw new IllegalArgumentException("Expected boolean field: " + name);
    }

    private static <E extends Enum<E>> E requiredEnum(JsonObject object, String name, Class<E> enumType) {
        String value = requiredString(object, name);
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid enum value for field " + name + ": " + value, e);
        }
    }

    private static <T extends JsonValue> T requireType(JsonValue value, Class<T> type, String name) {
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("Unexpected JSON type for field: " + name);
        }
        return type.cast(value);
    }
}
