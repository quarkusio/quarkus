package io.quarkus.funqy.runtime.bindings.knative.events;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import io.quarkus.funqy.knative.events.AbstractCloudEvent;
import io.quarkus.funqy.knative.events.CloudEvent;

class JsonCloudEventImpl<T> extends AbstractCloudEvent<T> implements CloudEvent<T> {
    String id;
    String specVersion;
    String source;
    String type;

    String subject;
    OffsetDateTime time;
    Map<String, String> extensions;

    String dataSchema;
    String dataContentType;
    T data;

    final JsonNode event;
    final ObjectMapper mapper;
    final Type dataType;
    private ObjectReader reader;

    public JsonCloudEventImpl(JsonNode event, Type dataType, ObjectMapper mapper, ObjectReader reader) {
        this.event = event;
        this.mapper = mapper;
        this.dataType = dataType;
        this.reader = reader;
    }

    @Override
    public String id() {
        if (id == null) {
            JsonNode id = event.get("id");
            if (id != null)
                this.id = id.asText();
        }

        return id;
    }

    @Override
    public String specVersion() {
        if (specVersion == null) {
            JsonNode specVersion = event.get("specversion");
            if (specVersion != null) {
                this.specVersion = specVersion.asText();
            }
        }

        return specVersion;
    }

    @Override
    public String source() {
        if (source == null && event.has("source")) {
            this.source = event.get("source").asText();
        }

        return source;
    }

    @Override
    public String type() {
        if (type == null) {
            JsonNode source = event.get("type");
            if (source != null)
                this.type = source.asText();
        }

        return type;
    }

    @Override
    public String subject() {
        if (subject == null) {
            JsonNode subject = event.get("subject");
            if (subject != null) {
                this.subject = subject.asText();
            }
        }

        return subject;
    }

    @Override
    public OffsetDateTime time() {
        if (time == null) {
            JsonNode time = event.get("time");
            if (time != null) {
                this.time = OffsetDateTime.parse(time.asText());
            }
        }

        return time;
    }

    private static final Set<String> reservedAttributes;
    static {
        Set<String> ra = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        ra.add("specversion");
        ra.add("id");
        ra.add("source");
        ra.add("type");

        ra.add("datacontenttype");
        ra.add("subject");
        ra.add("time");

        ra.add("datacontentencoding");
        ra.add("schemaurl");
        ra.add("dataschema");

        ra.add("data");

        reservedAttributes = Collections.unmodifiableSet(ra);
    }

    @Override
    public Map<String, String> extensions() {

        if (extensions == null) {
            extensions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            event.fields().forEachRemaining(e -> {
                if (!reservedAttributes.contains(e.getKey())) {
                    extensions.put(e.getKey(), e.getValue().textValue());
                }
            });
            extensions = Collections.unmodifiableMap(extensions);
        }

        return extensions;
    }

    @Override
    public String dataSchema() {
        if (dataSchema == null) {
            String dsName = majorSpecVersion() == 0 ? "schemaurl" : "dataschema";
            JsonNode dataSchema = event.get(dsName);
            if (dataSchema != null) {
                this.dataSchema = dataSchema.asText();
            }
        }

        return dataSchema;
    }

    @Override
    public String dataContentType() {
        if (dataContentType == null) {
            JsonNode dataContentType = event.get("datacontenttype");
            if (dataContentType != null) {
                this.dataContentType = dataContentType.asText();
            }
        }

        return dataContentType;
    }

    @Override
    public T data() {
        if (data != null) {
            return data;
        }
        if (dataContentType() != null && dataContentType().startsWith("application/json") && !byte[].class.equals(dataType)) {
            try {
                data = reader.readValue(event.get("data"));
                return data;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else if (byte[].class.equals(dataType)) {
            try {
                if (majorSpecVersion() == 0) {
                    boolean isBase64 = false;
                    if (event.has("datacontentencoding")) {
                        String dce = event.get("datacontentencoding").asText();
                        if ("base64".equals(dce)) {
                            isBase64 = true;
                        } else {
                            throw new RuntimeException("Cannot deserialize data for data-content-encoding: '" + dce + "'.");
                        }
                    }
                    if (isBase64) {
                        if (event.has("data")) {
                            String txt = event.get("data").asText();
                            data = (T) Base64.getDecoder().decode(txt);
                            return data;
                        } else {
                            return null;
                        }
                    } else {
                        if (event.has("data")) {
                            data = (T) mapper.writeValueAsBytes(event.get("data"));
                            return data;
                        } else {
                            return null;
                        }
                    }
                } else {
                    if (event.has("data")) {
                        data = (T) mapper.writeValueAsBytes(event.get("data"));
                        return data;
                    } else if (event.has("data_base64")) {
                        String txt = event.get("data_base64").asText();
                        data = (T) Base64.getDecoder().decode(txt);
                        return data;
                    } else {
                        return null;
                    }
                }

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } else {
            String msg = String.format("Don't know how to get event data (dataContentType: '%s', javaType: '%s').",
                    dataContentType(), dataType.getTypeName());
            throw new RuntimeException(msg);
        }
    }

}
