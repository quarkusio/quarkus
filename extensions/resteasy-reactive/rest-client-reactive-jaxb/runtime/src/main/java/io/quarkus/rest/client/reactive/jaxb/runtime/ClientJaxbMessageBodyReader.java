package io.quarkus.rest.client.reactive.jaxb.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

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
