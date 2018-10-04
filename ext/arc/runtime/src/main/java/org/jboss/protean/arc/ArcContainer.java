package org.jboss.protean.arc;

import java.lang.annotation.Annotation;
import java.util.function.Supplier;

import javax.enterprise.util.TypeLiteral;

/**
 *
 * @author Martin Kouba
 */
public interface ArcContainer {

    /**
     *
     * @param scopeType
     * @return the context for the given scope, does not throw {@link javax.enterprise.context.ContextNotActiveException}
     */
    InjectableContext getContext(Class<? extends Annotation> scopeType);

    <T> InstanceHandle<T> instance(Class<T> type, Annotation... qualifiers);

    <T> InstanceHandle<T> instance(TypeLiteral<T> type, Annotation... qualifiers);

    /**
     *
     * @return the context for {@link javax.enterprise.context.RequestScoped}
     */
    ManagedContext requestContext();

    /**
     * Ensures the action is perform with the request context active. Does not manage the context if it's already active.
     *
     * @param action
     */
    void withinRequest(Runnable action);

    /**
     * Ensures the action is perform with the request context active. Does not manage the context if it's already active.
     *
     * @param action
     */
    <T> T withinRequest(Supplier<T> action);

}
