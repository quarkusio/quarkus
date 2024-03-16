package io.quarkus.resteasy.reactive.kotlin.serialization.runtime

import io.quarkus.hibernate.validator.runtime.jaxrs.ViolationReport
import jakarta.ws.rs.core.Response
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = ViolationReport::class)
object ViolationReportSerializer : KSerializer<ViolationReport> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("io.quarkus.hibernate.validator.runtime.jaxrs.ViolationReport") {
            element("title", serialDescriptor<String>())
            element("status", serialDescriptor<Int>())
            element(
                "violations",
                listSerialDescriptor(ListSerializer(ViolationReportViolationSerializer).descriptor)
            )
        }

    override fun deserialize(decoder: Decoder): ViolationReport {
        return decoder.decodeStructure(descriptor) {
            var title: String? = null
            var status: Int? = null
            var violations: List<ViolationReport.Violation> = emptyList()

            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    DECODE_DONE -> break@loop
                    0 -> title = decodeStringElement(descriptor, 0)
                    1 -> status = decodeIntElement(descriptor, 1)
                    2 ->
                        violations =
                            decodeSerializableElement(
                                descriptor,
                                2,
                                ListSerializer(ViolationReportViolationSerializer)
                            )
                    else -> throw SerializationException("Unexpected index $index")
                }
            }

            ViolationReport(
                requireNotNull(title),
                status?.let { Response.Status.fromStatusCode(it) },
                violations
            )
        }
    }

    override fun serialize(encoder: Encoder, value: ViolationReport) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.title)
            encodeIntElement(descriptor, 1, value.status)
            encodeSerializableElement(
                descriptor,
                2,
                ListSerializer(ViolationReportViolationSerializer),
                value.violations
            )
        }
    }
}
