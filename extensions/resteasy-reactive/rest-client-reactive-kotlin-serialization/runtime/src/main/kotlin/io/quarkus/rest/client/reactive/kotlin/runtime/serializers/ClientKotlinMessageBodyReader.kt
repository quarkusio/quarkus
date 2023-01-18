package io.quarkus.rest.client.reactive.kotlin.runtime.serializers

import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.ext.MessageBodyReader
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer
import org.jboss.resteasy.reactive.common.util.StreamUtil
import java.io.InputStream
import java.lang.reflect.Type

@OptIn(ExperimentalSerializationApi::class)
class ClientKotlinMessageBodyReader(private val json: Json) : MessageBodyReader<Any> {
    override fun isReadable(type: Class<*>?, generic: Type?, annotations: Array<out Annotation>?, mediaType: MediaType?) = true

    override fun readFrom(
        type: Class<Any>,
        generic: Type,
        annotations: Array<out Annotation>?,
        mediaType: MediaType?,
        httpHeaders: MultivaluedMap<String, String>?,
        entityStream: InputStream
    ): Any? {
        return if (StreamUtil.isEmpty(entityStream)) null else {
            json.decodeFromStream(serializer(generic), entityStream)
        }
    }
}
