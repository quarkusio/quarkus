package org.jboss.resteasy.reactive.common.providers.serialisers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
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

    @SuppressWarnings("unchecked")
    public Object readFrom(Class type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        String input = MessageReaderUtil.readString(entityStream, mediaType);
        validateInput(input);
        return TypeConverter.getType(type, input);
    }

    protected abstract void validateInput(String input) throws ProcessingException;
}
