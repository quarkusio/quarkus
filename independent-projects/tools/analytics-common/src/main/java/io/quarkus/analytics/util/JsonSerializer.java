package io.quarkus.analytics.util;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.analytics.dto.config.Identity;
import io.quarkus.analytics.dto.config.LocalConfig;
import io.quarkus.analytics.dto.config.RemoteConfig;
import io.quarkus.analytics.dto.segment.Track;
import io.quarkus.analytics.dto.segment.TrackEventType;
import io.quarkus.analytics.dto.segment.TrackProperties;
import io.quarkus.bootstrap.json.Json;
import io.quarkus.bootstrap.json.JsonArray;
import io.quarkus.bootstrap.json.JsonBoolean;
import io.quarkus.bootstrap.json.JsonDouble;
import io.quarkus.bootstrap.json.JsonInteger;
import io.quarkus.bootstrap.json.JsonObject;
import io.quarkus.bootstrap.json.JsonReader;
import io.quarkus.bootstrap.json.JsonString;
import io.quarkus.bootstrap.json.JsonValue;

public final class JsonSerializer {

    private JsonSerializer() {
    }

    public static String toJson(RemoteConfig config) {
        Json.JsonObjectBuilder builder = Json.object();
        builder.put("active", config.isActive());
        if (config.getDenyAnonymousIds() != null) {
            builder.put("deny_anonymous_ids", toJsonArray(config.getDenyAnonymousIds()));
        }
        if (config.getDenyQuarkusVersions() != null) {
            builder.put("deny_quarkus_versions", toJsonArray(config.getDenyQuarkusVersions()));
        }
        if (config.getRefreshInterval() != null) {
            builder.put("refresh_interval", config.getRefreshInterval().toString());
        }
        return toJsonString(builder);
    }

    public static RemoteConfig parseRemoteConfig(String json) {
        JsonObject obj = JsonReader.of(json).read();
        RemoteConfig config = new RemoteConfig();

        JsonBoolean active = obj.get("active");
        if (active != null) {
            config.setActive(active.value());
        }

        JsonArray denyAnonymousIds = obj.get("deny_anonymous_ids");
        if (denyAnonymousIds != null) {
            config.setDenyAnonymousIds(toStringList(denyAnonymousIds));
        }

        JsonArray denyQuarkusVersions = obj.get("deny_quarkus_versions");
        if (denyQuarkusVersions != null) {
            config.setDenyQuarkusVersions(toStringList(denyQuarkusVersions));
        }

        JsonValue refreshInterval = obj.get("refresh_interval");
        if (refreshInterval != null) {
            if (refreshInterval instanceof JsonString str) {
                config.setRefreshInterval(Duration.parse(str.value()));
            } else if (refreshInterval instanceof JsonInteger intVal) {
                config.setRefreshInterval(Duration.ofSeconds(intVal.longValue()));
            } else if (refreshInterval instanceof JsonDouble doubleVal) {
                config.setRefreshInterval(Duration.ofSeconds((long) doubleVal.value()));
            }
        }

        return config;
    }

    public static String toJson(LocalConfig config) {
        Json.JsonObjectBuilder builder = Json.object();
        builder.put("disabled", config.isDisabled());
        return toJsonString(builder);
    }

    public static LocalConfig parseLocalConfig(String json) {
        JsonObject obj = JsonReader.of(json).read();
        LocalConfig config = new LocalConfig();

        JsonBoolean disabled = obj.get("disabled");
        if (disabled != null) {
            config.setDisabled(disabled.value());
        }

        return config;
    }

    public static String toJson(Identity identity) {
        Json.JsonObjectBuilder builder = Json.object();
        if (identity.getUserId() != null) {
            builder.put("userId", identity.getUserId());
        }
        if (identity.getContext() != null) {
            builder.put("context", toJsonObject(identity.getContext()));
        }
        if (identity.getTimestamp() != null) {
            builder.put("timestamp", identity.getTimestamp().toString());
        }
        return toJsonString(builder);
    }

    public static String toJson(Track track) {
        Json.JsonObjectBuilder builder = Json.object();
        if (track.getUserId() != null) {
            builder.put("userId", track.getUserId());
        }
        if (track.getEvent() != null) {
            builder.put("event", track.getEvent().name());
        }
        if (track.getProperties() != null) {
            builder.put("properties", toJsonObject(track.getProperties()));
        }
        if (track.getContext() != null) {
            builder.put("context", toJsonObject(track.getContext()));
        }
        if (track.getTimestamp() != null) {
            builder.put("timestamp", track.getTimestamp().toString());
        }
        return toJsonString(builder);
    }

    public static Track parseTrack(String json) {
        JsonObject obj = JsonReader.of(json).read();
        Track track = new Track();

        JsonString userId = obj.get("userId");
        if (userId != null) {
            track.setUserId(userId.value());
        }

        JsonString event = obj.get("event");
        if (event != null) {
            track.setEvent(TrackEventType.valueOf(event.value()));
        }

        JsonObject properties = obj.get("properties");
        if (properties != null) {
            track.setProperties(parseTrackProperties(properties));
        }

        JsonObject context = obj.get("context");
        if (context != null) {
            track.setContext(toMap(context));
        }

        JsonString timestamp = obj.get("timestamp");
        if (timestamp != null) {
            track.setTimestamp(Instant.parse(timestamp.value()));
        }

        return track;
    }

    private static TrackProperties parseTrackProperties(JsonObject obj) {
        TrackProperties props = new TrackProperties();

        JsonArray appExtensions = obj.get("app_extensions");
        if (appExtensions != null) {
            List<TrackProperties.AppExtension> extensions = new ArrayList<>();
            for (JsonValue value : appExtensions.value()) {
                if (value instanceof JsonObject extObj) {
                    extensions.add(parseAppExtension(extObj));
                }
            }
            props.setAppExtensions(extensions);
        }

        return props;
    }

    private static TrackProperties.AppExtension parseAppExtension(JsonObject obj) {
        TrackProperties.AppExtension ext = new TrackProperties.AppExtension();

        JsonString groupId = obj.get("group_id");
        if (groupId != null) {
            ext.setGroupId(groupId.value());
        }

        JsonString artifactId = obj.get("artifact_id");
        if (artifactId != null) {
            ext.setArtifactId(artifactId.value());
        }

        JsonString version = obj.get("version");
        if (version != null) {
            ext.setVersion(version.value());
        }

        return ext;
    }

    private static Json.JsonObjectBuilder toJsonObject(TrackProperties props) {
        Json.JsonObjectBuilder builder = Json.object();
        if (props.getAppExtensions() != null) {
            Json.JsonArrayBuilder arrayBuilder = Json.array();
            for (TrackProperties.AppExtension ext : props.getAppExtensions()) {
                arrayBuilder.add(toJsonObject(ext));
            }
            builder.put("app_extensions", arrayBuilder);
        }
        return builder;
    }

    private static Json.JsonObjectBuilder toJsonObject(TrackProperties.AppExtension ext) {
        Json.JsonObjectBuilder builder = Json.object();
        if (ext.getGroupId() != null) {
            builder.put("group_id", ext.getGroupId());
        }
        if (ext.getArtifactId() != null) {
            builder.put("artifact_id", ext.getArtifactId());
        }
        if (ext.getVersion() != null) {
            builder.put("version", ext.getVersion());
        }
        return builder;
    }

    @SuppressWarnings("unchecked")
    private static Json.JsonObjectBuilder toJsonObject(Map<String, Object> map) {
        Json.JsonObjectBuilder builder = Json.object();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String str) {
                builder.put(entry.getKey(), str);
            } else if (value instanceof Boolean bool) {
                builder.put(entry.getKey(), bool);
            } else if (value instanceof Integer intVal) {
                builder.put(entry.getKey(), intVal);
            } else if (value instanceof Long longVal) {
                builder.put(entry.getKey(), longVal);
            } else if (value instanceof Map) {
                builder.put(entry.getKey(), toJsonObject((Map<String, Object>) value));
            } else if (value instanceof List) {
                builder.put(entry.getKey(), toJsonArrayFromObjects((List<Object>) value));
            } else if (value != null) {
                builder.put(entry.getKey(), value.toString());
            }
        }
        return builder;
    }

    private static Json.JsonArrayBuilder toJsonArray(List<String> list) {
        Json.JsonArrayBuilder builder = Json.array();
        builder.addAll(list);
        return builder;
    }

    @SuppressWarnings("unchecked")
    private static Json.JsonArrayBuilder toJsonArrayFromObjects(List<Object> list) {
        Json.JsonArrayBuilder builder = Json.array();
        for (Object item : list) {
            if (item instanceof String str) {
                builder.add(str);
            } else if (item instanceof Boolean bool) {
                builder.add(bool);
            } else if (item instanceof Integer intVal) {
                builder.add(intVal);
            } else if (item instanceof Long longVal) {
                builder.add(longVal);
            } else if (item instanceof Map) {
                builder.add(toJsonObject((Map<String, Object>) item));
            } else if (item instanceof List) {
                builder.add(toJsonArrayFromObjects((List<Object>) item));
            } else if (item != null) {
                builder.add(item.toString());
            }
        }
        return builder;
    }

    private static List<String> toStringList(JsonArray array) {
        List<String> result = new ArrayList<>();
        for (JsonValue value : array.value()) {
            if (value instanceof JsonString str) {
                result.add(str.value());
            }
        }
        return result;
    }

    private static Map<String, Object> toMap(JsonObject obj) {
        Map<String, Object> result = new HashMap<>();
        for (var member : obj.members()) {
            String key = member.attribute().value();
            JsonValue value = member.value();
            result.put(key, toJavaValue(value));
        }
        return result;
    }

    private static Object toJavaValue(JsonValue value) {
        if (value instanceof JsonString str) {
            return str.value();
        } else if (value instanceof JsonBoolean bool) {
            return bool.value();
        } else if (value instanceof JsonInteger intVal) {
            return intVal.longValue();
        } else if (value instanceof JsonObject obj) {
            return toMap(obj);
        } else if (value instanceof JsonArray arr) {
            List<Object> list = new ArrayList<>();
            for (JsonValue item : arr.value()) {
                list.add(toJavaValue(item));
            }
            return list;
        }
        return null;
    }

    private static String toJsonString(Json.JsonObjectBuilder builder) {
        StringBuilder sb = new StringBuilder();
        try {
            builder.appendTo(sb);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
        return sb.toString();
    }

    public static Identity parseIdentity(String json) {
        JsonObject obj = JsonReader.of(json).read();
        Identity.IdentityBuilder builder = Identity.builder();

        JsonString userId = obj.get("userId");
        if (userId != null) {
            builder.userId(userId.value());
        }

        JsonObject ctx = obj.get("context");
        if (ctx != null) {
            builder.context(toMap(ctx));
        }

        JsonString timestamp = obj.get("timestamp");
        if (timestamp != null) {
            builder.timestamp(Instant.parse(timestamp.value()));
        }

        return builder.build();
    }
}
