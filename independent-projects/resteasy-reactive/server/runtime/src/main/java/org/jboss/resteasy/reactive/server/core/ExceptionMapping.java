package org.jboss.resteasy.reactive.server.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.model.ResourceExceptionMapper;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveExceptionMapper;

public class ExceptionMapping {

    private static final Logger log = Logger.getLogger(ExceptionMapping.class);

    private final Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> mappers = new HashMap<>();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Response mapException(Throwable throwable, ResteasyReactiveRequestContext context) {
        Class<?> klass = throwable.getClass();
        boolean isWebApplicationException = throwable instanceof WebApplicationException;
        Response response = null;
        if (isWebApplicationException) {
            response = ((WebApplicationException) throwable).getResponse();
        }
        if (response != null && response.hasEntity())
            return response;
        // we match superclasses only if not a WebApplicationException according to spec 3.3.4 Exceptions
        ExceptionMapper exceptionMapper = getExceptionMapper((Class<Throwable>) klass, context);
        if (exceptionMapper != null) {
            if (exceptionMapper instanceof ResteasyReactiveExceptionMapper) {
                return ((ResteasyReactiveExceptionMapper) exceptionMapper).toResponse(throwable, context);
            }
            return exceptionMapper.toResponse(throwable);
        }
        if (isWebApplicationException) {
            // FIXME: can response be null?
            return response;
        }
        if (throwable instanceof IOException) {
            log.debugf(throwable,
                    "IOError processing HTTP request to %s failed, the client likely terminated the connection.",
                    context.serverRequest().getRequestAbsoluteUri());
        } else {
            log.error("Request failed ", throwable);
        }
        // FIXME: configurable? stack trace?
        return Response.serverError().build();
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
        mappers.put(exceptionClass, mapper);
    }

}
