package io.quarkus.qrs.runtime.core;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import io.quarkus.qrs.runtime.model.ResourceExceptionMapper;

public class ExceptionMapping {

    private Map<String, ResourceExceptionMapper<Throwable>> mappers = new HashMap<>();

    public Response mapException(Throwable throwable, RequestContext requestContext) {
        Class<? extends Throwable> klass = throwable.getClass();
        return mapException(throwable, klass, requestContext);
    }

    private Response mapException(Throwable throwable, Class<?> klass, RequestContext requestContext) {
        do {
            ResourceExceptionMapper<Throwable> mapper = mappers.get(klass.getName());
            if (mapper != null) {
                return mapper.getFactory().createInstance(requestContext).getInstance().toResponse(throwable);
            }
            klass = klass.getSuperclass();
        } while (klass != null);

        // FIXME: configurable? stack trace?
        return Response.serverError().build();
    }

    public void addExceptionMapper(String exceptionClass, ResourceExceptionMapper<Throwable> mapper) {
        mappers.put(exceptionClass, mapper);
    }

}
