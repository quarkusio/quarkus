package io.quarkus.rest.runtime.core;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.jboss.logging.Logger;

import io.quarkus.rest.runtime.model.ResourceExceptionMapper;

public class ExceptionMapping {

    private static final Logger log = Logger.getLogger(ExceptionMapping.class);

    private final Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> mappers = new HashMap<>();

    @SuppressWarnings("unchecked")
    public Response mapException(Throwable throwable) {
        Class<?> klass = throwable.getClass();
        boolean isWebApplicationException = throwable instanceof WebApplicationException;
        Response response = null;
        if (isWebApplicationException) {
            response = ((WebApplicationException) throwable).getResponse();
        }
        if (response != null && response.hasEntity())
            return response;
        // we match superclasses only if not a WebApplicationException according to spec 3.3.4 Exceptions
        ExceptionMapper exceptionMapper = getExceptionMapper((Class<Throwable>) klass, isWebApplicationException);
        if (exceptionMapper != null) {
            return exceptionMapper.toResponse(throwable);
        }
        if (isWebApplicationException) {
            // if we have a subtype of WebApplicationException, we must also try the WebApplicationException type, according to spec
            if (klass != WebApplicationException.class) {
                exceptionMapper = getExceptionMapper(WebApplicationException.class, isWebApplicationException);
            }
            if (exceptionMapper != null) {
                return exceptionMapper.toResponse(throwable);
            }
            // FIXME: can response be null?
            return response;
        }
        log.error("Request failed ", throwable);
        // FIXME: configurable? stack trace?
        return Response.serverError().build();
    }

    /**
     * Return the proper Exception that handles {@param throwable} or {@code null}
     * if none is found
     */
    @SuppressWarnings("unchecked")
    public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> clazz, boolean singleLevel) {
        Class<?> klass = clazz;
        do {
            ResourceExceptionMapper<? extends Throwable> mapper = mappers.get(klass);
            if (mapper != null) {
                return (ExceptionMapper<T>) mapper.getFactory()
                        .createInstance().getInstance();
            }
            if (singleLevel)
                return null;
            klass = klass.getSuperclass();
        } while (klass != null);

        return null;
    }

    public <T extends Throwable> void addExceptionMapper(Class<T> exceptionClass, ResourceExceptionMapper<T> mapper) {
        mappers.put(exceptionClass, mapper);
    }

}
