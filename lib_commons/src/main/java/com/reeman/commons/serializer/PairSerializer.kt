package com.reeman.commons.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

@Serializer(forClass = Pair::class)
object PairSerializer : KSerializer<Pair<String, String>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Pair") {
        element<String>("first")
        element<String>("second")
    }

    override fun serialize(encoder: Encoder, value: Pair<String, String>) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.first)
            encodeStringElement(descriptor, 1, value.second)
        }
    }

    override fun deserialize(decoder: Decoder): Pair<String, String> {
        var first: String? = null
        var second: String? = null

        decoder.decodeStructure(descriptor) {
            for (i in 0 until descriptor.elementsCount) {
                when (i) {
                    0 -> first = decodeStringElement(descriptor, i)
                    1 -> second = decodeStringElement(descriptor, i)
                }
            }
        }

        return Pair(first ?: throw SerializationException("Missing first"), second ?: throw SerializationException("Missing second"))
    }
}
