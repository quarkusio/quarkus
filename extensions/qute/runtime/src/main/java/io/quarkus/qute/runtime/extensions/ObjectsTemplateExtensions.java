package io.quarkus.qute.runtime.extensions;

import java.util.Objects;

import jakarta.enterprise.inject.Vetoed;

import io.quarkus.qute.TemplateExtension;

@Vetoed // Make sure no bean is created from this class
public class ObjectsTemplateExtensions {

    @TemplateExtension(matchNames = { "eq", "==", "is" })
    static boolean eq(Object value, String ignoredName, Object other) {
        return Objects.equals(value, other);
    }
}
