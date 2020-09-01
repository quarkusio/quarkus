package io.quarkus.rest.runtime.core.serialization;

import java.io.IOException;
import java.util.Map;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;

/**
 * An entity writer that will delegate based on the actual type of the
 * returned entity.
 */
public class MediaTypeDelegatingEntityWriter implements EntityWriter {

    private final Map<MediaType, EntityWriter> typeMap;

    public MediaTypeDelegatingEntityWriter(Map<MediaType, EntityWriter> typeMap) {
        this.typeMap = typeMap;
    }

    @Override
    public void write(QuarkusRestRequestContext context, Object entity) throws IOException {
        EntityWriter delegate = typeMap.get(context.getResponse().getMediaType());
        if (delegate != null) {
            delegate.write(context, entity);
            return;
        }
        throw new InternalServerErrorException("Could not find MessageBodyWriter for " + entity.getClass(),
                Response.serverError().build());
    }
}
