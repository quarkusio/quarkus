package io.quarkus.it.config;

import javax.annotation.Priority;

import org.eclipse.microprofile.config.spi.Converter;

@Priority(222)
public class MicroProfileCustomValueConverter implements Converter<MicroProfileCustomValue> {

    @Override
    public MicroProfileCustomValue convert(String value) {
        return new MicroProfileCustomValue(Integer.valueOf(value));
    }
}
