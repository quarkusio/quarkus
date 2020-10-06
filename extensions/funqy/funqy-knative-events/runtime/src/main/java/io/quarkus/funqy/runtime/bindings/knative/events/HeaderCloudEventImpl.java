package io.quarkus.funqy.runtime.bindings.knative.events;

import java.time.OffsetDateTime;

import io.quarkus.funqy.knative.events.CloudEvent;
import io.vertx.core.http.HttpServerRequest;

class HeaderCloudEventImpl implements CloudEvent {
    String id;
    String specVersion;
    String source;
    String subject;
    OffsetDateTime time;

    final HttpServerRequest request;

    HeaderCloudEventImpl(HttpServerRequest request) {
        this.request = request;
    }

    @Override
    public String id() {
        if (id == null) {
            id = this.request.getHeader("ce-id");
        }

        return id;
    }

    @Override
    public String specVersion() {
        if (specVersion == null) {
            specVersion = this.request.getHeader("ce-specversion");
        }

        return specVersion;
    }

    @Override
    public String source() {
        if (source == null) {
            source = this.request.getHeader("ce-source");
        }

        return source;
    }

    @Override
    public String subject() {
        if (subject == null) {
            subject = this.request.getHeader("ce-subject");
        }

        return subject;
    }

    @Override
    public OffsetDateTime time() {
        if (time == null) {
            String t = this.request.getHeader("ce-time");
            if (t != null) {
                time = OffsetDateTime.parse(t);
            }
        }

        return time;
    }
}
