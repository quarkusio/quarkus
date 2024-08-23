package io.quarkus.resteasy.common.runtime.graal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.spi.ResteasyConfiguration;
import org.w3c.dom.Document;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Manipulating {@link Document}s in REST services is very unlikely to be needed
 * and this provider contributes a significant amount of code to the native
 * image due to its dependency to Xerces and Xalan.
 * <p>
 * Let's remove it for now and see if people complain about it. If so, we
 * will need a more advanced strategy to disable/enable it.
 */
@TargetClass(className = "org.jboss.resteasy.plugins.providers.DocumentProvider")
final class DeleteDocumentProvider {

    @Substitute
    public DeleteDocumentProvider(final @Context ResteasyConfiguration config) {
    }

    @Substitute
    public boolean isReadable(Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType) {
        return false;
    }

    @Substitute
    public Document readFrom(Class<Document> clazz, Type type, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> headers,
            InputStream input) throws IOException, WebApplicationException {
        return null;
    }

    @Substitute
    public boolean isWriteable(Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType) {
        return false;
    }

    @Substitute
    public void writeTo(Document document, Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType,
            MultivaluedMap<String, Object> headers,
            OutputStream output) throws IOException, WebApplicationException {

    }
}
