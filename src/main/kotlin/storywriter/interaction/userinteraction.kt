package storywriter.interaction

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

interface Component<R : Response> {
	val expectsReply: Boolean
	val responseSerializer: KSerializer<R>
}

interface Response
// nonce object for type satisfaction
object NoResponse : Response

interface RequestComponent<T : Response> : Component<T> {
	override val expectsReply: Boolean
		get() = true
}
interface DisplayComponent : Component<NoResponse> {
	override val expectsReply: Boolean
		get() = false
	override val responseSerializer: KSerializer<NoResponse>
		get() = throw Exception("display components should never be trying to deserialize responses!")
}

interface UserInteraction {
	// TODO: ill considered, we should have some concept of a "frame" where we can have
	//       <input component> <display component> <input component>
	//       but that doesn't fit with the pollFor syntax of our extension methods
	//       probably make a DSL where we define the components in order (which get collected)
	//       and then at the end we specify our expectations (much like the pipeline DSL)
	//       something like:
	/*
user.request {
	// input component
	val as = push(ActionSelect(listOf("go back", "reset")))

	// display component
	push(VignetteDisplay(vignettes))

	val vc = push(VignetteChoice(vignettes.size))

	// may be null? if this is an end state with no further options?
	// need to consider if that makes sense
	val response = response {
		oneOf(as, vc)
		// could also be allOf(as, vc)
	}

	// could also have been the following, if only one request component
	val response = single(as)
}
	 */
	fun addDisplayComponents(vararg components: DisplayComponent)

	/**
	 * guaranteed to return 1:1 response or null for each request slot
	 */
	suspend fun pollForRaw(messageType: String, requests: List<RequestComponent<*>>): List<Response?>

	suspend fun <T : Response> pollForSingle(request: RequestComponent<T>): T {
		// we trust the remote end won't mess up
		@Suppress("UNCHECKED_CAST")
		return pollForRaw("ALL_OF", listOf(request)).first() as T
	}

	suspend fun updateStatus(status: String)
}