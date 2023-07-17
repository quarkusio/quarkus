package io.quarkus.it.config;

import jakarta.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

@Priority(222)
public class MicroProfileCustomValueConverter implements Converter<MicroProfileCustomValue> {

    @Override
    public MicroProfileCustomValue convert(String value) {
        return new MicroProfileCustomValue(Integer.valueOf(value));
    }
}
