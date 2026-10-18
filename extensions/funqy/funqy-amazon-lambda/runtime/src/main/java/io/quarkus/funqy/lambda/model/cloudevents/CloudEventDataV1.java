package io.quarkus.funqy.lambda.model.cloudevents;

import java.nio.charset.StandardCharsets;

import io.cloudevents.CloudEventData;

public class CloudEventDataV1 implements CloudEventData {

    private final byte[] data;

    public CloudEventDataV1(final String data) {
        if (data == null) {
            this.data = null;
        } else {
            this.data = data.getBytes(StandardCharsets.UTF_8);
        }
    }

    public CloudEventDataV1(final byte[] data) {
        this.data = data;
    }

    @Override
    public byte[] toBytes() {
        return this.data;
    }
}
