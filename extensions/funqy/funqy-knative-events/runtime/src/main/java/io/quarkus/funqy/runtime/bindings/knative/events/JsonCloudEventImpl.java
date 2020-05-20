package io.quarkus.funqy.runtime.bindings.knative.events;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.funqy.knative.events.CloudEvent;

class JsonCloudEventImpl implements CloudEvent {
    String id;
    String specVersion;
    String source;
    String subject;
    OffsetDateTime time;

    final JsonNode event;

    public JsonCloudEventImpl(JsonNode event) {
        this.event = event;
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
            if (specVersion != null)
                this.specVersion = specVersion.asText();
        }

        return specVersion;
    }

    @Override
    public String source() {
        if (source == null) {
            JsonNode source = event.get("source");
            if (source != null)
                this.source = source.asText();
        }

        return source;
    }

    @Override
    public String subject() {
        if (subject == null) {
            JsonNode subject = event.get("subject");
            if (subject != null)
                this.subject = subject.asText();
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
}
