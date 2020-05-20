package io.quarkus.funqy.knative.events;

import java.time.OffsetDateTime;

/**
 * Cloud event. Represents only the headers. No data.
 *
 */
public interface CloudEvent {
    String id();

    String specVersion();

    String source();

    String subject();

    OffsetDateTime time();
}
