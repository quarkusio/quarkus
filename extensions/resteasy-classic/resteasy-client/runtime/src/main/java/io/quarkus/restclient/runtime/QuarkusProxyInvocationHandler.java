package io.quarkus.restclient.runtime;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ResponseProcessingException;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;

import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.microprofile.client.ExceptionMapping;
import org.jboss.resteasy.microprofile.client.RestClientProxy;
import org.jboss.resteasy.microprofile.client.header.ClientHeaderFillingException;
import org.jboss.resteasy.microprofile.client.header.ClientHeaderProviders;

/**
 * Quarkus version of {@link org.jboss.resteasy.microprofile.client.ProxyInvocationHandler} retaining the ability to
 * create a custom interceptor chain and invoke it manually.
 * <p/>
 * This is needed due to changes in https://github.com/resteasy/resteasy-microprofile/pull/182
 * <p/>
 * In theory, it could be improved by pre-generating proxies for {@code @RegisterRestClient} interfaces and registering
 * them as standard beans with all their interceptor bindings.
 */
public class QuarkusProxyInvocationHandler implements InvocationHandler {

    private static final Logger LOGGER = Logger.getLogger(QuarkusProxyInvocationHandler.class);
    public static final Type[] NO_TYPES = {};

    private final Object target;

    private final Set<Object> providerInstances;

    private final Map<Method, List<QuarkusInvocationContextImpl.InterceptorInvocation>> interceptorChains;

    private final Map<Method, Set<Annotation>> interceptorBindingsMap;

    private final ResteasyClient client;

    private final CreationalContext<?> creationalContext;

    private final AtomicBoolean closed;

    public QuarkusProxyInvocationHandler(final Class<?> restClientInterface,
            final Object target,
            final Set<Object> providerInstances,
            final ResteasyClient client, final BeanManager beanManager) {
        this.target = target;
        this.providerInstances = providerInstances;
        this.client = client;
        this.closed = new AtomicBoolean();
        if (beanManager != null) {
            this.creationalContext = beanManager.createCreationalContext(null);
            this.interceptorBindingsMap = new HashMap<>();
            this.interceptorChains = initInterceptorChains(beanManager, creationalContext, restClientInterface,
                    interceptorBindingsMap);
        } else {
            this.creationalContext = null;
            this.interceptorChains = Collections.emptyMap();
            this.interceptorBindingsMap = Collections.emptyMap();
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (RestClientProxy.class.equals(method.getDeclaringClass())) {
            return invokeRestClientProxyMethod(method);
        }
        // Autocloseable/Closeable
        if (method.getName().equals("close") && (args == null || args.length == 0)) {
            close();
            return null;
        }
        // Check if this proxy is closed or the client itself is closed. The client may be closed if this proxy was a
        // sub-resource and the resource client itself was closed.
        if (closed.get() || client.isClosed()) {
            closed.set(true);
            throw new IllegalStateException("RestClientProxy is closed");
        }

        boolean replacementNeeded = false;
        Object[] argsReplacement = args != null ? new Object[args.length] : null;
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        if (args != null) {
            for (Object p : providerInstances) {
                if (p instanceof ParamConverterProvider) {

                    int index = 0;
                    for (Object arg : args) {
                        // ParamConverter's are not allowed to be passed null values. If we have a null value do not process
                        // it through the provider.
                        if (arg == null) {
                            continue;
                        }

                        if (parameterAnnotations[index].length > 0) { // does a parameter converter apply?
                            ParamConverter<?> converter = ((ParamConverterProvider) p).getConverter(arg.getClass(), null,
                                    parameterAnnotations[index]);
                            if (converter != null) {
                                Type[] genericTypes = getGenericTypes(converter.getClass());
                                if (genericTypes.length == 1) {

                                    // minimum supported types
                                    switch (genericTypes[0].getTypeName()) {
                                        case "java.lang.String":
                                            @SuppressWarnings("unchecked")
                                            ParamConverter<String> stringConverter = (ParamConverter<String>) converter;
                                            argsReplacement[index] = stringConverter.toString((String) arg);
                                            replacementNeeded = true;
                                            break;
                                        case "java.lang.Integer":
                                            @SuppressWarnings("unchecked")
                                            ParamConverter<Integer> intConverter = (ParamConverter<Integer>) converter;
                                            argsReplacement[index] = intConverter.toString((Integer) arg);
                                            replacementNeeded = true;
                                            break;
                                        case "java.lang.Boolean":
                                            @SuppressWarnings("unchecked")
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
        }

        if (replacementNeeded) {
            args = argsReplacement;
        }

        List<QuarkusInvocationContextImpl.InterceptorInvocation> chain = interceptorChains.get(method);
        if (chain != null) {
            // Invoke business method interceptors
            return new QuarkusInvocationContextImpl(target, method, args, chain, interceptorBindingsMap.get(method)).proceed();
        } else {
            try {
                final Object result = method.invoke(target, args);
                final Class<?> returnType = method.getReturnType();
                // Check if this is a sub-resource. A sub-resource must be an interface.
                if (returnType.isInterface()) {
                    final Annotation[] annotations = method.getDeclaredAnnotations();
                    boolean hasPath = false;
                    boolean hasHttpMethod = false;
                    // Check the annotations. If the method has one of the @HttpMethod annotations, we will just use the
                    // current method. If it only has a @Path, then we need to create a proxy for the return type.
                    for (Annotation annotation : annotations) {
                        final Class<?> type = annotation.annotationType();
                        if (type.equals(Path.class)) {
                            hasPath = true;
                        } else if (type.getDeclaredAnnotation(HttpMethod.class) != null) {
                            hasHttpMethod = true;
                        }
                    }
                    if (!hasHttpMethod && hasPath) {
                        // Create a proxy of the return type re-using the providers and client, but do not add the required
                        // interfaces for the sub-resource.
                        return createProxy(returnType, result, false, providerInstances, client);
                    }
                }
                return result;
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof CompletionException) {
                    cause = cause.getCause();
                }
                if (cause instanceof ExceptionMapping.HandlerException) {
                    ((ExceptionMapping.HandlerException) cause).mapException(method);
                    // no applicable exception mapper found or applicable mapper returned null
                    return null;
                }
                if (cause instanceof ResponseProcessingException) {
                    ResponseProcessingException rpe = (ResponseProcessingException) cause;
                    cause = rpe.getCause();
                    if (cause instanceof RuntimeException) {
                        throw cause;
                    }
                } else {
                    if (cause instanceof ProcessingException &&
                            cause.getCause() instanceof ClientHeaderFillingException) {
                        throw cause.getCause().getCause();
                    }
                    if (cause instanceof RuntimeException) {
                        throw cause;
                    }
                }
                throw e;
            }
        }
    }

    /**
     * Creates a proxy for the interface.
     * <p>
     * If {@code addExtendedInterfaces} is set to {@code true}, the proxy will implement the interfaces
     * {@link RestClientProxy} and {@link Closeable}.
     * </p>
     *
     * @param resourceInterface the resource interface to create the proxy for
     * @param target the target object for the proxy
     * @param addExtendedInterfaces {@code true} if the proxy should also implement {@link RestClientProxy} and
     *        {@link Closeable}
     * @param providers the providers for the client
     * @param client the client to use
     * @return the new proxy
     */
    static Object createProxy(final Class<?> resourceInterface, final Object target, final boolean addExtendedInterfaces,
            final Set<Object> providers, final ResteasyClient client) {
        final Class<?>[] interfaces;
        if (addExtendedInterfaces) {
            interfaces = new Class<?>[3];
            interfaces[1] = RestClientProxy.class;
            interfaces[2] = Closeable.class;
        } else {
            interfaces = new Class[1];
        }
        interfaces[0] = resourceInterface;
        final BeanManager beanManager = getBeanManager(resourceInterface);
        final Object proxy = Proxy.newProxyInstance(resourceInterface.getClassLoader(), interfaces,
                new QuarkusProxyInvocationHandler(resourceInterface, target, Set.copyOf(providers), client,
                        beanManager));
        ClientHeaderProviders.registerForClass(resourceInterface, proxy, beanManager);
        return proxy;
    }

    private Object invokeRestClientProxyMethod(final Method method) {
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

    private static Map<Method, List<QuarkusInvocationContextImpl.InterceptorInvocation>> initInterceptorChains(
            BeanManager beanManager, CreationalContext<?> creationalContext, Class<?> restClientInterface,
            Map<Method, Set<Annotation>> interceptorBindingsMap) {

        Map<Method, List<QuarkusInvocationContextImpl.InterceptorInvocation>> chains = new HashMap<>();
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
                    List<QuarkusInvocationContextImpl.InterceptorInvocation> chain = new ArrayList<>();
                    for (Interceptor<?> interceptor : interceptors) {
                        chain.add(new QuarkusInvocationContextImpl.InterceptorInvocation(interceptor,
                                interceptorInstances.computeIfAbsent(interceptor,
                                        i -> beanManager.getReference(i, i.getBeanClass(), creationalContext))));
                    }
                    interceptorBindingsMap.put(method, Set.of(interceptorBindings));
                    chains.put(method, chain);
                }
            }
        }
        return chains.isEmpty() ? Collections.emptyMap() : chains;
    }

    private static Annotation[] merge(List<Annotation> methodLevelBindings, List<Annotation> classLevelBindings) {
        Set<Class<? extends Annotation>> types = methodLevelBindings.stream()
                .map(a -> a.annotationType())
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
