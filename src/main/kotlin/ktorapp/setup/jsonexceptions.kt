package ktorapp.setup

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.response.respond
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException

// JsonDecodeException is marked internal so we can't read it off properties
fun extractJsonFail(exceptionMessage: String): String {
	val regex = Regex("Unexpected JSON token at offset \\d+")
	return regex.find(exceptionMessage)?.value ?: "unknown error"
}

@OptIn(ExperimentalSerializationApi::class)
fun StatusPagesConfig.setupSerializationExceptions() {
	exception<BadRequestException> { call, cause ->
		if(cause.cause !is JsonConvertException)
			return@exception

		val error = when (val actualCause = (cause.cause as JsonConvertException).cause) {
			is MissingFieldException -> {
				if(actualCause.missingFields.size > 1) {
					"Missing required fields: ${actualCause.missingFields.joinToString(", ")}"
				} else {
					"Missing required field: ${actualCause.missingFields.firstOrNull()}"
				}
			}
			is SerializationException -> "Serialization error: ${extractJsonFail(actualCause.message ?: "")}"
			// these are triggered by require(...) refinements in init {} constructors of serializable types
			is IllegalArgumentException -> "${actualCause.message}"
			else -> "Serialization error: ${actualCause?.message ?: "unknown error"}"
		}
		call.respond(
			HttpStatusCode.BadRequest,
			mapOf("error" to error)
		)
	}
}