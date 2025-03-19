@file:OptIn(ExperimentalSerializationApi::class)

import io.ktor.client.plugins.logging.LogLevel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

@Serializer(forClass=URL::class)
object URLSerializer : KSerializer<URL> {
	override fun serialize(encoder: Encoder, value: URL) {
		encoder.encodeString(value.toString())
	}

	override fun deserialize(decoder: Decoder): URL {
		val url = decoder.decodeString()
		return try {
			URI.create(url).toURL()
		} catch (ex: URISyntaxException) {
			throw SerializationException("Invalid URL: $url")
		} catch (ex: MalformedURLException) {
			throw SerializationException("Invalid URL: $url")
		}
	}
}

@Serializer(forClass = LogLevel::class)
object LogLevelSerializer : KSerializer<LogLevel> {
	override fun serialize(encoder: Encoder, value: LogLevel) {
		encoder.encodeString(value.name)
	}

	override fun deserialize(decoder: Decoder): LogLevel {
		val name = decoder.decodeString()
		return LogLevel.valueOf(name)
	}
}