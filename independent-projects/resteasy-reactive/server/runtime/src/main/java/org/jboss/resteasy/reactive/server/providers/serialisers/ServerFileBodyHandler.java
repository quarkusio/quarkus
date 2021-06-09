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
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

// TODO: this is very simplistic at the moment

@Provider
@Produces("*/*")
@Consumes("*/*")
public class ServerFileBodyHandler extends FileBodyHandler implements ServerMessageBodyWriter<File> {

    @Override
    public long getSize(File o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return o.length();
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return File.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(File o, Type genericType, ServerRequestContext context) throws WebApplicationException {
        ServerHttpResponse vertxResponse = context.serverResponse();
        ByteArrayOutputStream baos = new ByteArrayOutputStream((int) o.length());
        try {
            doWrite(o, baos);
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }
        vertxResponse.end(baos.toByteArray());
    }
}
