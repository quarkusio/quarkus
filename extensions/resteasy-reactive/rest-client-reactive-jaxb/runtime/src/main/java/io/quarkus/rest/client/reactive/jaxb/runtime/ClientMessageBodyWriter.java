package io.quarkus.rest.client.reactive.jaxb.runtime;

import java.beans.Introspector;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

@ActivateRequestContext
public class ClientMessageBodyWriter implements MessageBodyWriter<Object> {

    @Inject
    Marshaller marshaller;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws WebApplicationException {
        setContentTypeIfNecessary(httpHeaders);
        marshal(o, entityStream);
    }

    private void setContentTypeIfNecessary(MultivaluedMap<String, Object> httpHeaders) {
        Object contentType = httpHeaders.getFirst(HttpHeaders.CONTENT_TYPE);
        if (isNotXml(contentType)) {
            httpHeaders.putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML);
        }
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

    private boolean isNotXml(Object contentType) {
        return contentType == null || !contentType.toString().contains("xml");
    }
}
