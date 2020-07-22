package io.quarkus.qrs.runtime.core;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import io.quarkus.qrs.runtime.model.ResourceExceptionMapper;

public class ExceptionMapping {

    private Map<Class<? extends Throwable>, ResourceExceptionMapper<? extends Throwable>> mappers = new HashMap<>();

    public Response mapException(Throwable throwable, RequestContext requestContext) {
        Class<?> klass = throwable.getClass();
        do {
            ResourceExceptionMapper<? extends Throwable> mapper = mappers.get(klass);
            if (mapper != null) {
                ExceptionMapper<Throwable> instance = (ExceptionMapper<Throwable>) mapper.getFactory()
                        .createInstance(requestContext).getInstance();
                return instance.toResponse(throwable);
            }
            klass = klass.getSuperclass();
        } while (klass != null);

        // FIXME: configurable? stack trace?
        return Response.serverError().build();
    }

    public <T extends Throwable> void addExceptionMapper(Class<T> exceptionClass, ResourceExceptionMapper<T> mapper) {
        mappers.put(exceptionClass, mapper);
    }

}
