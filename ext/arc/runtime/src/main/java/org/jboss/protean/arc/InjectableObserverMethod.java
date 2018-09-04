package org.jboss.protean.arc;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.ObserverMethod;

/**
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

}
