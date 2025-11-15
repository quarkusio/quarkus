package io.quarkus.funqy.test;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;

public class BadBinaryInputCE {
    @Funq
    @CloudEventMapping(trigger = "test-bad-input-ce")
    public void badInputCE(CloudEvent<Byte[]> data) {
    }
}
