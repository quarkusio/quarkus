package io.quarkus.rest.client.reactive.kotlin.runtime.serializers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer
import java.io.OutputStream
import java.lang.reflect.Type
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.ext.MessageBodyWriter

@OptIn(ExperimentalSerializationApi::class)
class ClientKotlinMessageBodyWriter(private val json: Json) : MessageBodyWriter<Any> {
    override fun isWriteable(type: Class<*>, genericType: Type, annotations: Array<out Annotation>?, mediaType: MediaType?) = true

    override fun writeTo(
        t: Any,
        type: Class<*>,
        genericType: Type,
        annotations: Array<out Annotation>?,
        mediaType: MediaType,
        httpHeaders: MultivaluedMap<String, Any>,
        entityStream: OutputStream
    ) {
        json.encodeToStream(serializer(genericType), t, entityStream)
    }
}
