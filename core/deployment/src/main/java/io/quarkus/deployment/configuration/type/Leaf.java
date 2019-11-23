package io.quarkus.deployment.configuration.type;

import java.util.Objects;

import org.eclipse.microprofile.config.spi.Converter;

/**
 *
 */
public final class Leaf extends ConverterType {
    private final Class<?> type;
    private final Class<? extends Converter<?>> convertWith;
    private int hashCode;

    public Leaf(final Class<?> type, final Class<?> convertWith) {
        this.type = type;
        this.convertWith = (Class<? extends Converter<?>>) convertWith;
    }

    @Override
    public Class<?> getLeafType() {
        return type;
    }

    public Class<? extends Converter<?>> getConvertWith() {
        return convertWith;
    }

    @Override
    public int hashCode() {
        int hashCode = this.hashCode;
        if (hashCode == 0) {
            hashCode = Objects.hash(type, convertWith);
            if (hashCode == 0) {
                hashCode = 0x8000_0000;
            }
            this.hashCode = hashCode;
        }
        return hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof Leaf && equals((Leaf) obj);
    }

    public boolean equals(final Leaf obj) {
        return obj == this || obj != null && type == obj.type && convertWith == obj.convertWith;
    }
}
