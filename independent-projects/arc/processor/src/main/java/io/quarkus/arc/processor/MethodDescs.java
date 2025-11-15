package io.quarkus.arc.processor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.EventContext;
import jakarta.enterprise.inject.spi.EventMetadata;
import jakarta.interceptor.InvocationContext;

import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ArcInvocationContext;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.ComponentsProvider;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InjectableInterceptor;
import io.quarkus.arc.InjectableReferenceProvider;
import io.quarkus.arc.InterceptorCreator;
import io.quarkus.arc.impl.ClientProxies;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.impl.DecoratorDelegateProvider;
import io.quarkus.arc.impl.FixedValueSupplier;
import io.quarkus.arc.impl.InjectableReferenceProviders;
import io.quarkus.arc.impl.InjectionPointImpl;
import io.quarkus.arc.impl.InterceptedMethodMetadata;
import io.quarkus.arc.impl.InterceptorInvocation;
import io.quarkus.arc.impl.InvocationContexts;
import io.quarkus.arc.impl.MapValueSupplier;
import io.quarkus.arc.impl.Reflections;
import io.quarkus.arc.impl.RemovedBeanImpl;
import io.quarkus.arc.impl.Sets;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

final class MethodDescs {
    private MethodDescs() {
    }

    static final ConstructorDesc FIXED_VALUE_SUPPLIER_CONSTRUCTOR = ConstructorDesc.of(FixedValueSupplier.class,
            Object.class);

    static final ConstructorDesc MAP_VALUE_SUPPLIER_CONSTRUCTOR = ConstructorDesc.of(MapValueSupplier.class,
            Map.class, String.class);

    static final MethodDesc SUPPLIER_GET = MethodDesc.of(Supplier.class, "get", Object.class);

    static final MethodDesc CONSUMER_ACCEPT = MethodDesc.of(Consumer.class, "accept", void.class, Object.class);

    static final MethodDesc CREATIONAL_CTX_CHILD = MethodDesc.of(CreationalContextImpl.class,
            "child", CreationalContextImpl.class, CreationalContext.class);

    static final MethodDesc CREATIONAL_CTX_CHILD_CONTEXTUAL = MethodDesc.of(CreationalContextImpl.class,
            "child", CreationalContextImpl.class, InjectableReferenceProvider.class, CreationalContext.class);

    static final MethodDesc INJECTABLE_REF_PROVIDER_GET = MethodDesc.of(InjectableReferenceProvider.class,
            "get", Object.class, CreationalContext.class);

    static final ConstructorDesc OBJECT_CONSTRUCTOR = ConstructorDesc.of(Object.class);

    static final MethodDesc INTERCEPTOR_INVOCATION_POST_CONSTRUCT = MethodDesc.of(InterceptorInvocation.class,
            "postConstruct", InterceptorInvocation.class, InjectableInterceptor.class, Object.class);

    static final MethodDesc INTERCEPTOR_INVOCATION_PRE_DESTROY = MethodDesc.of(InterceptorInvocation.class,
            "preDestroy", InterceptorInvocation.class, InjectableInterceptor.class, Object.class);

    static final MethodDesc INTERCEPTOR_INVOCATION_AROUND_CONSTRUCT = MethodDesc.of(InterceptorInvocation.class,
            "aroundConstruct", InterceptorInvocation.class, InjectableInterceptor.class, Object.class);

    static final MethodDesc INTERCEPTOR_INVOCATION_AROUND_INVOKE = MethodDesc.of(InterceptorInvocation.class,
            "aroundInvoke", InterceptorInvocation.class, InjectableInterceptor.class, Object.class);

    static final MethodDesc REFLECTIONS_FIND_CONSTRUCTOR = MethodDesc.of(Reflections.class,
            "findConstructor", Constructor.class, Class.class, Class[].class);

    static final MethodDesc REFLECTIONS_FIND_METHOD = MethodDesc.of(Reflections.class,
            "findMethod", Method.class, Class.class, String.class, Class[].class);

    static final MethodDesc REFLECTIONS_FIND_FIELD = MethodDesc.of(Reflections.class,
            "findField", Field.class, Class.class, String.class);

    static final MethodDesc REFLECTIONS_WRITE_FIELD = MethodDesc.of(Reflections.class,
            "writeField", void.class, Class.class, String.class, Object.class, Object.class);

    static final MethodDesc REFLECTIONS_READ_FIELD = MethodDesc.of(Reflections.class,
            "readField", Object.class, Class.class, String.class, Object.class);

    static final MethodDesc REFLECTIONS_INVOKE_METHOD = MethodDesc.of(Reflections.class,
            "invokeMethod", Object.class, Class.class, String.class, Class[].class, Object.class, Object[].class);

    static final MethodDesc REFLECTIONS_NEW_INSTANCE = MethodDesc.of(Reflections.class,
            "newInstance", Object.class, Class.class, Class[].class, Object[].class);

    static final MethodDesc CLIENT_PROXY_GET_CONTEXTUAL_INSTANCE = MethodDesc.of(ClientProxy.class,
            ClientProxyGenerator.GET_CONTEXTUAL_INSTANCE_METHOD_NAME, Object.class);

    static final MethodDesc CLIENT_PROXY_UNWRAP = MethodDesc.of(ClientProxy.class, "unwrap", Object.class, Object.class);

    static final MethodDesc INJECTABLE_BEAN_GET_SCOPE = MethodDesc.of(InjectableBean.class, "getScope", Class.class);

    static final MethodDesc INJECTABLE_BEAN_DESTROY = MethodDesc.of(InjectableBean.class,
            "destroy", void.class, Object.class, CreationalContext.class);

    static final MethodDesc INJECTABLE_REFERENCE_PROVIDERS_DESTROY = MethodDesc.of(InjectableReferenceProviders.class,
            "destroy", void.class, InjectableReferenceProvider.class, Object.class, CreationalContext.class);

    static final MethodDesc CREATIONAL_CTX_RELEASE = MethodDesc.of(CreationalContext.class, "release", void.class);

    static final MethodDesc EVENT_CONTEXT_GET_EVENT = MethodDesc.of(EventContext.class, "getEvent", Object.class);

    static final MethodDesc EVENT_CONTEXT_GET_METADATA = MethodDesc.of(EventContext.class, "getMetadata", EventMetadata.class);

    static final MethodDesc INVOCATION_CONTEXTS_PERFORM_AROUND_INVOKE = MethodDesc.of(InvocationContexts.class,
            "performAroundInvoke", Object.class, Object.class, Object[].class, InterceptedMethodMetadata.class);

    static final MethodDesc INVOCATION_CONTEXTS_PERFORM_TARGET_AROUND_INVOKE = MethodDesc.of(InvocationContexts.class,
            "performTargetAroundInvoke", Object.class, InvocationContext.class, List.class, BiFunction.class);

    static final MethodDesc INVOCATION_CONTEXTS_AROUND_CONSTRUCT = MethodDesc.of(InvocationContexts.class,
            "aroundConstruct", InvocationContext.class, Constructor.class, Object[].class, List.class, Function.class,
            Set.class);

    static final MethodDesc INVOCATION_CONTEXTS_POST_CONSTRUCT = MethodDesc.of(InvocationContexts.class,
            "postConstruct", InvocationContext.class, Object.class, List.class, Set.class, Runnable.class);

    static final MethodDesc INVOCATION_CONTEXTS_PRE_DESTROY = MethodDesc.of(InvocationContexts.class,
            "preDestroy", InvocationContext.class, Object.class, List.class, Set.class, Runnable.class);

    static final MethodDesc INVOCATION_CONTEXTS_PERFORM_SUPERCLASS = MethodDesc.of(InvocationContexts.class,
            "performSuperclassInterception", Object.class, InvocationContext.class, List.class, Object.class, Object[].class);

    static final MethodDesc INVOCATION_CONTEXT_PROCEED = MethodDesc.of(InvocationContext.class, "proceed", Object.class);

    static final MethodDesc INVOCATION_CONTEXT_GET_TARGET = MethodDesc.of(InvocationContext.class,
            "getTarget", Object.class);

    static final MethodDesc INVOCATION_CONTEXT_GET_PARAMETERS = MethodDesc.of(InvocationContext.class,
            "getParameters", Object[].class);

    static final MethodDesc CREATIONAL_CTX_ADD_DEP_TO_PARENT = MethodDesc.of(CreationalContextImpl.class,
            "addDependencyToParent", void.class, InjectableBean.class, Object.class, CreationalContext.class);

    static final MethodDesc COLLECTIONS_SINGLETON = MethodDesc.of(Collections.class,
            "singleton", Set.class, Object.class);

    static final MethodDesc COLLECTIONS_SINGLETON_LIST = MethodDesc.of(Collections.class,
            "singletonList", List.class, Object.class);

    static final MethodDesc SETS_OF = MethodDesc.of(Sets.class, "of", Set.class, Object[].class);

    static final MethodDesc ARC_REQUIRE_CONTAINER = MethodDesc.of(Arc.class, "requireContainer", ArcContainer.class);

    static final MethodDesc ARC_CONTAINER_BEAN = MethodDesc.of(ArcContainer.class,
            "bean", InjectableBean.class, String.class);

    static final MethodDesc ARC_CONTAINER_GET_ACTIVE_CONTEXT = MethodDesc.of(ArcContainer.class,
            "getActiveContext", InjectableContext.class, Class.class);

    static final MethodDesc ARC_CONTAINER_GET_CONTEXTS = MethodDesc.of(ArcContainer.class,
            "getContexts", List.class, Class.class);

    static final MethodDesc CONTEXT_GET = MethodDesc.of(Context.class,
            "get", Object.class, Contextual.class, CreationalContext.class);

    static final MethodDesc CONTEXT_GET_IF_PRESENT = MethodDesc.of(Context.class, "get", Object.class, Contextual.class);

    static final MethodDesc GET_IDENTIFIER = MethodDesc.of(InjectableBean.class, "getIdentifier", String.class);

    static final ConstructorDesc INTERCEPTED_METHOD_METADATA_CONSTRUCTOR = ConstructorDesc.of(InterceptedMethodMetadata.class,
            List.class, Method.class, Set.class, BiFunction.class);

    static final MethodDesc CREATIONAL_CTX_HAS_DEPENDENT_INSTANCES = MethodDesc.of(CreationalContextImpl.class,
            "hasDependentInstances", boolean.class);

    static final MethodDesc THREAD_GET_TCCL = MethodDesc.of(Thread.class, "getContextClassLoader", ClassLoader.class);

    static final MethodDesc CL_FOR_NAME = MethodDesc.of(Class.class,
            "forName", Class.class, String.class, boolean.class, ClassLoader.class);

    static final ConstructorDesc REMOVED_BEAN_IMPL = ConstructorDesc.of(RemovedBeanImpl.class,
            InjectableBean.Kind.class, String.class, Set.class, Set.class);

    static final MethodDesc CLIENT_PROXIES_GET_APP_SCOPED_DELEGATE = MethodDesc.of(ClientProxies.class,
            "getApplicationScopedDelegate", Object.class, InjectableContext.class, InjectableBean.class);

    static final MethodDesc CLIENT_PROXIES_GET_SINGLE_CONTEXT_DELEGATE = MethodDesc.of(ClientProxies.class,
            "getSingleContextDelegate", Object.class, InjectableContext.class, InjectableBean.class);

    static final MethodDesc CLIENT_PROXIES_GET_DELEGATE = MethodDesc.of(ClientProxies.class,
            "getDelegate", Object.class, InjectableBean.class);

    static final MethodDesc DECORATOR_DELEGATE_PROVIDER_GET = MethodDesc.of(DecoratorDelegateProvider.class,
            "getCurrent", Object.class, CreationalContext.class);

    static final MethodDesc DECORATOR_DELEGATE_PROVIDER_SET = MethodDesc.of(DecoratorDelegateProvider.class,
            "setCurrent", Object.class, CreationalContext.class, Object.class);

    static final MethodDesc COMPONENTS_PROVIDER_UNABLE_TO_LOAD_REMOVED_BEAN_TYPE = MethodDesc.of(ComponentsProvider.class,
            "unableToLoadRemovedBeanType", void.class, String.class, Throwable.class);

    static final MethodDesc BEANS_TO_STRING = MethodDesc.of(io.quarkus.arc.impl.Beans.class,
            "toString", String.class, InjectableBean.class);

    static final ConstructorDesc INJECTION_POINT_IMPL_CONSTRUCTOR = ConstructorDesc.of(InjectionPointImpl.class,
            Type.class, Type.class, Set.class, InjectableBean.class, Set.class, Member.class, int.class, boolean.class);

    static final MethodDesc INTERCEPT_FUNCTION_INTERCEPT = MethodDesc.of(InterceptorCreator.InterceptFunction.class,
            "intercept", Object.class, ArcInvocationContext.class);

    static final MethodDesc ACTIVE_RESULT_VALUE = MethodDesc.of(ActiveResult.class, "value", boolean.class);

    static final MethodDesc ACTIVE_RESULT_REASON = MethodDesc.of(ActiveResult.class, "inactiveReason", String.class);

    static final MethodDesc ACTIVE_RESULT_CAUSE = MethodDesc.of(ActiveResult.class, "inactiveCause", ActiveResult.class);
}
