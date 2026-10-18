package io.quarkus.arc.processor;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

/**
 * Represents a transformation of an annotation target.
 * <p>
 * The transformation is not applied until the {@link Transformation#done()} method is invoked.
 *
 * @see AnnotationsTransformer
 */
public final class Transformation implements AnnotationsTransformation<Transformation> {
    private static final Logger LOG = Logger.getLogger(Transformation.class);

    private final AnnotationTransformation.TransformationContext ctx;
    private final Collection<AnnotationInstance> modifiedAnnotations;

    Transformation(AnnotationTransformation.TransformationContext ctx) {
        this.ctx = ctx;
        this.modifiedAnnotations = new HashSet<>(ctx.annotations());

        if (LOG.isTraceEnabled()) {
            String stack = Arrays.stream(Thread.currentThread().getStackTrace())
                    .skip(2)
                    .limit(7)
                    .map(se -> "\n\t" + se.toString())
                    .collect(Collectors.joining());
            LOG.tracef("Transforming annotations of %s %s\n\t...", ctx.declaration(), stack);
        }
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
        LOG.tracef("Annotations of %s transformed: %s", ctx.declaration(), modifiedAnnotations);
        ctx.removeAll();
        ctx.addAll(modifiedAnnotations);
    }

}
