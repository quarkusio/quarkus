package io.quarkus.resteasy.reactive.server.runtime.multipart;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.ServerSerialisers;
import org.jboss.resteasy.reactive.server.handlers.RequestDeserializeHandler;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;

import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

/**
 * This class isn't used directly, it is however used by generated code meant to deal with multipart forms.
 */
public final class MultipartSupport {

    private static final Logger log = Logger.getLogger(RequestDeserializeHandler.class);

    private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];

    private MultipartSupport() {
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Object convertFormAttribute(String value, Class type, Type genericType, MediaType mediaType,
            ResteasyReactiveRequestContext context) {
        if (value == null) {
            return null;
        }

        ServerSerialisers serialisers = context.getDeployment().getSerialisers();
        List<MessageBodyReader<?>> readers = serialisers.findReaders(null, type, mediaType, RuntimeType.SERVER);
        if (readers.isEmpty()) {
            throw new NotSupportedException();
        }

        for (MessageBodyReader<?> reader : readers) {
            if (reader instanceof ServerMessageBodyReader) {
                ServerMessageBodyReader<?> serverMessageBodyReader = (ServerMessageBodyReader<?>) reader;
                if (serverMessageBodyReader.isReadable(type, genericType, context.getTarget().getLazyMethod(), mediaType)) {
                    // this should always be an empty stream as multipart doesn't set the request body
                    InputStream originalInputStream = context.getInputStream();
                    try {
                        // we need to set a fake input stream in order to trick the readers into thinking they are reading from the body
                        context.setInputStream(formAttributeValueToInputStream(value));
                        return serverMessageBodyReader.readFrom(type, genericType, mediaType, context);
                    } catch (IOException e) {
                        log.debug("Error occurred during deserialization of input", e);
                        throw new InternalServerErrorException(e);
                    } finally {
                        context.setInputStream(originalInputStream);
                    }

                }
            } else {
                // TODO: should we be passing in the annotations?
                if (reader.isReadable(type, genericType, EMPTY_ANNOTATIONS, mediaType)) {
                    try {
                        return reader.readFrom((Class) type, genericType, EMPTY_ANNOTATIONS, mediaType,
                                context.getHttpHeaders().getRequestHeaders(),
                                formAttributeValueToInputStream(value));
                    } catch (IOException e) {
                        log.debug("Error occurred during deserialization of input", e);
                        throw new InternalServerErrorException(e);
                    }
                }
            }
        }
        throw new NotSupportedException("Media type '" + mediaType + "' in multipart request is not supported");
    }

    public static QuarkusFileUpload getFileUpload(String formName, ResteasyReactiveRequestContext context) {
        RoutingContext routingContext = context.unwrap(RoutingContext.class);
        Set<FileUpload> fileUploads = routingContext.fileUploads();
        for (FileUpload fileUpload : fileUploads) {
            if (fileUpload.name().equals(formName)) {
                return new QuarkusFileUpload(fileUpload);
            }
        }
        return null;
    }

    public static List<QuarkusFileUpload> getFileUploads(ResteasyReactiveRequestContext context) {
        RoutingContext routingContext = context.unwrap(RoutingContext.class);
        Set<FileUpload> fileUploads = routingContext.fileUploads();
        List<QuarkusFileUpload> result = new ArrayList<>(fileUploads.size());
        for (FileUpload fileUpload : fileUploads) {
            result.add(new QuarkusFileUpload(fileUpload));
        }
        return result;
    }

    private static ByteArrayInputStream formAttributeValueToInputStream(String formAttributeValue) {
        return new ByteArrayInputStream(formAttributeValue.getBytes(StandardCharsets.UTF_8));
    }

}
