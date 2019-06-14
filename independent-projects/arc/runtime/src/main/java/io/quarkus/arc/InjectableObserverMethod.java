package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;
import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.ObserverMethod;

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

    static int compare(InjectableObserverMethod<?> o1, InjectableObserverMethod<?> o2) {
        return Integer.compare(o1.getPriority(), o2.getPriority());
    }

}
