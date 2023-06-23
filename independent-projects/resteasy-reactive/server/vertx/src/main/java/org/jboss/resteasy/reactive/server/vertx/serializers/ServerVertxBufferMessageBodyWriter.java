package org.jboss.resteasy.reactive.server.vertx.serializers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;

@Provider
public class ServerVertxBufferMessageBodyWriter implements ServerMessageBodyWriter<Buffer> {

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    public void writeTo(Buffer buffer, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        entityStream.write(buffer.getBytes());
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeResponse(Buffer buffer, Type genericType, ServerRequestContext context) throws WebApplicationException {
        // optimization to avoid slicing the Netty buffer by using Buffer::getByteBuf
        if (buffer instanceof BufferImpl) {
            var vertxBuffer = (BufferImpl) buffer;
            var nettyBuffer = (ByteBuf) vertxBuffer.byteBuf();
            // it shouldn't be null but better safe than sorry
            if (nettyBuffer != null && nettyBuffer.hasArray()) {
                writeSharedArrayRange(nettyBuffer, context);
                return;
            }
        }
        context.serverResponse().end(buffer.getBytes());
    }

    private static void writeSharedArrayRange(ByteBuf nettyBuffer, ServerRequestContext context) {
        byte[] array = nettyBuffer.array();
        final int offset = nettyBuffer.arrayOffset() + nettyBuffer.readerIndex();
        final int length = nettyBuffer.readableBytes();
        context.serverResponse().endShared(array, offset, length);
    }
}
