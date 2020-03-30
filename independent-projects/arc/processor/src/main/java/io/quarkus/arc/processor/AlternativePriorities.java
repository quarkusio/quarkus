package io.quarkus.arc.processor;

import java.util.Collection;
import org.jboss.jandex.AnnotationTarget;

/**
 * Represents an additional way of defining priorities for alternative beans.
 */
public interface AlternativePriorities {

    /**
     * 
     * @param target The bean class, producer method or field
     * @param stereotypes The collection of stereotypes
     * @return a computed priority value or {@code null}
     */
    Integer compute(AnnotationTarget target, Collection<StereotypeInfo> stereotypes);

}
