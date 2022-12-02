package io.quarkus.runtime.configuration;

import java.util.Objects;

import org.eclipse.microprofile.config.spi.Converter;

final class ConverterClassHolder {
    Class<?> type;
    Class<? extends Converter<?>> converterType;

    public ConverterClassHolder(Class<?> type, Class<? extends Converter<?>> converterType) {
        this.type = type;
        this.converterType = converterType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ConverterClassHolder that = (ConverterClassHolder) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(converterType, that.converterType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, converterType);
    }
}
