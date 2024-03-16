package io.quarkus.rest.client.reactive.jaxb.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.xml.transform.stream.StreamSource;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import org.jboss.resteasy.reactive.common.util.StreamUtil;

@ActivateRequestContext
public class ClientJaxbMessageBodyReader implements MessageBodyReader<Object> {

    @Inject
    Unmarshaller unmarshaller;

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws WebApplicationException, IOException {
        return doReadFrom(type, entityStream);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isReadable(mediaType, type);
    }

    protected boolean isReadable(MediaType mediaType, Class<?> type) {
        if (mediaType == null) {
            return false;
        }
        if (String.class.equals(type)) { // don't attempt to read plain strings
            return false;
        }
        String subtype = mediaType.getSubtype();
        boolean isCorrectMediaType = "application".equals(mediaType.getType()) || "text".equals(mediaType.getType());
        return (isCorrectMediaType && "xml".equalsIgnoreCase(subtype) || subtype.endsWith("+xml"))
                || (mediaType.isWildcardSubtype() && (mediaType.isWildcardType() || isCorrectMediaType));
    }

    private Object doReadFrom(Class<Object> type, InputStream entityStream) throws IOException {
        if (isInputStreamEmpty(entityStream)) {
            return null;
        }

        return unmarshal(entityStream, type);
    }

    protected Object unmarshal(InputStream entityStream, Class<Object> type) {
        try {
            JAXBElement<Object> item = unmarshaller.unmarshal(new StreamSource(entityStream), type);
            return item.getValue();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isInputStreamEmpty(InputStream entityStream) throws IOException {
        return StreamUtil.isEmpty(entityStream) || entityStream.available() == 0;
    }
}
