package io.quarkus.resteasy.reactive.kotlin.serialization.runtime

import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.MultivaluedMap
import java.io.OutputStream
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer
import org.jboss.resteasy.reactive.common.providers.serialisers.JsonMessageBodyWriterUtil
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter.AllWriteableMessageBodyWriter
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext

@Produces("application/json", "application/*+json", "text/json")
@OptIn(ExperimentalSerializationApi::class)
class KotlinSerializationMessageBodyWriter(private val json: Json) :
    AllWriteableMessageBodyWriter() {
    override fun writeTo(
        o: Any,
        type: Class<*>,
        genericType: Type,
        annotations: Array<Annotation>,
        mediaType: MediaType,
        httpHeaders: MultivaluedMap<String, Any>,
        entityStream: OutputStream
    ) {
        JsonMessageBodyWriterUtil.setContentTypeIfNecessary(httpHeaders)
        if (o is String) { // YUK: done in order to avoid adding extra quotes...
            entityStream.write(o.toByteArray(StandardCharsets.UTF_8))
        } else {
            json.encodeToStream(serializer(genericType), o, entityStream)
        }
    }

    override fun writeResponse(o: Any, genericType: Type, context: ServerRequestContext) {
        val originalStream = context.orCreateOutputStream
        val stream: OutputStream = NoopCloseAndFlushOutputStream(originalStream)

        if (o is String) { // YUK: done in order to avoid adding extra quotes...
            stream.write(o.toByteArray(StandardCharsets.UTF_8))
        } else {
            json.encodeToStream(serializer(genericType), o, stream)
        }
        // we don't use try-with-resources because that results in writing to the http output
        // without the exception mapping coming into play
        originalStream.close()
    }

    private class NoopCloseAndFlushOutputStream(private val delegate: OutputStream) :
        OutputStream() {
        override fun flush() {}
        override fun close() {}
        override fun write(b: Int) {
            delegate.write(b)
        }

        override fun write(b: ByteArray) {
            delegate.write(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            delegate.write(b, off, len)
        }
    }
}
