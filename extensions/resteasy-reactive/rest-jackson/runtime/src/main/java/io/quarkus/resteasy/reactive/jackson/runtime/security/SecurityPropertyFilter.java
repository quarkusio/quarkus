package io.quarkus.resteasy.reactive.jackson.runtime.security;

import static io.quarkus.resteasy.reactive.jackson.runtime.mappers.JacksonMapperUtil.includeSecureField;

import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;

import io.quarkus.resteasy.reactive.jackson.SecureField;

public class SecurityPropertyFilter extends SimpleBeanPropertyFilter {

    static final String FILTER_ID = "securityFilter";

    @Override
    protected boolean include(PropertyWriter writer) {
        SecureField secureField = writer.getAnnotation(SecureField.class);
        return secureField == null ? super.include(writer) : includeSecureField(secureField.rolesAllowed());
    }
}
