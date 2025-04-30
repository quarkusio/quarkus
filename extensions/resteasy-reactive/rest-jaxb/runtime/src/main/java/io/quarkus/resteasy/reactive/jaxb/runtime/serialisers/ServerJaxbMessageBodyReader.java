package io.quarkus.resteasy.reactive.jaxb.runtime.serialisers;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.xml.transform.stream.StreamSource;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Providers;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.UnmarshalException;
import jakarta.xml.bind.Unmarshaller;

import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import io.quarkus.jaxb.runtime.JaxbContextConfigRecorder;

public class ServerJaxbMessageBodyReader implements ServerMessageBodyReader<Object> {

    @Inject
    Unmarshaller unmarshaller;

    @Context
    Providers providers;

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws WebApplicationException, IOException {
        return doReadFrom(type, genericType, entityStream);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, MediaType mediaType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        return doReadFrom(type, genericType, context.getInputStream());
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isReadable(mediaType, type);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod, MediaType mediaType) {
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

    protected Object unmarshal(InputStream entityStream, Class<Object> type) {
        try {
            JAXBElement<Object> item = getUnmarshall(type)
                    .unmarshal(new StreamSource(entityStream), type);
            return item.getValue();
        } catch (UnmarshalException e) {
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private Unmarshaller getUnmarshall(Class<Object> type) throws JAXBException {
        if (JaxbContextConfigRecorder.isClassBound(type)) {
            return unmarshaller;
        }

        return providers.getContextResolver(JAXBContext.class, MediaType.APPLICATION_XML_TYPE)
                .getContext(type)
                .createUnmarshaller();
    }

    private Object doReadFrom(Class<Object> type, Type genericType, InputStream entityStream) throws IOException {
        PushbackInputStream pushbackEntityStream = new PushbackInputStream(entityStream);
        if (isStreamEmpty(pushbackEntityStream)) {
            return null;
        }
        return unmarshal(pushbackEntityStream, type);
    }

    private boolean isStreamEmpty(PushbackInputStream pushbackStream) throws IOException {
        int firstByte = pushbackStream.read();
        pushbackStream.unread(firstByte);
        return firstByte == -1;
    }
}
