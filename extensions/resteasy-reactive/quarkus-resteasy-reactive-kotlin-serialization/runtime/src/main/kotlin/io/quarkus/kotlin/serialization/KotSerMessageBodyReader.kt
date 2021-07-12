package io.quarkus.kotlin.serialization

import io.quarkus.resteasy.reactive.server.runtime.StreamUtil
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.jboss.resteasy.reactive.common.providers.serialisers.AbstractJsonMessageBodyReader
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext
import java.io.InputStream
import java.lang.reflect.Type
import javax.inject.Inject
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap

class KotSerMessageBodyReader(@Inject var json: Json) : AbstractJsonMessageBodyReader(), ServerMessageBodyReader<Any> {
    override fun isReadable(type: Class<*>, genericType: Type, annotations: Array<Annotation>, mediaType: MediaType) =
        isReadable(mediaType, type)

    override fun isReadable(type: Class<*>, genericType: Type, lazyMethod: ResteasyReactiveResourceInfo, mediaType: MediaType) =
        isReadable(mediaType, type)

    override fun readFrom(
        type: Class<Any>,
        genericType: Type,
        annotations: Array<out Annotation>,
        mediaType: MediaType,
        httpHeaders: MultivaluedMap<String, String>,
        entityStream: InputStream
    ): Any? {
        return doReadFrom(type, entityStream)
    }

    override fun readFrom(type: Class<Any>, genericType: Type, mediaType: MediaType, context: ServerRequestContext): Any? {
        return doReadFrom(type, context.inputStream)
    }

    @ExperimentalSerializationApi
    private fun doReadFrom(type: Class<Any>, entityStream: InputStream): Any? {
        return if (StreamUtil.isEmpty(entityStream)) {
            null
        } else {
            json.decodeFromString(serializer(type), entityStream.readAllBytes().decodeToString())
        }
    }
}