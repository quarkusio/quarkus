package io.quarkus.rest.runtime.core;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.jboss.logging.Logger;

import io.quarkus.rest.runtime.model.ResourceExceptionMapper;
import io.quarkus.rest.runtime.spi.QuarkusRestExceptionMapper;

public class ExceptionMapping {

    private static final Logger log = Logger.getLogger(ExceptionMapping.class);

    private final Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> mappers = new HashMap<>();

    @SuppressWarnings("unchecked")
    public Response mapException(Throwable throwable, QuarkusRestRequestContext context) {
        Class<?> klass = throwable.getClass();
        boolean isWebApplicationException = throwable instanceof WebApplicationException;
        Response response = null;
        if (isWebApplicationException) {
            response = ((WebApplicationException) throwable).getResponse();
        }
        if (response != null && response.hasEntity())
            return response;
        // we match superclasses only if not a WebApplicationException according to spec 3.3.4 Exceptions
        ExceptionMapper exceptionMapper = getExceptionMapper((Class<Throwable>) klass);
        if (exceptionMapper != null) {
            if (exceptionMapper instanceof QuarkusRestExceptionMapper) {
                return ((QuarkusRestExceptionMapper) exceptionMapper).toResponse(throwable, context);
            }
            return exceptionMapper.toResponse(throwable);
        }
        if (isWebApplicationException) {
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
