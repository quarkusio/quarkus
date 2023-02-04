package io.quarkus.qute.runtime.extensions;

import java.util.Optional;

import jakarta.enterprise.inject.Vetoed;

import io.quarkus.qute.Results;
import io.quarkus.qute.TemplateExtension;

@Vetoed // Make sure no bean is created from this class
@TemplateExtension
public class OrOperatorTemplateExtensions {

    static <T> T or(T value, T other) {
        if (value == null || Results.isNotFound(value) || (value instanceof Optional && ((Optional<?>) value).isEmpty())) {
            return other;
        }
        return value;
    }

}
