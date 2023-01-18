package io.quarkus.qute.runtime;

import jakarta.enterprise.util.AnnotationLiteral;

import io.quarkus.qute.Location;

/**
 * Supports inline instantiation of {@link Location}.
 */
public class LocationLiteral extends AnnotationLiteral<Location> implements Location {

    private static final long serialVersionUID = 1L;

    private final String value;

    public LocationLiteral(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

}
