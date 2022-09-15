package org.jboss.resteasy.reactive.server.vertx.test.resource.basic.resource;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import org.jboss.logging.Logger;

@Provider
@Produces("*/*")
public class GenericEntityDoubleWriter implements MessageBodyWriter<List<Double>> {

    private static final Logger LOG = Logger.getLogger(GenericEntityDoubleWriter.class);

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        LOG.debug("DoubleWriter type: " + type.getName());
        if (!List.class.isAssignableFrom(type)) {
            return false;
        }
        LOG.debug("DoubleWriter: " + genericType);
        if (!(genericType instanceof ParameterizedType)) {
            return false;
        }
        LOG.debug("DoubleWriter");
        ParameterizedType pt = (ParameterizedType) genericType;
        boolean result = pt.getActualTypeArguments()[0].equals(Double.class);
        LOG.debug("Doublewriter result!!!: " + result);
        return result;
    }

    public long getSize(List<Double> doubles, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return -1;
    }

    public void writeTo(List<Double> floats, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        StringBuilder buf = new StringBuilder();
        for (Double f : floats) {
            buf.append(f.toString()).append("D ");
        }
        entityStream.write(buf.toString().getBytes());
    }
}
