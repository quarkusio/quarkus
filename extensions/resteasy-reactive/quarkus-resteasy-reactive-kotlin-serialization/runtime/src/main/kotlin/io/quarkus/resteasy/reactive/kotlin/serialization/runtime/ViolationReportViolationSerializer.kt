package io.quarkus.resteasy.reactive.kotlin.serialization.runtime

import io.quarkus.hibernate.validator.runtime.jaxrs.ViolationReport
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.*

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = ViolationReport.Violation::class)
object ViolationReportViolationSerializer : KSerializer<ViolationReport.Violation> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor(
            "io.quarkus.hibernate.validator.runtime.jaxrs.ViolationReport.Violation"
        ) {
            element("field", serialDescriptor<String>())
            element("message", serialDescriptor<String>())
        }

    override fun deserialize(decoder: Decoder): ViolationReport.Violation {
        return decoder.decodeStructure(descriptor) {
            var field: String? = null
            var message: String? = null

            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> field = decodeStringElement(descriptor, 0)
                    1 -> message = decodeStringElement(descriptor, 1)
                    else -> throw SerializationException("Unexpected index $index")
                }
            }

            ViolationReport.Violation(
                requireNotNull(field),
                requireNotNull(message),
            )
        }
    }

    override fun serialize(encoder: Encoder, value: ViolationReport.Violation) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.field)
            encodeStringElement(descriptor, 1, value.message)
        }
    }
}
