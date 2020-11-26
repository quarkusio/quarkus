package org.jboss.resteasy.reactive.server.providers.serialisers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.common.providers.serialisers.FileBodyHandler;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.LazyMethod;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;

// TODO: this is very simplistic at the moment

@Provider
@Produces("*/*")
@Consumes("*/*")
public class ServerFileBodyHandler extends FileBodyHandler implements ResteasyReactiveMessageBodyWriter<File> {

    @Override
    public long getSize(File o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return o.length();
    }

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return File.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(File o, ResteasyReactiveRequestContext context) throws WebApplicationException {
        ServerHttpResponse vertxResponse = context.serverResponse();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            doWrite(o, baos);
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }
        vertxResponse.end(baos.toByteArray());
    }
}
