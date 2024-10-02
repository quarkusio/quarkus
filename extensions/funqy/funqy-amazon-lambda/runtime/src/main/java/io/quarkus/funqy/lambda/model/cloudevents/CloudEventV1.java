package io.quarkus.funqy.lambda.model.cloudevents;

import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.SpecVersion;

/**
 * This class represents a {@link CloudEvent} in version 1.0 and is Jackson friendly
 */
public class CloudEventV1 implements CloudEvent {

    //private static final Pattern JSON_TYPE_PATTERN = Pattern.compile("^(application|text)/([a-zA-Z]+\\+)?json;?.*$");

    private final CloudEventDataV1 data;
    private final SpecVersion specVersion;
    private final String id;
    private final String type;
    private final URI source;
    private final String dataContentType;
    private final URI dataSchema;
    private final String subject;
    private final OffsetDateTime time;
    private final Map<String, Object> extensions;

    public CloudEventV1(
            @JsonProperty("specversion") String specVersion,
            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("source") URI source,
            @JsonProperty("datacontenttype") String dataContentType,
            @JsonProperty("dataschema") URI dataSchema,
            @JsonProperty("subject") String subject,
            @JsonProperty("time") OffsetDateTime time,
            @JsonProperty("data") JsonNode data,
            @JsonProperty("data_base64") JsonNode dataBase64) {
        this.specVersion = SpecVersion.parse(specVersion);
        this.id = id;
        this.type = type;
        this.source = source;
        this.dataContentType = dataContentType;
        this.dataSchema = dataSchema;
        this.subject = subject;
        this.time = time;
        this.extensions = new HashMap<>();
        this.data = deserializeData(data, dataBase64, dataContentType);
    }

    @JsonAnySetter
    public void add(String property, String value) {
        switch (property) {
            case "specversion":
            case "id":
            case "source":
            case "type":
            case "datacontenttype":
            case "dataschema":
            case "data":
            case "data_base64":
            case "subject":
            case "time":
                // Those names are reserved
                return;
        }
        extensions.put(property, value);
    }

    private CloudEventDataV1 deserializeData(final JsonNode data, final JsonNode dataBase64,
            final String dataContentType) {
        if (dataBase64 != null) {
            try {
                return new CloudEventDataV1(dataBase64.binaryValue());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (data == null) {
            return null;
        }

        if (data.isTextual()) {
            return new CloudEventDataV1(data.asText());
        } else {
            // This should work for every other type. Even for application/json, because we need to serialize
            // the data anyway for the interface.
            return new CloudEventDataV1(data.toString());
        }
    }

    @Override
    public CloudEventData getData() {
        return this.data;
    }

    @Override
    public SpecVersion getSpecVersion() {
        return this.specVersion;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public URI getSource() {
        return this.source;
    }

    @Override
    public String getDataContentType() {
        return this.dataContentType;
    }

    @Override
    public URI getDataSchema() {
        return this.dataSchema;
    }

    @Override
    public String getSubject() {
        return this.subject;
    }

    @Override
    public OffsetDateTime getTime() {
        return this.time;
    }

    @Override
    public Object getAttribute(final String attributeName) throws IllegalArgumentException {
        return switch (attributeName) {
            case "specversion" -> getSpecVersion();
            case "id" -> getId();
            case "source" -> getSource();
            case "type" -> getType();
            case "datacontenttype" -> getDataContentType();
            case "dataschema" -> getDataSchema();
            case "subject" -> getSubject();
            case "time" -> getTime();
            default -> throw new IllegalArgumentException(
                    "The specified attribute name \"" + attributeName + "\" is not specified in version v1.");
        };
    }

    @Override
    public Object getExtension(final String s) {
        if (s == null) {
            throw new IllegalArgumentException("Extension name cannot be null");
        }
        return this.extensions.get(s);
    }

    @Override
    public Set<String> getExtensionNames() {
        return this.extensions.keySet();
    }
}
