/*
 *  Copyright (c) 2019 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.quarkus.smallrye.restclient.runtime;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import org.eclipse.microprofile.rest.client.ext.AsyncInvocationInterceptor;
import org.eclipse.microprofile.rest.client.ext.AsyncInvocationInterceptorFactory;
import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;

import io.smallrye.restclient.InvocationContextImpl;
import io.smallrye.restclient.RestClientProxy;
import io.smallrye.restclient.async.AsyncInvocationInterceptorHandler;
import io.smallrye.restclient.header.ClientHeaderFillingException;

/**
 * Created by hbraun on 22.01.18.
 */
public class ProxyInvocationHandler implements InvocationHandler {

    private static final Logger LOGGER = Logger.getLogger(io.smallrye.restclient.ProxyInvocationHandler.class);
    public static final Type[] NO_TYPES = {};

    private final Object target;

    private final Set<Object> providerInstances;

    private final Map<Method, List<InvocationContextImpl.InterceptorInvocation>> interceptorChains;

    private final ResteasyClient client;

    private final CreationalContext<?> creationalContext;

    private final AtomicBoolean closed;

    private final List<AsyncInvocationInterceptorFactory> asyncInterceptorFactories;

    public ProxyInvocationHandler(Class<?> restClientInterface,
            Object target,
            Set<Object> providerInstances,
            ResteasyClient client,
            List<AsyncInvocationInterceptorFactory> asyncInterceptorFactories) {
        this.target = target;
        this.providerInstances = providerInstances;
        this.client = client;
        this.asyncInterceptorFactories = asyncInterceptorFactories;
        this.closed = new AtomicBoolean();
        BeanManager beanManager = getBeanManager(restClientInterface);
        if (beanManager != null) {
            this.creationalContext = beanManager.createCreationalContext(null);
            this.interceptorChains = initInterceptorChains(beanManager, creationalContext, restClientInterface);
        } else {
            this.creationalContext = null;
            this.interceptorChains = Collections.emptyMap();
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (RestClientProxy.class.equals(method.getDeclaringClass())) {
            return invokeRestClientProxyMethod(proxy, method, args);
        }
        if (closed.get()) {
            throw new IllegalStateException("RestClientProxy is closed");
        }

        prepareAsyncInterceptors();

        boolean replacementNeeded = false;
        Object[] argsReplacement = args != null ? new Object[args.length] : null;
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        for (Object p : providerInstances) {
            if (p instanceof ParamConverterProvider) {

                int index = 0;
                for (Object arg : args) {

                    if (parameterAnnotations[index].length > 0) { // does a parameter converter apply?

                        ParamConverter<?> converter = ((ParamConverterProvider) p).getConverter(arg.getClass(), null,
                                parameterAnnotations[index]);
                        if (converter != null) {
                            Type[] genericTypes = getGenericTypes(converter.getClass());
                            if (genericTypes.length == 1) {

                                // minimum supported types
                                switch (genericTypes[0].getTypeName()) {
                                    case "java.lang.String":
                                        ParamConverter<String> stringConverter = (ParamConverter<String>) converter;
                                        argsReplacement[index] = stringConverter.toString((String) arg);
                                        replacementNeeded = true;
                                        break;
                                    case "java.lang.Integer":
                                        ParamConverter<Integer> intConverter = (ParamConverter<Integer>) converter;
                                        argsReplacement[index] = intConverter.toString((Integer) arg);
                                        replacementNeeded = true;
                                        break;
                                    case "java.lang.Boolean":
                                        ParamConverter<Boolean> boolConverter = (ParamConverter<Boolean>) converter;
                                        argsReplacement[index] = boolConverter.toString((Boolean) arg);
                                        replacementNeeded = true;
                                        break;
                                    default:
                                        continue;
                                }
                            }
                        }
                    } else {
                        argsReplacement[index] = arg;
                    }
                    index++;
                }
            }
        }

        if (replacementNeeded) {
            args = argsReplacement;
        }

        List<InvocationContextImpl.InterceptorInvocation> chain = interceptorChains.get(method);
        if (chain != null) {
            // Invoke business method interceptors
            return new InvocationContextImpl(target, method, args, chain).proceed();
        } else {
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof ResponseProcessingException) {
                    ResponseProcessingException rpe = (ResponseProcessingException) e.getCause();
                    Throwable cause = rpe.getCause();
                    if (cause instanceof RuntimeException) {
                        throw cause;
                    }
                } else {
                    Throwable targetException = e.getTargetException();
                    if (targetException instanceof ProcessingException &&
                            targetException.getCause() instanceof ClientHeaderFillingException) {
                        throw targetException.getCause().getCause();
                    }
                    if (targetException instanceof RuntimeException) {
                        throw targetException;
                    }
                }
                throw e;
            }
        }
    }

    private void prepareAsyncInterceptors() {
        List<AsyncInvocationInterceptor> interceptors = asyncInterceptorFactories.stream()
                .map(AsyncInvocationInterceptorFactory::newInterceptor)
                .collect(Collectors.toList());
        interceptors.forEach(AsyncInvocationInterceptor::prepareContext);
        AsyncInvocationInterceptorHandler.register(interceptors);
    }

    private Object invokeRestClientProxyMethod(Object proxy, Method method, Object[] args) throws Throwable {
        switch (method.getName()) {
            case "getClient":
                return client;
            case "close":
                close();
                return null;
            default:
                throw new IllegalStateException("Unsupported RestClientProxy method: " + method);
        }
    }

    private void close() {
        if (closed.compareAndSet(false, true)) {
            if (creationalContext != null) {
                creationalContext.release();
            }
            client.close();
        }
    }

    private Type[] getGenericTypes(Class<?> aClass) {
        Type[] genericInterfaces = aClass.getGenericInterfaces();
        Type[] genericTypes = NO_TYPES;
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType) {
                genericTypes = ((ParameterizedType) genericInterface).getActualTypeArguments();
            }
        }
        return genericTypes;
    }

    private static List<Annotation> getBindings(Annotation[] annotations, BeanManager beanManager) {
        if (annotations.length == 0) {
            return Collections.emptyList();
        }
        List<Annotation> bindings = new ArrayList<>();
        for (Annotation annotation : annotations) {
            if (beanManager.isInterceptorBinding(annotation.annotationType())) {
                bindings.add(annotation);
            }
        }
        return bindings;
    }

    private static BeanManager getBeanManager(Class<?> restClientInterface) {
        try {
            CDI<Object> current = CDI.current();
            return current != null ? current.getBeanManager() : null;
        } catch (IllegalStateException e) {
            LOGGER.warnf("CDI container is not available - interceptor bindings declared on %s will be ignored",
                    restClientInterface.getSimpleName());
            return null;
        }
    }

    private static Map<Method, List<InvocationContextImpl.InterceptorInvocation>> initInterceptorChains(BeanManager beanManager,
            CreationalContext<?> creationalContext, Class<?> restClientInterface) {

        Map<Method, List<InvocationContextImpl.InterceptorInvocation>> chains = new HashMap<>();
        // Interceptor as a key in a map is not entirely correct (custom interceptors) but should work in most cases
        Map<Interceptor<?>, Object> interceptorInstances = new HashMap<>();

        List<Annotation> classLevelBindings = getBindings(restClientInterface.getAnnotations(), beanManager);

        for (Method method : restClientInterface.getMethods()) {
            if (method.isDefault() || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            List<Annotation> methodLevelBindings = getBindings(method.getAnnotations(), beanManager);

            if (!classLevelBindings.isEmpty() || !methodLevelBindings.isEmpty()) {

                Annotation[] interceptorBindings = merge(methodLevelBindings, classLevelBindings);

                List<Interceptor<?>> interceptors = beanManager.resolveInterceptors(InterceptionType.AROUND_INVOKE,
                        interceptorBindings);
                if (!interceptors.isEmpty()) {
                    List<InvocationContextImpl.InterceptorInvocation> chain = new ArrayList<>();
                    for (Interceptor<?> interceptor : interceptors) {
                        chain.add(new InvocationContextImpl.InterceptorInvocation(interceptor,
                                interceptorInstances.computeIfAbsent(interceptor,
                                        i -> beanManager.getReference(i, i.getBeanClass(), creationalContext))));
                    }
                    chains.put(method, chain);
                }
            }
        }
        return chains.isEmpty() ? Collections.emptyMap() : chains;
    }

    private static Annotation[] merge(List<Annotation> methodLevelBindings, List<Annotation> classLevelBindings) {
        Set<Class<? extends Annotation>> types = methodLevelBindings.stream().map(a -> a.annotationType())
                .collect(Collectors.toSet());
        List<Annotation> merged = new ArrayList<>(methodLevelBindings);
        for (Annotation annotation : classLevelBindings) {
            if (!types.contains(annotation.annotationType())) {
                merged.add(annotation);
            }
        }
        return merged.toArray(new Annotation[] {});
    }

}
