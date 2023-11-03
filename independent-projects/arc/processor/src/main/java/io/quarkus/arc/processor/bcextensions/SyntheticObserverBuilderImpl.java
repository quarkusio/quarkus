package io.quarkus.arc.processor.bcextensions;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticObserver;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticObserverBuilder;
import jakarta.enterprise.inject.spi.ObserverMethod;
import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.declarations.ClassInfo;

import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.Annotations;

class SyntheticObserverBuilderImpl<T> extends SyntheticComponentBuilderBase<SyntheticObserverBuilderImpl<T>>
        implements SyntheticObserverBuilder<T> {
    DotName declaringClass;
    org.jboss.jandex.Type type;
    Set<org.jboss.jandex.AnnotationInstance> qualifiers = new HashSet<>();
    int priority = ObserverMethod.DEFAULT_PRIORITY;
    boolean isAsync;
    TransactionPhase transactionPhase = TransactionPhase.IN_PROGRESS;
    Class<? extends SyntheticObserver<?>> implementationClass;

    SyntheticObserverBuilderImpl(DotName extensionClass, org.jboss.jandex.Type eventType) {
        this.declaringClass = extensionClass;
        this.type = eventType;
    }

    @Override
    SyntheticObserverBuilderImpl<T> self() {
        return this;
    }

    @Override
    public SyntheticObserverBuilder<T> declaringClass(Class<?> declaringClass) {
        this.declaringClass = DotName.createSimple(declaringClass.getName());
        return this;
    }

    @Override
    public SyntheticObserverBuilder<T> declaringClass(ClassInfo declaringClass) {
        this.declaringClass = ((ClassInfoImpl) declaringClass).jandexDeclaration.name();
        return this;
    }

    @Override
    public SyntheticObserverBuilder<T> qualifier(Class<? extends Annotation> qualifierAnnotation) {
        DotName annotationName = DotName.createSimple(qualifierAnnotation.getName());
        this.qualifiers.add(org.jboss.jandex.AnnotationInstance.create(annotationName, null, AnnotationValueArray.EMPTY));
        return this;
    }

    @Override
    public SyntheticObserverBuilder<T> qualifier(AnnotationInfo qualifierAnnotation) {
        this.qualifiers.add(((AnnotationInfoImpl) qualifierAnnotation).jandexAnnotation);
        return this;
    }

    @Override
    public SyntheticObserverBuilder<T> qualifier(Annotation qualifierAnnotation) {
        this.qualifiers.add(Annotations.jandexAnnotation(qualifierAnnotation));
        return this;
    }

    @Override
    public SyntheticObserverBuilder<T> priority(int priority) {
        this.priority = priority;
        return this;
    }

    @Override
    public SyntheticObserverBuilder<T> async(boolean isAsync) {
        this.isAsync = isAsync;
        return this;
    }

    @Override
    public SyntheticObserverBuilder<T> transactionPhase(TransactionPhase transactionPhase) {
        this.transactionPhase = transactionPhase;
        return this;
    }

    @Override
    public SyntheticObserverBuilder<T> observeWith(Class<? extends SyntheticObserver<T>> syntheticObserverClass) {
        this.implementationClass = syntheticObserverClass;
        return this;
    }
}
