package io.quarkus.resteasy.runtime;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import io.smallrye.mutiny.CompositeException;

@Provider
public class CompositeExceptionMapper implements ExceptionMapper<CompositeException> {

    @Context
    Providers p;

    @SuppressWarnings("unchecked")
    @Override
    public Response toResponse(CompositeException ex) {
        Throwable t = ex.getCauses().stream().filter(s -> s != null).findFirst()
                .orElseThrow(() -> new InternalServerErrorException());

        ExceptionMapper<Throwable> mapper = (ExceptionMapper<Throwable>) p.getExceptionMapper(t.getClass());
        if (mapper == null) {
            throw new InternalServerErrorException();
        }
        return mapper.toResponse(t);
    }

}
