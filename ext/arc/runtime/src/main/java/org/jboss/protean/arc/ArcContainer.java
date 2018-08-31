package org.jboss.protean.arc;

import java.lang.annotation.Annotation;

import javax.enterprise.context.spi.Context;
import javax.enterprise.util.TypeLiteral;

/**
 *
 * @author Martin Kouba
 */
public interface ArcContainer {

    Context getContext(Class<? extends Annotation> scopeType);

    <T> InstanceHandle<T> instance(Class<T> type, Annotation... qualifiers);

    <T> InstanceHandle<T> instance(TypeLiteral<T> type, Annotation... qualifiers);

    RequestContext requestContext();

    void withinRequest(Runnable action);

}
