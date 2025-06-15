package io.quarkus.hibernate.validator.runtime.jaxrs;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.hibernate.validator.runtime.interceptor.AbstractMethodValidationInterceptor;

@JaxrsEndPointValidated
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_AFTER + 800)
public class JaxrsEndPointValidationInterceptor extends AbstractMethodValidationInterceptor {

    private static final List<MediaType> JSON_MEDIA_TYPE_LIST = Collections
            .singletonList(MediaType.APPLICATION_JSON_TYPE);

    private final ConcurrentHashMap<Method, List<MediaType>> producedMediaTypesCache = new ConcurrentHashMap<>();

    @Inject
    ResteasyConfigSupport resteasyConfigSupport;

    @AroundInvoke
    @Override
    public Object validateMethodInvocation(InvocationContext ctx) throws Exception {
        try {
            return super.validateMethodInvocation(ctx);
        } catch (ConstraintViolationException e) {
            List<MediaType> producedMediaTypes = getProduces(ctx.getMethod());

            if (producedMediaTypes.isEmpty() && resteasyConfigSupport.isJsonDefault()) {
                producedMediaTypes = JSON_MEDIA_TYPE_LIST;
            }

            throw new ResteasyViolationExceptionImpl(e.getConstraintViolations(), producedMediaTypes);
        }
    }

    @AroundConstruct
    @Override
    public void validateConstructorInvocation(InvocationContext ctx) throws Exception {
        super.validateConstructorInvocation(ctx);
    }

    private List<MediaType> getProduces(Method originalMethod) {
        List<MediaType> cachedMediaTypes = producedMediaTypesCache.get(originalMethod);

        if (cachedMediaTypes != null) {
            return cachedMediaTypes;
        }

        return producedMediaTypesCache.computeIfAbsent(originalMethod, new Function<Method, List<MediaType>>() {

            @Override
            public List<MediaType> apply(Method method) {
                return doGetProduces(originalMethod);
            }
        });
    }

    /**
     * Ideally, we would be able to get the information from RESTEasy so that we can follow strictly the inheritance
     * rules. But, given RESTEasy Reactive is our new default REST layer, I think we can live with this limitation.
     * <p>
     * Superclass method annotations have precedence, then interface methods and finally class annotations.
     */
    private List<MediaType> doGetProduces(Method originalMethod) {
        Class<?> currentClass = originalMethod.getDeclaringClass();
        List<Class<?>> interfaces = new ArrayList<>();

        do {
            List<MediaType> classMethodProducedMediaTypes = getProducesFromMethod(currentClass, originalMethod);
            if (!classMethodProducedMediaTypes.isEmpty()) {
                return classMethodProducedMediaTypes;
            }

            for (Class<?> interfaze : currentClass.getInterfaces()) {
                interfaces.add(interfaze);
            }

            currentClass = currentClass.getSuperclass();
        } while (!Object.class.equals(currentClass));

        for (Class<?> interfaze : interfaces) {
            List<MediaType> interfaceMethodProducedMediaTypes = getProducesFromMethod(interfaze, originalMethod);
            if (!interfaceMethodProducedMediaTypes.isEmpty()) {
                return interfaceMethodProducedMediaTypes;
            }
        }

        List<MediaType> classProducedMediaTypes = getProduces(
                originalMethod.getDeclaringClass().getAnnotation(Produces.class));
        if (!classProducedMediaTypes.isEmpty()) {
            return classProducedMediaTypes;
        }

        for (Class<?> interfaze : interfaces) {
            List<MediaType> interfaceProducedMediaTypes = getProduces(interfaze.getAnnotation(Produces.class));
            if (!interfaceProducedMediaTypes.isEmpty()) {
                return interfaceProducedMediaTypes;
            }
        }

        return Collections.emptyList();
    }

    private List<MediaType> getProducesFromMethod(Class<?> currentClass, Method originalMethod) {
        if (currentClass.equals(originalMethod.getDeclaringClass())) {
            return getProduces(originalMethod.getAnnotation(Produces.class));
        }

        try {
            return getProduces(currentClass.getMethod(originalMethod.getName(), originalMethod.getParameterTypes())
                    .getAnnotation(Produces.class));
        } catch (NoSuchMethodException | SecurityException e) {
            // we don't have a visible method around, let's ignore this class
            return Collections.emptyList();
        }
    }

    public static List<MediaType> getProduces(Produces produces) {
        if (produces == null) {
            return Collections.emptyList();
        }

        MediaType[] mediaTypes = new MediaType[produces.value().length];
        for (int i = 0; i < produces.value().length; i++) {
            mediaTypes[i] = MediaType.valueOf(produces.value()[i]);
        }

        return mediaTypes.length != 0 ? List.of(mediaTypes) : Collections.emptyList();
    }
}
