package io.quarkus.proxy.test.support;

import java.time.Instant;
import java.util.Objects;

public class XMessage {
    public static final byte CLOCK_READ = 1;
    public static final byte CLOCK_RESPONSE = 2;

    private byte type;
    private Instant time;

    public XMessage(byte type) {
        this.type = type;
    }

    public XMessage(byte type, Instant time) {
        this.type = type;
        this.time = Objects.requireNonNull(time);
    }

    public byte getType() {
        return type;
    }

    public Instant getTime() {
        return time;
    }
}
