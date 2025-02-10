package io.quarkus.extest.runtime.config;

import java.nio.charset.StandardCharsets;

import org.eclipse.microprofile.config.spi.Converter;

public class ByteArrayConfigConverter implements Converter<byte[]> {

    @Override
    public byte[] convert(final String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
