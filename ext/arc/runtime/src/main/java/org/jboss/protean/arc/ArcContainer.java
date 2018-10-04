package org.jboss.protean.arc;

import java.lang.annotation.Annotation;

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

    void withinRequest(Runnable action);

}
