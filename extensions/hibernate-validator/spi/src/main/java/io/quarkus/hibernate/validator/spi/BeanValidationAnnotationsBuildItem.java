package io.quarkus.hibernate.validator.spi;

import java.util.Collections;
import java.util.Set;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * BuildItem used to publish the list of detected Bean Validation annotations
 * for consumption by other extensions.
 */
public final class BeanValidationAnnotationsBuildItem extends SimpleBuildItem {

    private final DotName valid;
    private final Set<DotName> constraints;
    private final Set<DotName> all;

    public BeanValidationAnnotationsBuildItem(DotName valid, Set<DotName> constraints, Set<DotName> all) {
        this.valid = valid;
        this.constraints = Collections.unmodifiableSet(constraints);
        this.all = Collections.unmodifiableSet(all);
    }

    public DotName getValidAnnotation() {
        return valid;
    }

    public Set<DotName> getConstraintAnnotations() {
        return constraints;
    }

    public Set<DotName> getAllAnnotations() {
        return all;
    }
}
