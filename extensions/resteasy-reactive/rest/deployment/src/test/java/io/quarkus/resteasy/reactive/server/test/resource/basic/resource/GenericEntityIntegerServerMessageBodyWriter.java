package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

@Provider
@Produces("*/*")
public class GenericEntityIntegerServerMessageBodyWriter implements ServerMessageBodyWriter<List<Integer>> {

    private static final Logger LOG = Logger.getLogger(GenericEntityIntegerServerMessageBodyWriter.class);

    @Override
    public long getSize(List<Integer> integers, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        throw new IllegalStateException("Should never have been called");
    }

    @Override
    public void writeTo(List<Integer> integers, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        throw new IllegalStateException("Should never have been called");
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        if (!List.class.isAssignableFrom(type)) {
            return false;
        }
        if (!(genericType instanceof ParameterizedType)) {
            return false;
        }
        ParameterizedType pt = (ParameterizedType) genericType;
        boolean result = pt.getActualTypeArguments()[0].equals(Integer.class);
        LOG.debug("IntegerWriter result!!!: " + result);
        return result;
    }

    @Override
    public void writeResponse(List<Integer> integers, Type genericType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        StringBuilder buf = new StringBuilder();
        for (Integer i : integers) {
            buf.append(i.toString()).append("I ");
        }
        context.getOrCreateOutputStream().write(buf.toString().getBytes());
    }
}
