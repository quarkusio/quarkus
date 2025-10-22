package io.quarkus.funqy.test;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEventMapping;

public class BadBinaryOutputRaw {
    @Funq
    @CloudEventMapping(trigger = "test-bad-output-raw")
    public Byte[] badOutputRaw() {
        return new Byte[] {};
    }
}
