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

    private Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> mappers = new HashMap<>();

    @SuppressWarnings("unchecked")
    public Response mapException(Throwable throwable) {
        Class<?> klass = throwable.getClass();
        ExceptionMapper<Throwable> exceptionMapper = getExceptionMapper((Class<Throwable>) klass);
        if (exceptionMapper != null) {
            return exceptionMapper.toResponse(throwable);
        }
        if (throwable instanceof WebApplicationException) {
            return ((WebApplicationException) throwable).getResponse();
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
    public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> clazz) {
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
