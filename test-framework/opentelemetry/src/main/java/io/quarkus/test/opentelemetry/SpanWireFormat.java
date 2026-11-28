package io.quarkus.test.opentelemetry;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.common.KeyValue;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.common.ValueType;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.TraceStateBuilder;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.ExceptionEventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * The JSON exchanged between {@link SpanExporterRoute}, which runs in the application under test, and
 * {@link SpanExporterClient}, which runs in the test. The format is private to this library: both ends live here, so it can
 * change freely.
 * <p>
 * The test side rebuilds the {@link SpanData} the application exported, so that tests work with the types and the
 * assertions of OpenTelemetry itself. For exception events, the reconstructed throwable carries the original message. The event
 * retains its recorded attributes, including the exception type and stack trace when present.
 */
final class SpanWireFormat {

    private SpanWireFormat() {
    }

    static JsonArray encode(Collection<SpanData> spans) {
        JsonArray result = new JsonArray();
        for (SpanData span : spans) {
            JsonArray events = new JsonArray();
            for (EventData event : span.getEvents()) {
                JsonObject encoded = new JsonObject()
                        .put("name", event.getName())
                        .put("epochNanos", event.getEpochNanos())
                        .put("attributes", encode(event.getAttributes()))
                        .put("totalAttributeCount", event.getTotalAttributeCount());
                if (event instanceof ExceptionEventData exception) {
                    encoded.put("exceptionMessage", exception.getException().getMessage());
                }
                events.add(encoded);
            }
            JsonArray links = new JsonArray();
            for (LinkData link : span.getLinks()) {
                links.add(new JsonObject()
                        .put("context", encode(link.getSpanContext()))
                        .put("attributes", encode(link.getAttributes()))
                        .put("totalAttributeCount", link.getTotalAttributeCount()));
            }
            InstrumentationScopeInfo scope = span.getInstrumentationScopeInfo();
            result.add(new JsonObject()
                    .put("name", span.getName())
                    .put("kind", span.getKind().name())
                    .put("context", encode(span.getSpanContext()))
                    .put("parentContext", encode(span.getParentSpanContext()))
                    .put("statusCode", span.getStatus().getStatusCode().name())
                    .put("statusDescription", span.getStatus().getDescription())
                    .put("startEpochNanos", span.getStartEpochNanos())
                    .put("endEpochNanos", span.getEndEpochNanos())
                    .put("ended", span.hasEnded())
                    .put("attributes", encode(span.getAttributes()))
                    .put("totalAttributeCount", span.getTotalAttributeCount())
                    .put("events", events)
                    .put("totalRecordedEvents", span.getTotalRecordedEvents())
                    .put("links", links)
                    .put("totalRecordedLinks", span.getTotalRecordedLinks())
                    .put("resourceAttributes", encode(span.getResource().getAttributes()))
                    .put("resourceSchemaUrl", span.getResource().getSchemaUrl())
                    .put("scopeName", scope.getName())
                    .put("scopeVersion", scope.getVersion())
                    .put("scopeSchemaUrl", scope.getSchemaUrl())
                    .put("scopeAttributes", encode(scope.getAttributes())));
        }
        return result;
    }

    private static JsonObject encode(SpanContext context) {
        JsonArray traceState = new JsonArray();
        context.getTraceState().forEach((key, value) -> traceState.add(new JsonObject().put("key", key).put("value", value)));
        return new JsonObject()
                .put("traceId", context.getTraceId())
                .put("spanId", context.getSpanId())
                .put("traceFlags", context.getTraceFlags().asHex())
                .put("traceState", traceState)
                .put("remote", context.isRemote());
    }

    private static JsonArray encode(Attributes attributes) {
        JsonArray result = new JsonArray();
        for (Map.Entry<AttributeKey<?>, Object> entry : attributes.asMap().entrySet()) {
            AttributeKey<?> key = entry.getKey();
            JsonObject attribute = new JsonObject().put("key", key.getKey());
            switch (key.getType()) {
                case STRING, BOOLEAN, LONG, DOUBLE -> attribute.put("type", key.getType().name()).put("value",
                        entry.getValue());
                case STRING_ARRAY, BOOLEAN_ARRAY, LONG_ARRAY, DOUBLE_ARRAY -> attribute.put("type", key.getType().name())
                        .put("value", new JsonArray((List<?>) entry.getValue()));
                case VALUE -> attribute.put("type", "VALUE").put("value", encodeValue((Value<?>) entry.getValue()));
            }
            result.add(attribute);
        }
        return result;
    }

    private static JsonObject encodeValue(Value<?> value) {
        JsonObject json = new JsonObject().put("type", value.getType().name());
        Object data = value.getValue();
        switch (value.getType()) {
            case STRING, LONG, DOUBLE, BOOLEAN -> json.put("value", data);
            case BYTES -> {
                ByteBuffer buffer = ((ByteBuffer) data).duplicate();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                json.put("value", bytes);
            }
            case ARRAY -> {
                JsonArray elements = new JsonArray();
                for (Object element : (List<?>) data) {
                    elements.add(encodeValue((Value<?>) element));
                }
                json.put("value", elements);
            }
            case KEY_VALUE_LIST -> {
                JsonArray entries = new JsonArray();
                for (Object element : (List<?>) data) {
                    KeyValue entry = (KeyValue) element;
                    entries.add(new JsonObject().put("key", entry.getKey()).put("value", encodeValue(entry.getValue())));
                }
                json.put("value", entries);
            }
            case EMPTY -> {
            }
        }
        return json;
    }

    private static Value<?> decodeValue(JsonObject json) {
        return switch (ValueType.valueOf(json.getString("type"))) {
            case STRING -> Value.of(json.getString("value"));
            case LONG -> Value.of(json.getLong("value"));
            case DOUBLE -> Value.of(json.getDouble("value"));
            case BOOLEAN -> Value.of(json.getBoolean("value"));
            case BYTES -> Value.of(json.getBinary("value"));
            case EMPTY -> Value.empty();
            case ARRAY -> {
                JsonArray array = json.getJsonArray("value");
                List<Value<?>> elements = new ArrayList<>(array.size());
                for (int i = 0; i < array.size(); i++) {
                    elements.add(decodeValue(array.getJsonObject(i)));
                }
                yield Value.of(elements);
            }
            case KEY_VALUE_LIST -> {
                JsonArray array = json.getJsonArray("value");
                KeyValue[] entries = new KeyValue[array.size()];
                for (int i = 0; i < entries.length; i++) {
                    JsonObject entry = array.getJsonObject(i);
                    entries[i] = KeyValue.of(entry.getString("key"), decodeValue(entry.getJsonObject("value")));
                }
                yield Value.of(entries);
            }
        };
    }

    static List<SpanData> decode(String json) {
        JsonArray spans = new JsonArray(json);
        List<SpanData> result = new ArrayList<>(spans.size());
        for (int i = 0; i < spans.size(); i++) {
            JsonObject span = spans.getJsonObject(i);
            JsonArray jsonEvents = span.getJsonArray("events");
            List<EventData> events = new ArrayList<>(jsonEvents.size());
            for (int j = 0; j < jsonEvents.size(); j++) {
                JsonObject event = jsonEvents.getJsonObject(j);
                long epochNanos = event.getLong("epochNanos");
                Attributes attributes = decodeAttributes(event.getJsonArray("attributes"));
                int total = event.getInteger("totalAttributeCount");
                if (event.containsKey("exceptionMessage")) {
                    events.add(ExceptionEventData.create(epochNanos, new ExportedException(event.getString("exceptionMessage")),
                            attributes, total));
                } else {
                    events.add(EventData.create(epochNanos, event.getString("name"), attributes, total));
                }
            }
            JsonArray jsonLinks = span.getJsonArray("links");
            List<LinkData> links = new ArrayList<>(jsonLinks.size());
            for (int j = 0; j < jsonLinks.size(); j++) {
                JsonObject link = jsonLinks.getJsonObject(j);
                links.add(LinkData.create(decodeContext(link.getJsonObject("context"), false),
                        decodeAttributes(link.getJsonArray("attributes")), link.getInteger("totalAttributeCount")));
            }
            result.add(TestSpanData.builder()
                    .setName(span.getString("name"))
                    .setKind(SpanKind.valueOf(span.getString("kind")))
                    .setSpanContext(decodeContext(span.getJsonObject("context"), false))
                    .setParentSpanContext(decodeContext(span.getJsonObject("parentContext"), true))
                    .setStatus(StatusData.create(StatusCode.valueOf(span.getString("statusCode")),
                            span.getString("statusDescription")))
                    .setStartEpochNanos(span.getLong("startEpochNanos"))
                    .setEndEpochNanos(span.getLong("endEpochNanos"))
                    .setHasEnded(span.getBoolean("ended"))
                    .setAttributes(decodeAttributes(span.getJsonArray("attributes")))
                    .setTotalAttributeCount(span.getInteger("totalAttributeCount"))
                    .setEvents(events)
                    .setTotalRecordedEvents(span.getInteger("totalRecordedEvents"))
                    .setLinks(links)
                    .setTotalRecordedLinks(span.getInteger("totalRecordedLinks"))
                    .setResource(Resource.create(decodeAttributes(span.getJsonArray("resourceAttributes")),
                            span.getString("resourceSchemaUrl")))
                    .setInstrumentationScopeInfo(InstrumentationScopeInfo.builder(span.getString("scopeName"))
                            .setVersion(span.getString("scopeVersion"))
                            .setSchemaUrl(span.getString("scopeSchemaUrl"))
                            .setAttributes(decodeAttributes(span.getJsonArray("scopeAttributes")))
                            .build())
                    .build());
        }
        return result;
    }

    private static final class ExportedException extends Exception {
        ExportedException(String message) {
            super(message, null, false, false);
        }
    }

    private static SpanContext decodeContext(JsonObject context, boolean allowAbsentParent) {
        TraceStateBuilder traceState = TraceState.builder();
        JsonArray entries = context.getJsonArray("traceState");
        // TraceStateBuilder.put makes the entry the highest priority.
        for (int i = entries.size() - 1; i >= 0; i--) {
            JsonObject entry = entries.getJsonObject(i);
            traceState.put(entry.getString("key"), entry.getString("value"));
        }
        String traceId = context.getString("traceId");
        String spanId = context.getString("spanId");
        boolean absentParent = allowAbsentParent && TraceId.getInvalid().equals(traceId) && SpanId.getInvalid().equals(spanId);
        if (!absentParent && !(TraceId.isValid(traceId) && SpanId.isValid(spanId))) {
            throw new IllegalArgumentException("Invalid span context: traceId=" + traceId + ", spanId=" + spanId);
        }
        TraceFlags traceFlags = TraceFlags.fromHex(context.getString("traceFlags"), 0);
        return context.getBoolean("remote")
                ? SpanContext.createFromRemoteParent(traceId, spanId, traceFlags, traceState.build())
                : SpanContext.create(traceId, spanId, traceFlags, traceState.build());
    }

    private static Attributes decodeAttributes(JsonArray attributes) {
        AttributesBuilder builder = Attributes.builder();
        for (int i = 0; i < attributes.size(); i++) {
            JsonObject attribute = attributes.getJsonObject(i);
            String key = attribute.getString("key");
            String type = attribute.getString("type");
            switch (type) {
                case "STRING" -> builder.put(AttributeKey.stringKey(key), attribute.getString("value"));
                case "VALUE" -> builder.put(AttributeKey.valueKey(key), decodeValue(attribute.getJsonObject("value")));
                case "BOOLEAN" -> builder.put(AttributeKey.booleanKey(key), attribute.getBoolean("value"));
                case "LONG" -> builder.put(AttributeKey.longKey(key), attribute.getLong("value"));
                case "DOUBLE" -> builder.put(AttributeKey.doubleKey(key), attribute.getDouble("value"));
                case "STRING_ARRAY" -> {
                    JsonArray values = attribute.getJsonArray("value");
                    List<String> list = new ArrayList<>(values.size());
                    for (int j = 0; j < values.size(); j++) {
                        list.add(values.getString(j));
                    }
                    builder.put(AttributeKey.stringArrayKey(key), list);
                }
                case "BOOLEAN_ARRAY" -> {
                    JsonArray values = attribute.getJsonArray("value");
                    List<Boolean> list = new ArrayList<>(values.size());
                    for (int j = 0; j < values.size(); j++) {
                        list.add(values.getBoolean(j));
                    }
                    builder.put(AttributeKey.booleanArrayKey(key), list);
                }
                case "LONG_ARRAY" -> {
                    JsonArray values = attribute.getJsonArray("value");
                    List<Long> list = new ArrayList<>(values.size());
                    for (int j = 0; j < values.size(); j++) {
                        list.add(values.getLong(j));
                    }
                    builder.put(AttributeKey.longArrayKey(key), list);
                }
                case "DOUBLE_ARRAY" -> {
                    JsonArray values = attribute.getJsonArray("value");
                    List<Double> list = new ArrayList<>(values.size());
                    for (int j = 0; j < values.size(); j++) {
                        list.add(values.getDouble(j));
                    }
                    builder.put(AttributeKey.doubleArrayKey(key), list);
                }
                default -> throw new IllegalArgumentException("Unknown attribute type: " + type);
            }
        }
        return builder.build();
    }
}
