package io.quarkus.funqy.test;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEventMapping;

public class BadBinaryInputRaw {
    @Funq
    @CloudEventMapping(trigger = "test-bad-input-raw")
    public void badInputRaw(Byte[] data) {
    }
}
