package io.quarkus.it.resteasy.reactive.kotlin

import io.quarkus.it.shared.Shared
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.ext.MessageBodyReader
import jakarta.ws.rs.ext.MessageBodyWriter
import jakarta.ws.rs.ext.Provider
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets

@Provider
@Produces("text/plain")
@Consumes("text/plain")
class AppSuppliedProvider : MessageBodyReader<Shared>, MessageBodyWriter<Shared> {

    override fun isReadable(p0: Class<*>?, type: Type?, p2: Array<out Annotation>?, p3: MediaType?): Boolean {
        return Shared::class.java == type
    }

    override fun readFrom(
        p0: Class<Shared>?,
        p1: Type?,
        p2: Array<out Annotation>?,
        p3: MediaType?,
        p4: MultivaluedMap<String, String>?,
        p5: InputStream?
    ): Shared {
        return Shared("app")
    }

    override fun isWriteable(p0: Class<*>?, type: Type?, p2: Array<out Annotation>?, p3: MediaType?): Boolean {
        return Shared::class.java == type
    }

    override fun writeTo(
        shared: Shared?,
        p1: Class<*>?,
        p2: Type?,
        p3: Array<out Annotation>?,
        p4: MediaType?,
        p5: MultivaluedMap<String, Any>?,
        entityStream: OutputStream?
    ) {
        entityStream?.write(
            String.format("{\"message\": \"app+%s\"}", shared!!.message).toByteArray(StandardCharsets.UTF_8)
        )
    }
}
