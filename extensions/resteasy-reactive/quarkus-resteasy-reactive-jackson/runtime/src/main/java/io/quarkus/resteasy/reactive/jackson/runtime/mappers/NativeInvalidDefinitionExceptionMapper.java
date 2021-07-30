package io.quarkus.resteasy.reactive.jackson.runtime.mappers;

import javax.ws.rs.Priorities;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.fasterxml.jackson.databind.type.SimpleType;

import io.quarkus.bootstrap.graal.ImageInfo;

public class NativeInvalidDefinitionExceptionMapper {

    protected static final Logger log = Logger.getLogger(NativeInvalidDefinitionExceptionMapper.class);

    @ServerExceptionMapper(priority = Priorities.USER + 100)
    public Response toResponse(InvalidDefinitionException e, SimpleResourceInfo resourceInfo) {
        if (ImageInfo.inImageRuntimeCode() && (e.getMessage().startsWith("No serializer found"))) {
            JavaType effectiveType = determineType(e.getType());
            if (effectiveType != null) {
                log.error("Jackson was unable to serialize type '" + effectiveType.toCanonical()
                        + "'. Consider annotating the class with '@RegisterForReflection' or using 'org.jboss.resteasy.reactive.RestResponse' as a response type of '"
                        + resourceInfo.getResourceClass().getName() + "#" + resourceInfo.getMethodName(), e);
            } else {
                // we were not able to determine the type, so just log the exception
                log.error(e);
            }
        }
        return Response.serverError().build();
    }

    private JavaType determineType(JavaType providedType) {
        if (providedType instanceof SimpleType) {
            return providedType;
        }
        if (providedType instanceof CollectionLikeType) {
            return determineType(providedType.getContentType());
        }
        // TODO: add more types
        return null;
    }
}
