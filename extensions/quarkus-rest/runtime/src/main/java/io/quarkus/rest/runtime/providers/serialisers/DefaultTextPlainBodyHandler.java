package io.quarkus.rest.runtime.providers.serialisers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import io.quarkus.rest.runtime.util.TypeConverter;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 *
 *          TODO: Reevaluate this as it depends on a lot of reflection for reading Java types.
 *          It should not be difficult to write handlers for these cases...
 */
@Provider
@Consumes("text/plain")
public class DefaultTextPlainBodyHandler implements MessageBodyReader<Object> {

    public boolean isReadable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        // StringTextStar should pick up strings
        return !String.class.equals(type) && TypeConverter.isConvertable(type);
    }

    @SuppressWarnings("unchecked")
    public Object readFrom(Class type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return TypeConverter.getType(type, MessageReaderUtil.readString(entityStream, mediaType));
    }
}
