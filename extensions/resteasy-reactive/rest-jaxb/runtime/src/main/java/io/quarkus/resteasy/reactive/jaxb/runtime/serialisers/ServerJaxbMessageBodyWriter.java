package io.quarkus.resteasy.reactive.jaxb.runtime.serialisers;

import java.beans.Introspector;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import javax.xml.namespace.QName;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Providers;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import io.quarkus.jaxb.runtime.JaxbContextConfigRecorder;
import io.vertx.core.MultiMap;

public class ServerJaxbMessageBodyWriter extends ServerMessageBodyWriter.AllWriteableMessageBodyWriter {

    @Inject
    Marshaller marshaller;

    @Context
    Providers providers;

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
            Class<?> clazz = o.getClass();
            Object jaxbObject = o;
            if (o instanceof JAXBElement) {
                clazz = ((JAXBElement<?>) o).getDeclaredType();
            } else {
                XmlRootElement jaxbElement = clazz.getAnnotation(XmlRootElement.class);
                if (jaxbElement == null) {
                    jaxbObject = new JAXBElement(new QName(Introspector.decapitalize(clazz.getSimpleName())), clazz, o);
                }
            }

            getMarshall(clazz).marshal(jaxbObject, outputStream);

        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private Marshaller getMarshall(Class<?> type) throws JAXBException {
        if (JaxbContextConfigRecorder.isClassBound(type)) {
            return marshaller;
        }

        return providers.getContextResolver(JAXBContext.class, MediaType.APPLICATION_XML_TYPE)
                .getContext(type)
                .createMarshaller();
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
