package io.quarkus.jfr.runtime;

import java.security.SecureRandom;
import java.util.HexFormat;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class QuarkusIdProducer implements IdProducer {

    private static final SecureRandom random = new SecureRandom();
    private final String traceId;

    public QuarkusIdProducer() {
        final byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        traceId = HexFormat.of().formatHex(bytes);
    }

    @Override
    public String getTraceId() {
        return traceId;
    }

    @Override
    public String getSpanId() {
        return null;
    }
}
