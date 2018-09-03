package org.jboss.protean.arc;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.enterprise.inject.spi.InterceptionType;
import javax.interceptor.InvocationContext;

/**
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
public interface InjectableInterceptor<T> extends InjectableBean<T> {

    Set<Annotation> getInterceptorBindings();

    boolean intercepts(InterceptionType type);

    Object intercept(InterceptionType type, T instance, InvocationContext ctx) throws Exception;

    int getPriority();

}
