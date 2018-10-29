package org.jboss.protean.arc.processor;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

/**
 * Convenient helper class.
 */
public final class Transformation {

    /**
     *
     * @param target
     * @param annotations
     * @return a new transformation
     */
    public static Transformation with(AnnotationTarget target, Collection<AnnotationInstance> annotations) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(annotations);
        return new Transformation(target, annotations);
    }

    private final AnnotationTarget target;

    private final List<AnnotationInstance> modified;

    public Transformation(AnnotationTarget target, Collection<AnnotationInstance> annotations) {
        this.target = target;
        this.modified = new ArrayList<>(annotations);
    }

    public Transformation add(AnnotationInstance annotation) {
        modified.add(annotation);
        return this;
    }

    public Transformation add(Class<? extends Annotation> annotationType, AnnotationValue... values) {
        add(DotNames.create(annotationType.getName()), values);
        return this;
    }

    public Transformation add(DotName name, AnnotationValue... values) {
        add(AnnotationInstance.create(name, target, values));
        return this;
    }

    public Transformation remove(Predicate<AnnotationInstance> predicate) {
        modified.removeIf(predicate);
        return this;
    }

    public Collection<AnnotationInstance> done() {
        return modified;
    }

}