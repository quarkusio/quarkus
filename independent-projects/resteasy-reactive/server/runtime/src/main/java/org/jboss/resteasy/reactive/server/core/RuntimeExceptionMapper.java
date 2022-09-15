package org.jboss.resteasy.reactive.server.core;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ResteasyReactiveClientProblem;
import org.jboss.resteasy.reactive.common.model.ResourceExceptionMapper;
import org.jboss.resteasy.reactive.server.mapping.RuntimeResource;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveAsyncExceptionMapper;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveExceptionMapper;

public class RuntimeExceptionMapper {

    private static final Logger log = Logger.getLogger(RuntimeExceptionMapper.class);

    private static Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> mappers;

    /**
     * Exceptions that indicate an blocking operation was performed on an IO thread.
     * <p>
     * We have a special log message for this.
     */
    private final List<Predicate<Throwable>> blockingProblemPredicates;
    private final List<Predicate<Throwable>> nonBlockingProblemPredicate;
    private final Set<Class<? extends Throwable>> unwrappedExceptions;

    public RuntimeExceptionMapper(ExceptionMapping mapping, ClassLoader classLoader) {
        try {
            mappers = new HashMap<>();
            for (var i : mapping.mappers.entrySet()) {
                mappers.put((Class<? extends Throwable>) Class.forName(i.getKey(), false, classLoader), i.getValue());
            }
            blockingProblemPredicates = new ArrayList<>(mapping.blockingProblemPredicates);
            nonBlockingProblemPredicate = new ArrayList<>(mapping.nonBlockingProblemPredicate);
            unwrappedExceptions = new HashSet<>();
            for (var i : mapping.unwrappedExceptions) {
                unwrappedExceptions.add((Class<? extends Throwable>) Class.forName(i, false, classLoader));
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load exception mapper", e);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void mapException(Throwable throwable, ResteasyReactiveRequestContext context) {
        Class<?> klass = throwable.getClass();
        //we don't thread WebApplicationException's thrown from the client as true 'WebApplicationException'
        //we consider it a security risk to transparently pass on the result to the calling server
        boolean isWebApplicationException = throwable instanceof WebApplicationException
                && !(throwable instanceof ResteasyReactiveClientProblem);
        Response response = null;
        if (isWebApplicationException) {
            response = ((WebApplicationException) throwable).getResponse();
        }
        if (response != null && response.hasEntity()) {
            context.setResult(response);
            return;
        }

        // we match superclasses only if not a WebApplicationException according to spec 3.3.4 Exceptions
        Map.Entry<Throwable, jakarta.ws.rs.ext.ExceptionMapper<? extends Throwable>> entry = getExceptionMapper(
                (Class<Throwable>) klass, context,
                throwable);
        if (entry != null) {
            jakarta.ws.rs.ext.ExceptionMapper exceptionMapper = entry.getValue();
            Throwable mappedException = entry.getKey();
            context.requireCDIRequestScope();
            if (exceptionMapper instanceof ResteasyReactiveAsyncExceptionMapper) {
                ((ResteasyReactiveAsyncExceptionMapper) exceptionMapper).asyncResponse(mappedException,
                        new AsyncExceptionMapperContextImpl(context));
                logBlockingErrorIfRequired(mappedException, context);
                logNonBlockingErrorIfRequired(mappedException, context);
                return;
            } else if (exceptionMapper instanceof ResteasyReactiveExceptionMapper) {
                response = ((ResteasyReactiveExceptionMapper) exceptionMapper).toResponse(mappedException, context);
            } else {
                response = exceptionMapper.toResponse(mappedException);
            }
            context.setResult(response);
            logBlockingErrorIfRequired(mappedException, context);
            logNonBlockingErrorIfRequired(mappedException, context);
            return;
        }
        if (isWebApplicationException) {
            context.setResult(response);
            logBlockingErrorIfRequired(throwable, context);
            return;
        }
        if (throwable instanceof IOException) {
            log.debugf(throwable,
                    "IOError processing HTTP request to %s failed, the client likely terminated the connection.",
                    context.serverRequest().getRequestAbsoluteUri());
        } else if (context.handlesUnmappedException()) {
            log.error("Request failed ", throwable);
        }
        logBlockingErrorIfRequired(throwable, context);
        logNonBlockingErrorIfRequired(throwable, context);
        context.handleUnmappedException(throwable);
    }

    private void logBlockingErrorIfRequired(Throwable throwable, ResteasyReactiveRequestContext context) {
        if (isBlockingProblem(throwable)) {
            RuntimeResource runtimeResource = context.getTarget();
            if (runtimeResource == null) {
                log.error("A blocking operation occurred on the IO thread. This likely means you need to use the @"
                        + Blocking.class.getName() + " annotation on the Resource method, class or "
                        + Application.class.getName() + " class.");
            } else {
                log.error("A blocking operation occurred on the IO thread. This likely means you need to annotate "
                        + runtimeResource.getResourceClass().getName() + "#" + runtimeResource.getJavaMethodName() + "("
                        + Arrays.stream(runtimeResource.getParameterTypes()).map(Objects::toString)
                                .collect(Collectors.joining(", "))
                        + ") with @"
                        + Blocking.class.getName()
                        + ". Alternatively you can annotate the class " + runtimeResource.getResourceClass().getName()
                        + " to make every method on the class blocking, or annotate your sub class of the "
                        + Application.class.getName() + " class to make the whole application blocking");
            }
        }
    }

    private void logNonBlockingErrorIfRequired(Throwable throwable, ResteasyReactiveRequestContext context) {
        if (isNonBlockingProblem(throwable)) {
            RuntimeResource runtimeResource = context.getTarget();
            if (runtimeResource == null) {
                log.error(
                        "An operation that needed be run on a Vert.x EventLoop thread was run on a worker pool thread. This likely means you use the @"
                                + NonBlocking.class.getName() + " annotation on the Resource method, class or "
                                + Application.class.getName() + " class.");
            } else {
                log.error(
                        "An operation that needed be run on a Vert.x EventLoop thread was run on a worker pool thread. This likely means you need to annotate "
                                + runtimeResource.getResourceClass().getName() + "#" + runtimeResource.getJavaMethodName()
                                + "("
                                + Arrays.stream(runtimeResource.getParameterTypes()).map(Objects::toString)
                                        .collect(Collectors.joining(", "))
                                + ") with @"
                                + NonBlocking.class.getName()
                                + ". Alternatively you can annotate the class " + runtimeResource.getResourceClass().getName()
                                + " to make every method on the class run on a Vert.x EventLoop thread, or annotate your sub class of the "
                                + Application.class.getName() + " class to affect the entire application");
            }
        }
    }

    private boolean isBlockingProblem(Throwable throwable) {
        return isKnownProblem(throwable, blockingProblemPredicates);
    }

    private boolean isNonBlockingProblem(Throwable throwable) {
        return isKnownProblem(throwable, nonBlockingProblemPredicate);
    }

    private boolean isKnownProblem(Throwable throwable, List<Predicate<Throwable>> predicates) {
        Throwable e = throwable;
        while (e != null) {
            for (Predicate<Throwable> predicate : predicates) {
                if (predicate.test(e)) {
                    return true;
                }
            }
            e = e.getCause();
        }
        return false;
    }

    public <T extends Throwable> void addExceptionMapper(Class<T> exceptionClass, ResourceExceptionMapper<T> mapper) {
        ResourceExceptionMapper<? extends Throwable> existing = mappers.get(exceptionClass);
        if (existing != null) {
            if (existing.getPriority() < mapper.getPriority()) {
                //already a higher priority mapper
                return;
            }
        }
        mappers.put(exceptionClass, mapper);
    }

    /**
     * Return the proper Exception that handles {@param clazz} or {@code null}
     * if none is found.
     * First checks if the Resource class that contained the Resource method contained class-level exception mappers.
     * {@param throwable} is optional and is used to when no mapper has been found for the original exception type, but the
     * application
     * has been configured to unwrap certain exceptions.
     */
    public <T extends Throwable> Map.Entry<Throwable, jakarta.ws.rs.ext.ExceptionMapper<? extends Throwable>> getExceptionMapper(
            Class<T> clazz,
            ResteasyReactiveRequestContext context,
            T throwable) {
        Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> classExceptionMappers = getClassExceptionMappers(
                context);
        if ((classExceptionMappers != null) && !classExceptionMappers.isEmpty()) {
            Map.Entry<Throwable, jakarta.ws.rs.ext.ExceptionMapper<? extends Throwable>> result = doGetExceptionMapper(clazz,
                    classExceptionMappers, throwable);
            if (result != null) {
                return result;
            }
        }
        return doGetExceptionMapper(clazz, mappers, throwable);
    }

    private Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> getClassExceptionMappers(
            ResteasyReactiveRequestContext context) {
        if (context == null) {
            return null;
        }
        return context.getTarget() != null ? context.getTarget().getClassExceptionMappers() : null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T extends Throwable> Map.Entry<Throwable, jakarta.ws.rs.ext.ExceptionMapper<? extends Throwable>> doGetExceptionMapper(
            Class<T> clazz,
            Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> mappers,
            Throwable throwable) {
        Class<?> klass = clazz;
        do {
            ResourceExceptionMapper<? extends Throwable> mapper = mappers.get(klass);
            if (mapper != null) {
                return new AbstractMap.SimpleEntry(throwable, mapper.getFactory()
                        .createInstance().getInstance());
            }
            klass = klass.getSuperclass();
        } while (klass != null);

        if ((throwable != null) && unwrappedExceptions.contains(clazz)) {
            Throwable cause = throwable.getCause();
            if (cause != null) {
                return doGetExceptionMapper(cause.getClass(), mappers, cause);
            }
        }
        return null;
    }

    public static Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> getMappers() {
        return mappers;
    }

}
