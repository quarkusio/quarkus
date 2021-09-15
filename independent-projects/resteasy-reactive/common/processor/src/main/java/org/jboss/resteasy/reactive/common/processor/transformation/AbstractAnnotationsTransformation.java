package org.jboss.resteasy.reactive.common.processor.transformation;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;

abstract class AbstractAnnotationsTransformation<T extends AnnotationsTransformation<T>, C extends Collection<AnnotationInstance>>
        implements AnnotationsTransformation<T> {

    private final AnnotationTarget target;
    private final Consumer<C> resultConsumer;
    protected final C modifiedAnnotations;

    /**
     * 
     * @param annotations Mutable collection of annotations
     * @param target
     * @param resultConsumer
     */
    public AbstractAnnotationsTransformation(C annotations, AnnotationTarget target,
            Consumer<C> resultConsumer) {
        this.target = target;
        this.resultConsumer = resultConsumer;
        this.modifiedAnnotations = annotations;
    }

    public T add(AnnotationInstance annotation) {
        modifiedAnnotations.add(annotation);
        return self();
    }

    public T addAll(Collection<AnnotationInstance> annotations) {
        modifiedAnnotations.addAll(annotations);
        return self();
    }

    public T addAll(AnnotationInstance... annotations) {
        Collections.addAll(modifiedAnnotations, annotations);
        return self();
    }

    public T add(Class<? extends Annotation> annotationType, AnnotationValue... values) {
        add(DotName.createSimple(annotationType.getName()), values);
        return self();
    }

    public T add(DotName name, AnnotationValue... values) {
        add(AnnotationInstance.create(name, target, values));
        return self();
    }

    public T remove(Predicate<AnnotationInstance> predicate) {
        modifiedAnnotations.removeIf(predicate);
        return self();
    }

    public T removeAll() {
        modifiedAnnotations.clear();
        return self();
    }

    public void done() {
        resultConsumer.accept(modifiedAnnotations);
    }

    protected abstract T self();

}
