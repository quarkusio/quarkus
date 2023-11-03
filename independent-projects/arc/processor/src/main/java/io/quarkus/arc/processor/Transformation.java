package io.quarkus.arc.processor;

import java.util.Collection;
import java.util.function.Consumer;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;

/**
 * Represents a transformation of an annotation target.
 * <p>
 * The transformation is not applied until the {@link Transformation#done()} method is invoked.
 *
 * @see AnnotationsTransformer
 */
public final class Transformation extends AbstractAnnotationsTransformation<Transformation, Collection<AnnotationInstance>> {

    public Transformation(Collection<AnnotationInstance> annotations, AnnotationTarget target,
            Consumer<Collection<AnnotationInstance>> transformationConsumer) {
        super(annotations, target, transformationConsumer);
    }

    @Override
    protected Transformation self() {
        return this;
    }

}
