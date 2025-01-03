package org.jboss.resteasy.reactive.client.handlers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.client.impl.ClientResponseBuilderImpl;
import org.jboss.resteasy.reactive.client.impl.ClientResponseContextImpl;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.impl.multipart.FileDownloadImpl;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.client.spi.FieldFiller;
import org.jboss.resteasy.reactive.client.spi.MultipartResponseData;
import org.jboss.resteasy.reactive.common.jaxrs.ResponseImpl;
import org.jboss.resteasy.reactive.common.jaxrs.StatusTypeImpl;

import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

public class ClientResponseCompleteRestHandler implements ClientRestHandler {

    @Override
    public void handle(RestClientRequestContext context) throws Exception {
        context.getResult().complete(mapToResponse(context, true));
    }

    public static ResponseImpl mapToResponse(RestClientRequestContext context,
            boolean parseContent)
            throws IOException {
        ClientResponseContextImpl responseContext = context.getOrCreateClientResponseContext();
        Response.StatusType statusType = new StatusTypeImpl(responseContext.getStatus(), responseContext.getReasonPhrase());
        return mapToResponse(context, statusType, parseContent);
    }

    public static ResponseImpl mapToResponse(RestClientRequestContext context,
            Response.StatusType effectiveResponseStatus,
            boolean parseContent)
            throws IOException {
        Map<Class<?>, MultipartResponseData> multipartDataMap = context.getMultipartResponsesData();
        ClientResponseContextImpl responseContext = context.getOrCreateClientResponseContext();
        ClientResponseBuilderImpl builder = new ClientResponseBuilderImpl();
        builder.status(effectiveResponseStatus);
        builder.setAllHeaders(responseContext.getHeaders());
        builder.invocationState(context);
        InputStream entityStream = responseContext.getEntityStream();
        if (context.isResponseTypeSpecified()
                // when we are returning a RestResponse, we don't want to do any parsing
                && (Response.Status.Family.familyOf(context.getResponseStatus()) == Response.Status.Family.SUCCESSFUL)
                && parseContent) { // this case means that a specific response type was requested
            if (context.getResponseMultipartParts() != null) {
                GenericType<?> responseType = context.getResponseType();
                if (!(responseType.getType() instanceof Class)) {
                    throw new IllegalArgumentException("Not supported return type for a multipart message, " +
                            "expected a non-generic class got : " + responseType.getType());
                }
                Class<?> responseClass = (Class<?>) responseType.getType();
                MultipartResponseData multipartData = multipartDataMap.get(responseClass);
                if (multipartData == null) {
                    throw new IllegalStateException("Failed to find multipart data for class " + responseClass + ". " +
                            "If it's meant to be used as multipart response type, consider annotating it with @MultipartForm");
                }
                Object result = multipartData.newInstance();
                builder.entity(result);
                List<InterfaceHttpData> parts = context.getResponseMultipartParts();
                for (InterfaceHttpData httpData : parts) {
                    FieldFiller fieldFiller = null;
                    // find the correct filler
                    for (FieldFiller ff : multipartData.getFieldFillers()) {
                        if (ff.getPartName().equals(httpData.getName())) {
                            fieldFiller = ff;
                            break;
                        }
                    }
                    if (fieldFiller == null) {
                        continue;
                    }
                    if (httpData instanceof Attribute at) {
                        // TODO: get rid of ByteArrayInputStream
                        // TODO: maybe we could extract something closer to input stream from attribute
                        ByteArrayInputStream in = new ByteArrayInputStream(
                                at.getValue().getBytes(StandardCharsets.UTF_8));
                        Object fieldValue = context.readEntity(in,
                                fieldFiller.getFieldType(),
                                MediaType.valueOf(fieldFiller.getMediaType()),
                                context.getMethodDeclaredAnnotationsSafe(),
                                // FIXME: we have strings, it wants objects, perhaps there's
                                // an Object->String conversion too many
                                (MultivaluedMap) responseContext.getHeaders());
                        if (fieldValue != null) {
                            fieldFiller.set(result, fieldValue);
                        }
                    } else if (httpData instanceof FileUpload fu) {
                        fieldFiller.set(result, new FileDownloadImpl(fu));
                    } else {
                        throw new IllegalArgumentException("Unsupported multipart message element type. " +
                                "Expected FileAttribute or Attribute, got: " + httpData.getClass());
                    }
                }
            } else {
                Class<?> rawType = context.getResponseType().getRawType();
                if (context.isFileDownload()) {
                    if (File.class.equals(rawType)) {
                        builder.entity(new File(context.getTmpFilePath()));
                    } else if (Path.class.equals(rawType)) {
                        builder.entity(Paths.get(context.getTmpFilePath()));
                    } else {
                        throw new IllegalStateException("Unhandled type: " + rawType);
                    }
                    context.clearTmpFilePath();
                } else if (!void.class.equals(rawType)) {
                    Object entity = context.readEntity(entityStream,
                            context.getResponseType(),
                            responseContext.getMediaType(),
                            context.getMethodDeclaredAnnotationsSafe(),
                            // FIXME: we have strings, it wants objects, perhaps there's
                            // an Object->String conversion too many
                            (MultivaluedMap) responseContext.getHeaders());
                    if (entity != null) {
                        builder.entity(entity);
                    }
                }
            }
        } else {
            // in this case no specific response type was requested so we just prepare the stream
            // the users of the response are meant to use readEntity
            builder.entityStream(entityStream);
        }
        return builder.build();
    }

}
