package io.quarkus.datasource.runtime;

import org.eclipse.microprofile.config.spi.Converter;

import io.quarkus.datasource.common.runtime.DatabaseKind;

public class DatabaseKindConverter implements Converter<String> {

    @Override
    public String convert(String value) {
        return DatabaseKind.normalize(value);
    }

}
