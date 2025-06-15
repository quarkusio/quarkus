package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.event.Reception;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.ObserverMethod;

import io.quarkus.arc.impl.EventContextImpl;
import io.quarkus.arc.impl.EventMetadataImpl;

/**
 * Represents an observer method.
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
public interface InjectableObserverMethod<T> extends ObserverMethod<T> {

    @Override
    default Set<Annotation> getObservedQualifiers() {
        return Collections.emptySet();
    }

    @Override
    default Reception getReception() {
        return Reception.ALWAYS;
    }

    @Override
    default TransactionPhase getTransactionPhase() {
        return TransactionPhase.IN_PROGRESS;
    }

    @Override
    default Bean<?> getDeclaringBean() {
        return getDeclaringBeanIdentifier() != null ? Arc.container().bean(getDeclaringBeanIdentifier()) : null;
    }

    @Override
    default void notify(T event) {
        notify(new EventContextImpl<>(event,
                new EventMetadataImpl(getObservedQualifiers(), event.getClass(), null)));
    }

    /**
     *
     * @return the identifier or null for synthetic observers
     * @see InjectableBean#getIdentifier()
     */
    String getDeclaringBeanIdentifier();

    static int compare(InjectableObserverMethod<?> o1, InjectableObserverMethod<?> o2) {
        return Integer.compare(o1.getPriority(), o2.getPriority());
    }

}
