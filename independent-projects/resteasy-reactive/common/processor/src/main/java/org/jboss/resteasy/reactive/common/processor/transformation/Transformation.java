package org.jboss.resteasy.reactive.common.processor.transformation;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.function.Predicate;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

public final class Transformation implements AnnotationsTransformation<Transformation> {

    private final AnnotationTransformation.TransformationContext ctx;
    private final Collection<AnnotationInstance> modifiedAnnotations;

    Transformation(AnnotationTransformation.TransformationContext ctx) {
        this.ctx = ctx;
        this.modifiedAnnotations = new HashSet<>(ctx.annotations());
    }

    public Transformation add(AnnotationInstance annotation) {
        modifiedAnnotations.add(annotation);
        return this;
    }

    public Transformation addAll(Collection<AnnotationInstance> annotations) {
        modifiedAnnotations.addAll(annotations);
        return this;
    }

    public Transformation addAll(AnnotationInstance... annotations) {
        Collections.addAll(modifiedAnnotations, annotations);
        return this;
    }

    public Transformation add(Class<? extends Annotation> annotationType, AnnotationValue... values) {
        add(DotName.createSimple(annotationType.getName()), values);
        return this;
    }

    public Transformation add(DotName name, AnnotationValue... values) {
        add(AnnotationInstance.create(name, ctx.declaration(), values));
        return this;
    }

    public Transformation remove(Predicate<AnnotationInstance> predicate) {
        modifiedAnnotations.removeIf(predicate);
        return this;
    }

    public Transformation removeAll() {
        modifiedAnnotations.clear();
        return this;
    }

    public void done() {
        ctx.removeAll();
        ctx.addAll(modifiedAnnotations);
    }

}
