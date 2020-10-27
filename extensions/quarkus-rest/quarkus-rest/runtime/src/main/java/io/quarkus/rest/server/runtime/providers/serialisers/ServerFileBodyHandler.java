package io.quarkus.rest.server.runtime.providers.serialisers;

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

import io.quarkus.rest.common.runtime.providers.serialisers.FileBodyHandler;
import io.quarkus.rest.server.runtime.core.LazyMethod;
import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.server.runtime.spi.QuarkusRestMessageBodyWriter;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;

// TODO: this is very simplistic at the moment

@Provider
@Produces("*/*")
@Consumes("*/*")
public class ServerFileBodyHandler extends FileBodyHandler implements QuarkusRestMessageBodyWriter<File> {

    @Override
    public long getSize(File o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return o.length();
    }

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return File.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(File o, QuarkusRestRequestContext context) throws WebApplicationException {
        HttpServerResponse vertxResponse = context.getHttpServerResponse();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            doWrite(o, baos);
        } catch (IOException e) {
            throw new WebApplicationException(e);
        }
        vertxResponse.end(Buffer.buffer(baos.toByteArray()));
    }
}
