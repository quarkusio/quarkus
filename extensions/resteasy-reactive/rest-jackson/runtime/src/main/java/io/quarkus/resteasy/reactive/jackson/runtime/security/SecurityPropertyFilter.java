package io.quarkus.resteasy.reactive.jackson.runtime.security;

import static io.quarkus.resteasy.reactive.jackson.runtime.mappers.JacksonMapperUtil.includeSecureField;

import io.quarkus.resteasy.reactive.jackson.SecureField;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;

public class SecurityPropertyFilter extends SimpleBeanPropertyFilter {

    static final String FILTER_ID = "securityFilter";

    @Override
    protected boolean include(PropertyWriter writer) {
        SecureField secureField = writer.getAnnotation(SecureField.class);
        return secureField == null ? super.include(writer) : includeSecureField(secureField.rolesAllowed());
    }
}
