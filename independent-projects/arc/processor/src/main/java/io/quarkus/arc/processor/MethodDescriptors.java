package io.quarkus.arc.processor;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableBean.Kind;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InjectableInterceptor;
import io.quarkus.arc.InjectableReferenceProvider;
import io.quarkus.arc.impl.ClientProxies;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.impl.DecoratorDelegateProvider;
import io.quarkus.arc.impl.FixedValueSupplier;
import io.quarkus.arc.impl.InjectableReferenceProviders;
import io.quarkus.arc.impl.InterceptedMethodMetadata;
import io.quarkus.arc.impl.InterceptorInvocation;
import io.quarkus.arc.impl.InvocationContexts;
import io.quarkus.arc.impl.MapValueSupplier;
import io.quarkus.arc.impl.Objects;
import io.quarkus.arc.impl.Reflections;
import io.quarkus.arc.impl.RemovedBeanImpl;
import io.quarkus.gizmo.MethodDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.EventContext;
import javax.enterprise.inject.spi.EventMetadata;
import javax.interceptor.InvocationContext;

/**
 *
 * @author Martin Kouba
 */
public final class MethodDescriptors {

    public static final MethodDescriptor FIXED_VALUE_SUPPLIER_CONSTRUCTOR = MethodDescriptor.ofConstructor(
            FixedValueSupplier.class,
            Object.class);

    public static final MethodDescriptor MAP_VALUE_SUPPLIER_CONSTRUCTOR = MethodDescriptor.ofConstructor(MapValueSupplier.class,
            Map.class, String.class);

    public static final MethodDescriptor SUPPLIER_GET = MethodDescriptor.ofMethod(Supplier.class, "get", Object.class);

    public static final MethodDescriptor CREATIONAL_CTX_CHILD = MethodDescriptor.ofMethod(CreationalContextImpl.class, "child",
            CreationalContextImpl.class,
            CreationalContext.class);

    public static final MethodDescriptor CREATIONAL_CTX_CHILD_CONTEXTUAL = MethodDescriptor.ofMethod(
            CreationalContextImpl.class,
            "child",
            CreationalContextImpl.class,
            InjectableReferenceProvider.class,
            CreationalContext.class);

    public static final MethodDescriptor MAP_GET = MethodDescriptor.ofMethod(Map.class, "get", Object.class, Object.class);

    public static final MethodDescriptor MAP_PUT = MethodDescriptor.ofMethod(Map.class, "put", Object.class, Object.class,
            Object.class);

    public static final MethodDescriptor INJECTABLE_REF_PROVIDER_GET = MethodDescriptor.ofMethod(
            InjectableReferenceProvider.class,
            "get", Object.class,
            CreationalContext.class);

    public static final MethodDescriptor SET_ADD = MethodDescriptor.ofMethod(Set.class, "add", boolean.class, Object.class);

    public static final MethodDescriptor LIST_ADD = MethodDescriptor.ofMethod(List.class, "add", boolean.class, Object.class);

    public static final MethodDescriptor OBJECT_EQUALS = MethodDescriptor.ofMethod(Object.class, "equals", boolean.class,
            Object.class);

    /**
     * No longer used - will be deleted
     * 
     * @deprecated
     */
    @Deprecated
    public static final MethodDescriptor OBJECT_HASH_CODE = MethodDescriptor.ofMethod(Object.class, "hashCode", int.class);

    /**
     * No longer used - will be deleted
     * 
     * @deprecated
     */
    @Deprecated
    public static final MethodDescriptor OBJECT_TO_STRING = MethodDescriptor.ofMethod(Object.class, "toString", String.class);

    public static final MethodDescriptor OBJECT_CONSTRUCTOR = MethodDescriptor.ofConstructor(Object.class);

    /**
     * No longer used - will be deleted
     * 
     * @deprecated
     */
    @Deprecated
    public static final MethodDescriptor OBJECTS_REFERENCE_EQUALS = MethodDescriptor.ofMethod(Objects.class, "referenceEquals",
            boolean.class, Object.class, Object.class);

    public static final MethodDescriptor INTERCEPTOR_INVOCATION_POST_CONSTRUCT = MethodDescriptor.ofMethod(
            InterceptorInvocation.class,
            "postConstruct",
            InterceptorInvocation.class, InjectableInterceptor.class, Object.class);

    public static final MethodDescriptor INTERCEPTOR_INVOCATION_PRE_DESTROY = MethodDescriptor.ofMethod(
            InterceptorInvocation.class,
            "preDestroy",
            InterceptorInvocation.class, InjectableInterceptor.class, Object.class);

    public static final MethodDescriptor INTERCEPTOR_INVOCATION_AROUND_CONSTRUCT = MethodDescriptor.ofMethod(
            InterceptorInvocation.class, "aroundConstruct",
            InterceptorInvocation.class, InjectableInterceptor.class, Object.class);

    public static final MethodDescriptor INTERCEPTOR_INVOCATION_AROUND_INVOKE = MethodDescriptor.ofMethod(
            InterceptorInvocation.class,
            "aroundInvoke",
            InterceptorInvocation.class, InjectableInterceptor.class, Object.class);

    public static final MethodDescriptor REFLECTIONS_FIND_CONSTRUCTOR = MethodDescriptor.ofMethod(Reflections.class,
            "findConstructor",
            Constructor.class, Class.class,
            Class[].class);

    public static final MethodDescriptor REFLECTIONS_FIND_METHOD = MethodDescriptor.ofMethod(Reflections.class, "findMethod",
            Method.class, Class.class, String.class,
            Class[].class);

    public static final MethodDescriptor REFLECTIONS_FIND_FIELD = MethodDescriptor.ofMethod(Reflections.class, "findField",
            Field.class, Class.class, String.class);

    public static final MethodDescriptor REFLECTIONS_WRITE_FIELD = MethodDescriptor.ofMethod(Reflections.class, "writeField",
            void.class, Class.class, String.class,
            Object.class, Object.class);

    public static final MethodDescriptor REFLECTIONS_READ_FIELD = MethodDescriptor.ofMethod(Reflections.class, "readField",
            Object.class, Class.class, String.class,
            Object.class);

    public static final MethodDescriptor REFLECTIONS_INVOKE_METHOD = MethodDescriptor.ofMethod(Reflections.class,
            "invokeMethod",
            Object.class, Class.class,
            String.class, Class[].class, Object.class, Object[].class);

    public static final MethodDescriptor REFLECTIONS_NEW_INSTANCE = MethodDescriptor.ofMethod(Reflections.class, "newInstance",
            Object.class, Class.class,
            Class[].class, Object[].class);

    public static final MethodDescriptor CLIENT_PROXY_GET_CONTEXTUAL_INSTANCE = MethodDescriptor.ofMethod(ClientProxy.class,
            ClientProxyGenerator.GET_CONTEXTUAL_INSTANCE_METHOD_NAME, Object.class);

    public static final MethodDescriptor INJECTABLE_BEAN_DESTROY = MethodDescriptor.ofMethod(InjectableBean.class, "destroy",
            void.class, Object.class,
            CreationalContext.class);

    public static final MethodDescriptor INJECTABLE_REFERENCE_PROVIDERS_DESTROY = MethodDescriptor.ofMethod(
            InjectableReferenceProviders.class, "destroy", void.class, InjectableReferenceProvider.class, Object.class,
            CreationalContext.class);

    public static final MethodDescriptor CREATIONAL_CTX_RELEASE = MethodDescriptor.ofMethod(CreationalContext.class, "release",
            void.class);

    public static final MethodDescriptor EVENT_CONTEXT_GET_EVENT = MethodDescriptor.ofMethod(EventContext.class, "getEvent",
            Object.class);

    public static final MethodDescriptor EVENT_CONTEXT_GET_METADATA = MethodDescriptor.ofMethod(EventContext.class,
            "getMetadata",
            EventMetadata.class);

    public static final MethodDescriptor INVOCATION_CONTEXTS_PERFORM_AROUND_INVOKE = MethodDescriptor.ofMethod(
            InvocationContexts.class,
            "performAroundInvoke",
            Object.class, Object.class, Method.class, Function.class, Object[].class, List.class,
            Set.class);

    public static final MethodDescriptor INVOCATION_CONTEXTS_AROUND_CONSTRUCT = MethodDescriptor.ofMethod(
            InvocationContexts.class,
            "aroundConstruct",
            InvocationContext.class, Constructor.class, List.class, Supplier.class, Set.class);

    public static final MethodDescriptor INVOCATION_CONTEXTS_POST_CONSTRUCT = MethodDescriptor.ofMethod(
            InvocationContexts.class,
            "postConstruct",
            InvocationContext.class, Object.class, List.class, Set.class);

    public static final MethodDescriptor INVOCATION_CONTEXTS_PRE_DESTROY = MethodDescriptor.ofMethod(InvocationContexts.class,
            "preDestroy",
            InvocationContext.class, Object.class, List.class, Set.class);

    public static final MethodDescriptor INVOCATION_CONTEXT_PROCEED = MethodDescriptor.ofMethod(InvocationContext.class,
            "proceed",
            Object.class);

    public static final MethodDescriptor INVOCATION_CONTEXT_GET_TARGET = MethodDescriptor.ofMethod(InvocationContext.class,
            "getTarget",
            Object.class);

    public static final MethodDescriptor CREATIONAL_CTX_ADD_DEP_TO_PARENT = MethodDescriptor.ofMethod(
            CreationalContextImpl.class,
            "addDependencyToParent", void.class,
            InjectableBean.class, Object.class, CreationalContext.class);

    public static final MethodDescriptor COLLECTIONS_UNMODIFIABLE_SET = MethodDescriptor.ofMethod(Collections.class,
            "unmodifiableSet",
            Set.class, Set.class);

    public static final MethodDescriptor COLLECTIONS_SINGLETON = MethodDescriptor.ofMethod(Collections.class, "singleton",
            Set.class, Object.class);

    public static final MethodDescriptor COLLECTIONS_SINGLETON_LIST = MethodDescriptor.ofMethod(Collections.class,
            "singletonList",
            List.class, Object.class);

    public static final MethodDescriptor COLLECTIONS_EMPTY_MAP = MethodDescriptor.ofMethod(Collections.class, "emptyMap",
            Map.class);

    public static final MethodDescriptor SET_OF = MethodDescriptor.ofMethod(Set.class, "of", Set.class, Object[].class);

    public static final MethodDescriptor ARC_CONTAINER = MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class);

    public static final MethodDescriptor ARC_CONTAINER_BEAN = MethodDescriptor.ofMethod(ArcContainer.class, "bean",
            InjectableBean.class, String.class);

    public static final MethodDescriptor ARC_CONTAINER_GET_ACTIVE_CONTEXT = MethodDescriptor.ofMethod(ArcContainer.class,
            "getActiveContext", InjectableContext.class, Class.class);

    public static final MethodDescriptor CONTEXT_GET = MethodDescriptor.ofMethod(Context.class, "get", Object.class,
            Contextual.class,
            CreationalContext.class);

    public static final MethodDescriptor CONTEXT_GET_IF_PRESENT = MethodDescriptor.ofMethod(Context.class, "get", Object.class,
            Contextual.class);

    public static final MethodDescriptor GET_IDENTIFIER = MethodDescriptor.ofMethod(InjectableBean.class, "getIdentifier",
            String.class);

    public static final MethodDescriptor INTERCEPTED_METHOD_METADATA_CONSTRUCTOR = MethodDescriptor.ofConstructor(
            InterceptedMethodMetadata.class,
            List.class, Method.class, Set.class);

    public static final MethodDescriptor CREATIONAL_CTX_HAS_DEPENDENT_INSTANCES = MethodDescriptor.ofMethod(
            CreationalContextImpl.class,
            "hasDependentInstances", boolean.class);

    public static final MethodDescriptor THREAD_CURRENT_THREAD = MethodDescriptor.ofMethod(Thread.class, "currentThread",
            Thread.class);

    public static final MethodDescriptor THREAD_GET_TCCL = MethodDescriptor.ofMethod(Thread.class, "getContextClassLoader",
            ClassLoader.class);

    public static final MethodDescriptor CL_FOR_NAME = MethodDescriptor.ofMethod(Class.class, "forName", Class.class,
            String.class,
            boolean.class, ClassLoader.class);

    public static final MethodDescriptor REMOVED_BEAN_IMPL = MethodDescriptor.ofConstructor(RemovedBeanImpl.class, Kind.class,
            String.class, Set.class, Set.class);

    public static final MethodDescriptor CLIENT_PROXIES_GET_APP_SCOPED_DELEGATE = MethodDescriptor.ofMethod(ClientProxies.class,
            "getApplicationScopedDelegate", Object.class, InjectableContext.class, InjectableBean.class);

    public static final MethodDescriptor CLIENT_PROXIES_GET_DELEGATE = MethodDescriptor.ofMethod(ClientProxies.class,
            "getDelegate", Object.class, InjectableBean.class);

    public static final MethodDescriptor DECORATOR_DELEGATE_PROVIDER_SET = MethodDescriptor
            .ofMethod(DecoratorDelegateProvider.class, "set", Object.class, Object.class);
    public static final MethodDescriptor DECORATOR_DELEGATE_PROVIDER_UNSET = MethodDescriptor
            .ofMethod(DecoratorDelegateProvider.class, "unset", void.class);
    public static final MethodDescriptor DECORATOR_DELEGATE_PROVIDER_GET = MethodDescriptor
            .ofMethod(DecoratorDelegateProvider.class, "get", Object.class);

    private MethodDescriptors() {
    }

}
