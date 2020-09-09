package io.quarkus.hibernate.validator.spi;

import java.util.Collections;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * BuildItem used to publish the list of detected Bean Validation annotations
 * for consumption by other extensions.
 */
public final class BeanValidationAnnotationsBuildItem extends SimpleBuildItem {

    private final String valid;
    private final Set<String> constraints;
    private final Set<String> all;

    public BeanValidationAnnotationsBuildItem(String valid, Set<String> constraints, Set<String> all) {
        this.valid = valid;
        this.constraints = Collections.unmodifiableSet(constraints);
        this.all = Collections.unmodifiableSet(all);
    }

    public String getValidAnnotation() {
        return valid;
    }

    public Set<String> getConstraintAnnotations() {
        return constraints;
    }

    public Set<String> getAllAnnotations() {
        return all;
    }
}
