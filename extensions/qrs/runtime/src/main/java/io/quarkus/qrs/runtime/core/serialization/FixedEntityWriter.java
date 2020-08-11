package io.quarkus.qrs.runtime.core.serialization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.spi.QrsMessageBodyWriter;
import io.vertx.core.buffer.Buffer;

/**
 * A fixed entity writer that can be used when we know the result will always be written
 * by a given provider.
 */
public class FixedEntityWriter implements EntityWriter {

    private final MessageBodyWriter writer;

    public FixedEntityWriter(MessageBodyWriter writer) {
        this.writer = writer;
    }

    @Override
    public void write(QrsRequestContext context, Object entity) throws IOException {
        invokeWriter(context, entity, writer);
    }

    public static void invokeWriter(QrsRequestContext context, Object entity, MessageBodyWriter writer) throws IOException {
        Response response = context.getResponse();
        if (writer instanceof QrsMessageBodyWriter) {
            ((QrsMessageBodyWriter<Object>) writer).writeResponse(entity, context);
        } else {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            writer.writeTo(entity, context.getTarget().getLazyMethod().getMethod().getReturnType(),
                    context.getTarget().getLazyMethod().getGenericReturnType(),
                    context.getTarget().getLazyMethod().getAnnotations(), response.getMediaType(), response.getHeaders(), baos);
            context.getContext().response().end(Buffer.buffer(baos.toByteArray()));
        }
    }
}
