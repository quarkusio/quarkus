package org.jboss.protean.arc.processor;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;

import org.jboss.protean.arc.ClientProxy;
import org.jboss.protean.arc.CreationalContextImpl;
import org.jboss.protean.arc.InjectableBean;
import org.jboss.protean.arc.InjectableInterceptor;
import org.jboss.protean.arc.InjectableReferenceProvider;
import org.jboss.protean.arc.InvocationContextImpl.InterceptorInvocation;
import org.jboss.protean.arc.Reflections;
import org.jboss.protean.gizmo.MethodDescriptor;

/**
 *
 * @author Martin Kouba
 */
final class MethodDescriptors {

    static final MethodDescriptor CREATIONAL_CTX_CHILD = MethodDescriptor.ofMethod(CreationalContextImpl.class, "child", CreationalContextImpl.class,
            CreationalContext.class);

    static final MethodDescriptor MAP_GET = MethodDescriptor.ofMethod(Map.class, "get", Object.class, Object.class);

    static final MethodDescriptor INJECTABLE_REF_PROVIDER_GET = MethodDescriptor.ofMethod(InjectableReferenceProvider.class, "get", Object.class,
            CreationalContext.class);

    static final MethodDescriptor SET_ADD = MethodDescriptor.ofMethod(Set.class, "add", boolean.class, Object.class);

    static final MethodDescriptor LIST_ADD = MethodDescriptor.ofMethod(List.class, "add", boolean.class, Object.class);

    static final MethodDescriptor OBJECT_EQUALS = MethodDescriptor.ofMethod(Object.class, "equals", boolean.class, Object.class);

    static final MethodDescriptor OBJECT_CONSTRUCTOR = MethodDescriptor.ofConstructor(Object.class);

    static final MethodDescriptor INTERCEPTOR_INVOCATION_POST_CONSTRUCT = MethodDescriptor.ofMethod(InterceptorInvocation.class, "postConstruct",
            InterceptorInvocation.class, InjectableInterceptor.class, Object.class);

    static final MethodDescriptor INTERCEPTOR_INVOCATION_AROUND_CONSTRUCT = MethodDescriptor.ofMethod(InterceptorInvocation.class, "aroundConstruct",
            InterceptorInvocation.class, InjectableInterceptor.class, Object.class);

    static final MethodDescriptor REFLECTIONS_FIND_CONSTRUCTOR = MethodDescriptor.ofMethod(Reflections.class, "findConstructor", Constructor.class, Class.class,
            Class[].class);

    static final MethodDescriptor REFLECTIONS_WRITE_FIELD = MethodDescriptor.ofMethod(Reflections.class, "writeField", void.class, Class.class, String.class,
            Object.class, Object.class);

    static final MethodDescriptor REFLECTIONS_READ_FIELD = MethodDescriptor.ofMethod(Reflections.class, "readField", Object.class, Class.class, String.class,
            Object.class);

    static final MethodDescriptor REFLECTIONS_INVOKE_METHOD = MethodDescriptor.ofMethod(Reflections.class, "invokeMethod", Object.class, Class.class,
            String.class, Class[].class, Object.class, Object[].class);

    static final MethodDescriptor REFLECTIONS_NEW_INSTANCE = MethodDescriptor.ofMethod(Reflections.class, "newInstance", Object.class, Class.class,
            Class[].class, Object[].class);

    static final MethodDescriptor CLIENT_PROXY_GET_CONTEXTUAL_INSTANCE = MethodDescriptor.ofMethod(ClientProxy.class, "getContextualInstance", Object.class);

    static final MethodDescriptor INJECTABLE_BEAN_DESTROY = MethodDescriptor.ofMethod(InjectableBean.class, "destroy", void.class, Object.class,
            CreationalContext.class);

    private MethodDescriptors() {
    }

}
