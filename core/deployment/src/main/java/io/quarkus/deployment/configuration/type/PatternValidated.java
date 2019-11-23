package io.quarkus.deployment.configuration.type;

import java.util.Objects;

/**
 *
 */
public final class PatternValidated extends ConverterType {
    private final ConverterType type;
    private final String patternString;
    private int hashCode;

    public PatternValidated(final ConverterType type, final String patternString) {
        this.type = type;
        this.patternString = patternString;
    }

    public ConverterType getNestedType() {
        return type;
    }

    public String getPatternString() {
        return patternString;
    }

    @Override
    public int hashCode() {
        int hashCode = this.hashCode;
        if (hashCode == 0) {
            hashCode = Objects.hash(type, patternString);
            if (hashCode == 0) {
                hashCode = 0x8000_0000;
            }
            this.hashCode = hashCode;
        }
        return hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof PatternValidated && equals((PatternValidated) obj);
    }

    public boolean equals(final PatternValidated obj) {
        return obj == this || obj != null && type.equals(obj.type) && patternString.equals(obj.patternString);
    }

    @Override
    public Class<?> getLeafType() {
        return type.getLeafType();
    }
}
