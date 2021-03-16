package io.quarkus.funqy.test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEventMapping;

public class BinaryPayload {

    @Funq
    @CloudEventMapping(trigger = "test-type")
    public byte[] doubleInt32BE(byte[] data) {
        int i = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getInt();
        byte[] result = new byte[4];
        ByteBuffer.wrap(result).order(ByteOrder.BIG_ENDIAN).putInt(i * 2);
        return result;
    }
}
