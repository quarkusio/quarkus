package org.jboss.resteasy.reactive.common.processor.transformation;

import java.util.Collection;
import java.util.function.Consumer;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;

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
