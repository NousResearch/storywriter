package scripts

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ktorapp.BadUserInputException
import llms.*
import ktorapp.route.MultiTurnRequest
import ktorapp.route.multiTurnRoutes
import ktorapp.setup.setupSerializationExceptions

suspend fun main() {
	val params = InferenceParameters(logProbs=true, systemPrompt=listOf(TextBlock("whatever", cacheHint=CacheHint.Cache)))
	val mtr = MultiTurnRequest(
		"gpt4o-mini",
		listOf(
			Message("hi what's up"),
			Message("Hello! As an AI assistant, I don't actually experience emotions or have a personal state, " +
					"but I'm here and ready to help you with any questions or tasks you may have. How can I assist " +
					"you today?", MessageRole.Assistant),
			Message("are you sure?"),
		),
		params,
		params.asIC {
			logProbs(InferenceFlag.Preferred)
		}
	)

	println(Json.encodeToString(mtr))

	embeddedServer(Netty, port = 8080) {
		install(ContentNegotiation) {
			json()
		}

		install(StatusPages) {
			setupSerializationExceptions()
			exception<BadUserInputException> { call, cause ->
				call.respond(HttpStatusCode.BadRequest, mapOf("error" to cause.message))
			}
			exception<Throwable> { call, cause ->
				println(cause)
				// TODO: log
				call.respond(
					HttpStatusCode.InternalServerError,
					mapOf("error" to "Unknown error")
				)
			}
		}

		routing {
			multiTurnRoutes()
		}
	}.start(wait = true)
}