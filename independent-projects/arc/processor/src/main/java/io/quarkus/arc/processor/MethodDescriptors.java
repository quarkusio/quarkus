/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.arc.processor;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.CreationalContextImpl;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InjectableInterceptor;
import io.quarkus.arc.InjectableReferenceProvider;
import io.quarkus.arc.InvocationContextImpl;
import io.quarkus.arc.InvocationContextImpl.InterceptorInvocation;
import io.quarkus.arc.LazyValue;
import io.quarkus.arc.Reflections;
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
final class MethodDescriptors {

    static final MethodDescriptor CREATIONAL_CTX_CHILD = MethodDescriptor.ofMethod(CreationalContextImpl.class, "child",
            CreationalContextImpl.class,
            CreationalContext.class);

    static final MethodDescriptor MAP_GET = MethodDescriptor.ofMethod(Map.class, "get", Object.class, Object.class);

    static final MethodDescriptor MAP_PUT = MethodDescriptor.ofMethod(Map.class, "put", Object.class, Object.class,
            Object.class);

    static final MethodDescriptor INJECTABLE_REF_PROVIDER_GET = MethodDescriptor.ofMethod(InjectableReferenceProvider.class,
            "get", Object.class,
            CreationalContext.class);

    static final MethodDescriptor SET_ADD = MethodDescriptor.ofMethod(Set.class, "add", boolean.class, Object.class);

    static final MethodDescriptor LIST_ADD = MethodDescriptor.ofMethod(List.class, "add", boolean.class, Object.class);

    static final MethodDescriptor OBJECT_EQUALS = MethodDescriptor.ofMethod(Object.class, "equals", boolean.class,
            Object.class);

    static final MethodDescriptor OBJECT_TO_STRING = MethodDescriptor.ofMethod(Object.class, "toString", String.class);

    static final MethodDescriptor OBJECT_CONSTRUCTOR = MethodDescriptor.ofConstructor(Object.class);

    static final MethodDescriptor INTERCEPTOR_INVOCATION_POST_CONSTRUCT = MethodDescriptor.ofMethod(InterceptorInvocation.class,
            "postConstruct",
            InterceptorInvocation.class, InjectableInterceptor.class, Object.class);

    static final MethodDescriptor INTERCEPTOR_INVOCATION_AROUND_CONSTRUCT = MethodDescriptor.ofMethod(
            InterceptorInvocation.class, "aroundConstruct",
            InterceptorInvocation.class, InjectableInterceptor.class, Object.class);

    static final MethodDescriptor INTERCEPTOR_INVOCATION_AROUND_INVOKE = MethodDescriptor.ofMethod(InterceptorInvocation.class,
            "aroundInvoke",
            InterceptorInvocation.class, InjectableInterceptor.class, Object.class);

    static final MethodDescriptor REFLECTIONS_FIND_CONSTRUCTOR = MethodDescriptor.ofMethod(Reflections.class, "findConstructor",
            Constructor.class, Class.class,
            Class[].class);

    static final MethodDescriptor REFLECTIONS_FIND_METHOD = MethodDescriptor.ofMethod(Reflections.class, "findMethod",
            Method.class, Class.class, String.class,
            Class[].class);

    static final MethodDescriptor REFLECTIONS_FIND_FIELD = MethodDescriptor.ofMethod(Reflections.class, "findField",
            Field.class, Class.class, String.class);

    static final MethodDescriptor REFLECTIONS_WRITE_FIELD = MethodDescriptor.ofMethod(Reflections.class, "writeField",
            void.class, Class.class, String.class,
            Object.class, Object.class);

    static final MethodDescriptor REFLECTIONS_READ_FIELD = MethodDescriptor.ofMethod(Reflections.class, "readField",
            Object.class, Class.class, String.class,
            Object.class);

    static final MethodDescriptor REFLECTIONS_INVOKE_METHOD = MethodDescriptor.ofMethod(Reflections.class, "invokeMethod",
            Object.class, Class.class,
            String.class, Class[].class, Object.class, Object[].class);

    static final MethodDescriptor REFLECTIONS_NEW_INSTANCE = MethodDescriptor.ofMethod(Reflections.class, "newInstance",
            Object.class, Class.class,
            Class[].class, Object[].class);

    static final MethodDescriptor CLIENT_PROXY_GET_CONTEXTUAL_INSTANCE = MethodDescriptor.ofMethod(ClientProxy.class,
            "getContextualInstance", Object.class);

    static final MethodDescriptor INJECTABLE_BEAN_DESTROY = MethodDescriptor.ofMethod(InjectableBean.class, "destroy",
            void.class, Object.class,
            CreationalContext.class);

    static final MethodDescriptor CREATIONAL_CTX_RELEASE = MethodDescriptor.ofMethod(CreationalContext.class, "release",
            void.class);

    static final MethodDescriptor EVENT_CONTEXT_GET_EVENT = MethodDescriptor.ofMethod(EventContext.class, "getEvent",
            Object.class);

    static final MethodDescriptor EVENT_CONTEXT_GET_METADATA = MethodDescriptor.ofMethod(EventContext.class, "getMetadata",
            EventMetadata.class);

    static final MethodDescriptor INVOCATION_CONTEXT_AROUND_INVOKE = MethodDescriptor.ofMethod(InvocationContextImpl.class,
            "aroundInvoke",
            InvocationContextImpl.class, Object.class, Method.class, Object[].class, List.class, Function.class, Set.class);

    static final MethodDescriptor INVOCATION_CONTEXT_AROUND_CONSTRUCT = MethodDescriptor.ofMethod(InvocationContextImpl.class,
            "aroundConstruct",
            InvocationContextImpl.class, Constructor.class, List.class, Supplier.class, Set.class);

    static final MethodDescriptor INVOCATION_CONTEXT_POST_CONSTRUCT = MethodDescriptor.ofMethod(InvocationContextImpl.class,
            "postConstruct",
            InvocationContextImpl.class, Object.class, List.class, Set.class);

    static final MethodDescriptor INVOCATION_CONTEXT_PRE_DESTROY = MethodDescriptor.ofMethod(InvocationContextImpl.class,
            "preDestroy",
            InvocationContextImpl.class, Object.class, List.class, Set.class);

    static final MethodDescriptor INVOCATION_CONTEXT_PROCEED = MethodDescriptor.ofMethod(InvocationContext.class, "proceed",
            Object.class);

    static final MethodDescriptor CREATIONAL_CTX_ADD_DEP_TO_PARENT = MethodDescriptor.ofMethod(CreationalContextImpl.class,
            "addDependencyToParent", void.class,
            InjectableBean.class, Object.class, CreationalContext.class);

    static final MethodDescriptor COLLECTIONS_UNMODIFIABLE_SET = MethodDescriptor.ofMethod(Collections.class, "unmodifiableSet",
            Set.class, Set.class);

    static final MethodDescriptor ARC_CONTAINER = MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class);

    static final MethodDescriptor ARC_CONTAINER_GET_ACTIVE_CONTEXT = MethodDescriptor.ofMethod(ArcContainer.class,
            "getActiveContext", InjectableContext.class, Class.class);

    static final MethodDescriptor CONTEXT_GET = MethodDescriptor.ofMethod(Context.class, "get", Object.class, Contextual.class,
            CreationalContext.class);

    static final MethodDescriptor CONTEXT_GET_IF_PRESENT = MethodDescriptor.ofMethod(Context.class, "get", Object.class,
            Contextual.class);

    static final MethodDescriptor LAZY_VALUE_GET = MethodDescriptor.ofMethod(LazyValue.class, "get", Object.class);

    private MethodDescriptors() {
    }

}
