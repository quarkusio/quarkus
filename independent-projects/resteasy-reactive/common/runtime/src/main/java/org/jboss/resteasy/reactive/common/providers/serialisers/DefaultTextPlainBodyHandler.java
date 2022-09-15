package org.jboss.resteasy.reactive.common.providers.serialisers;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import org.jboss.resteasy.reactive.common.util.TypeConverter;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 *
 *          TODO: Reevaluate this as it depends on a lot of reflection for reading Java types.
 *          It should not be difficult to write handlers for these cases...
 */
public abstract class DefaultTextPlainBodyHandler implements MessageBodyReader<Object> {

    public boolean isReadable(Class type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        // StringTextStar should pick up strings
        return !String.class.equals(type) && TypeConverter.isConvertable(type);
    }

    public Object readFrom(Class type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        return doReadFrom(type, mediaType, entityStream);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Object doReadFrom(Class type, MediaType mediaType, InputStream entityStream) throws IOException {
        String input = MessageReaderUtil.readString(entityStream, mediaType);
        validateInput(input);
        return TypeConverter.getType(type, input);
    }

    protected abstract void validateInput(String input) throws ProcessingException;
}
