package ktorapp.route

import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import llms.InferenceControl
import llms.InferenceParameters
import llms.Message
import llms.configs.ModelRegistry
import io.ktor.server.response.*
import ktorapp.BadUserInputException

@Serializable
data class MultiTurnRequest(
    val model: String,
    val messages: List<Message>,
    val params: InferenceParameters,
    val ic: InferenceControl
)

fun Route.multiTurnRoutes() {
    post("/multiturn") {
        val request = call.receive<MultiTurnRequest>()

        val model = ModelRegistry.get(request.model)
            ?: throw BadUserInputException("Invalid model specified: ${request.model}")

        val result = model.multiTurn(
            messages = request.messages,
            params = request.params,
            ic = request.ic
        )
        call.respond(result)
    }
}