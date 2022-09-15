package io.quarkus.arc.processor;

import io.quarkus.gizmo.MethodCreator;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.ObserverMethod;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

/**
 * Configures a synthetic observer.
 * <p>
 * This construct is not thread-safe.
 *
 * @see ObserverRegistrar
 */
public final class ObserverConfigurator extends ConfiguratorBase<ObserverConfigurator> implements Consumer<AnnotationInstance> {

    final Consumer<ObserverConfigurator> consumer;
    String id;
    DotName beanClass;
    Type observedType;
    final Set<AnnotationInstance> observedQualifiers;
    int priority;
    boolean isAsync;
    TransactionPhase transactionPhase;
    Consumer<MethodCreator> notifyConsumer;

    public ObserverConfigurator(Consumer<ObserverConfigurator> consumer) {
        this.consumer = consumer;
        this.observedQualifiers = new HashSet<>();
        this.priority = ObserverMethod.DEFAULT_PRIORITY;
        this.isAsync = false;
        this.transactionPhase = TransactionPhase.IN_PROGRESS;
    }

    /**
     * A unique identifier should be used for multiple synthetic observer methods with the same
     * attributes (including the bean class).
     *
     * @param id
     * @return self
     */
    public ObserverConfigurator id(String id) {
        this.id = id;
        return this;
    }

    public ObserverConfigurator beanClass(DotName beanClass) {
        this.beanClass = beanClass;
        return this;
    }

    public ObserverConfigurator observedType(Class<?> observedType) {
        this.observedType = Type.create(DotName.createSimple(observedType.getName()), Kind.CLASS);
        return this;
    }

    public ObserverConfigurator observedType(Type observedType) {
        this.observedType = observedType;
        return this;
    }

    public ObserverConfigurator addQualifier(Class<? extends Annotation> annotationClass) {
        return addQualifier(DotName.createSimple(annotationClass.getName()));
    }

    public ObserverConfigurator addQualifier(DotName annotationName) {
        return addQualifier(AnnotationInstance.create(annotationName, null, new AnnotationValue[] {}));
    }

    public ObserverConfigurator addQualifier(AnnotationInstance qualifier) {
        this.observedQualifiers.add(qualifier);
        return this;
    }

    public QualifierConfigurator<ObserverConfigurator> addQualifier() {
        return new QualifierConfigurator<>(this);
    }

    public ObserverConfigurator qualifiers(AnnotationInstance... qualifiers) {
        Collections.addAll(this.observedQualifiers, qualifiers);
        return this;
    }

    public ObserverConfigurator priority(int priority) {
        this.priority = priority;
        return this;
    }

    public ObserverConfigurator async(boolean value) {
        this.isAsync = value;
        return this;
    }

    public ObserverConfigurator transactionPhase(TransactionPhase transactionPhase) {
        this.transactionPhase = transactionPhase;
        return this;
    }

    public ObserverConfigurator notify(Consumer<MethodCreator> notifyConsumer) {
        this.notifyConsumer = notifyConsumer;
        return this;
    }

    public void done() {
        if (beanClass == null) {
            throw new IllegalStateException("Observer bean class must be set!");
        }
        if (observedType == null) {
            throw new IllegalStateException("Observed type must be set!");
        }
        if (notifyConsumer == null) {
            throw new IllegalStateException("Bytecode generator for notify() method must be set!");
        }
        consumer.accept(this);
    }

    @Override
    public void accept(AnnotationInstance qualifier) {
        addQualifier(qualifier);
    }

}
