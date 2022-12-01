package io.quarkus.resteasy.reactive.server.test.resource.basic.resource;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

@Provider
@Produces("*/*")
public class GenericEntityFloatWriter implements MessageBodyWriter<List<Float>> {

    private static final Logger LOG = Logger.getLogger(GenericEntityFloatWriter.class);

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (!List.class.isAssignableFrom(type)) {
            return false;
        }
        if (!(genericType instanceof ParameterizedType)) {
            return false;
        }
        ParameterizedType pt = (ParameterizedType) genericType;
        boolean result = pt.getActualTypeArguments()[0].equals(Float.class);
        LOG.debug("FloatWriter result!!!: " + result);
        return result;
    }

    public long getSize(List<Float> floats, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return -1;
    }

    public void writeTo(List<Float> floats, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        StringBuilder buf = new StringBuilder();
        for (Float f : floats) {
            buf.append(f.toString()).append("F ");
        }
        entityStream.write(buf.toString().getBytes());
    }
}
