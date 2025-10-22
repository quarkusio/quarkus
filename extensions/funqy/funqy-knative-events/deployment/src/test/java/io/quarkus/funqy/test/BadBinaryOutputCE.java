package io.quarkus.funqy.test;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventBuilder;
import io.quarkus.funqy.knative.events.CloudEventMapping;

public class BadBinaryOutputCE {
    @Funq
    @CloudEventMapping(trigger = "test-bad-output-ce")
    public CloudEvent<Byte[]> badOutputCE() {
        return CloudEventBuilder.create()
                .specVersion("1.0")
                .id("test-bad-output-ce")
                .type("badOutputCE")
                .source("/badOutputCE")
                .build(new Byte[] {});
    }
}
