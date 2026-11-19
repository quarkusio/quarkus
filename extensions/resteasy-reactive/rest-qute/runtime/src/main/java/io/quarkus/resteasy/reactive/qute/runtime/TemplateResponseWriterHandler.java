package io.quarkus.resteasy.reactive.qute.runtime;

import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.serialization.EntityWriter;
import org.jboss.resteasy.reactive.server.core.serialization.FixedEntityWriter;
import org.jboss.resteasy.reactive.server.providers.serialisers.ServerByteArrayMessageBodyHandler;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

/**
 * The entity writer of a resource method that returns a template instance is selected at build time based on the
 * declared return type ({@link io.quarkus.qute.TemplateInstance}), i.e. the writer expects a String entity.
 * However, {@link TemplateResponseUniHandler} produces a byte array. Therefore, this handler replaces the entity
 * writer with a writer that is able to write the rendered byte array.
 */
public class TemplateResponseWriterHandler implements ServerRestHandler {

    private volatile EntityWriter writer;

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) {
        if (requestContext.getResponseEntity() instanceof byte[]) {
            // The rendered template is always a byte array; the default content type mirrors the behavior of the
            // previous implementation where a String entity was always negotiated to text/plain
            if (requestContext.getResponseContentType() == null) {
                requestContext.setResponseContentType(MediaType.TEXT_PLAIN_TYPE);
            }
            EntityWriter writer = this.writer;
            if (writer == null) {
                writer = new FixedEntityWriter(new ServerByteArrayMessageBodyHandler(),
                        requestContext.getDeployment().getSerialisers());
                this.writer = writer;
            }
            requestContext.setEntityWriter(writer);
        }
    }

}
