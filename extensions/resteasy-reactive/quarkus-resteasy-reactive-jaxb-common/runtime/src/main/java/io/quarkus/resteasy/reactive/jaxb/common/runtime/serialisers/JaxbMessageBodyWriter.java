package io.quarkus.resteasy.reactive.jaxb.common.runtime.serialisers;

import java.beans.Introspector;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import io.vertx.core.MultiMap;

public class JaxbMessageBodyWriter extends ServerMessageBodyWriter.AllWriteableMessageBodyWriter {

    @Inject
    Marshaller marshaller;

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws WebApplicationException {
        setContentTypeIfNecessary(httpHeaders);
        marshal(o, entityStream);
    }

    @Override
    public void writeResponse(Object o, Type genericType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        setContentTypeIfNecessary(context);
        OutputStream stream = context.getOrCreateOutputStream();
        marshal(o, stream);
        // we don't use try-with-resources because that results in writing to the http output without the exception mapping coming into play
        stream.close();
    }

    protected void marshal(Object o, OutputStream outputStream) {
        try {
            Object jaxbObject = o;
            Class<?> clazz = o.getClass();
            XmlRootElement jaxbElement = clazz.getAnnotation(XmlRootElement.class);
            if (jaxbElement == null) {
                jaxbObject = new JAXBElement(new QName(Introspector.decapitalize(clazz.getSimpleName())), clazz, o);
            }

            marshaller.marshal(jaxbObject, outputStream);

        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private void setContentTypeIfNecessary(MultivaluedMap<String, Object> httpHeaders) {
        Object contentType = httpHeaders.getFirst(HttpHeaders.CONTENT_TYPE);
        if (isNotXml(contentType)) {
            httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML);
        }
    }

    private void setContentTypeIfNecessary(ServerRequestContext context) {
        String currentContentType = null;
        Iterable<Map.Entry<String, String>> responseHeaders = context.serverResponse().getAllResponseHeaders();
        if (responseHeaders instanceof MultiMap) {
            currentContentType = ((MultiMap) responseHeaders).get(HttpHeaders.CONTENT_TYPE);
        } else {
            for (Map.Entry<String, String> entry : responseHeaders) {
                if (entry.getKey().equalsIgnoreCase(HttpHeaders.CONTENT_TYPE)) {
                    currentContentType = entry.getValue();
                    break;
                }
            }
        }
        if (isNotXml(currentContentType)) {
            context.serverResponse().setResponseHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML);
        }
    }

    private boolean isNotXml(Object contentType) {
        return contentType == null || !contentType.toString().contains("xml");
    }
}
