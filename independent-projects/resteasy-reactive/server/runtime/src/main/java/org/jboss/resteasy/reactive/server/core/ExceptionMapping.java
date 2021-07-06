package org.jboss.resteasy.reactive.server.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.model.ResourceExceptionMapper;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveAsyncExceptionMapper;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveExceptionMapper;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class ExceptionMapping {

    private static final Logger log = Logger.getLogger(ExceptionMapping.class);

    private final Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> mappers = new HashMap<>();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void mapException(Throwable throwable, ResteasyReactiveRequestContext context) {
        Class<?> klass = throwable.getClass();
        boolean isWebApplicationException = throwable instanceof WebApplicationException;
        Response response = null;
        if (isWebApplicationException) {
            response = ((WebApplicationException) throwable).getResponse();
        }
        if (response != null && response.hasEntity()) {
            context.setResult(response);
            return;
        }

        // we match superclasses only if not a WebApplicationException according to spec 3.3.4 Exceptions
        ExceptionMapper exceptionMapper = getExceptionMapper((Class<Throwable>) klass, context);
        if (exceptionMapper != null) {
            context.requireCDIRequestScope();
            if (exceptionMapper instanceof ResteasyReactiveAsyncExceptionMapper) {
                ((ResteasyReactiveAsyncExceptionMapper) exceptionMapper).asyncResponse(throwable,
                        new AsyncExceptionMapperContextImpl(context));
                return;
            } else if (exceptionMapper instanceof ResteasyReactiveExceptionMapper) {
                response = ((ResteasyReactiveExceptionMapper) exceptionMapper).toResponse(throwable, context);
            } else {
                response = exceptionMapper.toResponse(throwable);
            }
            context.setResult(response);
            return;
        }
        if (isWebApplicationException) {
            context.setResult(response);
            return;
        }
        if (throwable instanceof IOException) {
            log.debugf(throwable,
                    "IOError processing HTTP request to %s failed, the client likely terminated the connection.",
                    context.serverRequest().getRequestAbsoluteUri());
        } else {
            log.error("Request failed ", throwable);
        }
        context.handleUnmappedException(throwable);
    }

    /**
     * Return the proper Exception that handles {@param throwable} or {@code null}
     * if none is found.
     * First checks if the Resource class that contained the Resource method contained class-level exception mappers
     */
    public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> clazz, ResteasyReactiveRequestContext context) {
        Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> classExceptionMappers = getClassExceptionMappers(
                context);
        if ((classExceptionMappers != null) && !classExceptionMappers.isEmpty()) {
            ExceptionMapper<T> result = doGetExceptionMapper(clazz, classExceptionMappers);
            if (result != null) {
                return result;
            }
        }
        return doGetExceptionMapper(clazz, mappers);
    }

    private Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> getClassExceptionMappers(
            ResteasyReactiveRequestContext context) {
        if (context == null) {
            return null;
        }
        return context.getTarget() != null ? context.getTarget().getClassExceptionMappers() : null;
    }

    @SuppressWarnings("unchecked")
    private <T extends Throwable> ExceptionMapper<T> doGetExceptionMapper(Class<T> clazz,
            Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> mappers) {
        Class<?> klass = clazz;
        do {
            ResourceExceptionMapper<? extends Throwable> mapper = mappers.get(klass);
            if (mapper != null) {
                return (ExceptionMapper<T>) mapper.getFactory()
                        .createInstance().getInstance();
            }
            klass = klass.getSuperclass();
        } while (klass != null);

        return null;
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

    public Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> getMappers() {
        return mappers;
    }

    public void initializeDefaultFactories(Function<String, BeanFactory<?>> factoryCreator) {
        for (Map.Entry<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> entry : mappers.entrySet()) {
            if (entry.getValue().getFactory() == null) {
                entry.getValue().setFactory((BeanFactory) factoryCreator.apply(entry.getValue().getClassName()));
            }
        }
    }
}
