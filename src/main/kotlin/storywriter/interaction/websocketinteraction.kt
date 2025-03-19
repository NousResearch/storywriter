package storywriter.interaction

import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.util.UUID

private val json = Json {
	encodeDefaults = false
	prettyPrint = false
	serializersModule = buildSerializersModule()
}

@Serializable
class IDWrapped<R : Response, T : Component<R>>(
	val id: String = UUID.randomUUID().toString(),
	val data: T
)

// TODO: test robustness against deliberately malformed/malicious messages
class WebSocketUserInteraction(private val session: DefaultWebSocketSession) : UserInteraction {
	private val mutex = Mutex()
	private var pendingResponse: CompletableDeferred<String>? = null

	init {
		CoroutineScope(Dispatchers.Default).launch {
			try {
				for (frame in session.incoming) {
					if (frame is Frame.Text) {
						val text = frame.readText().trim()

						if(text == "PONG") {
							// ignore
						} else {
							mutex.withLock {
								pendingResponse?.complete(text)
								pendingResponse = null
							}
						}
					}
				}
			} catch (ex: Exception) {
				mutex.withLock {
					pendingResponse?.completeExceptionally(ex)
				}
			}
		}
	}

	private val displayComponents = mutableListOf<DisplayComponent>()
	override fun addDisplayComponents(vararg components: DisplayComponent) {
		displayComponents.addAll(components)
	}

	override suspend fun pollForRaw(messageType: String, requests: List<RequestComponent<*>>): List<Response?> {
		val deferred = CompletableDeferred<String>()

		mutex.withLock {
			if (pendingResponse != null)
				throw IllegalStateException("Another poll is already waiting for a response")

			pendingResponse = deferred
		}

		val display = displayComponents.map { IDWrapped(data=it) }
		val wrapped = requests.map { IDWrapped(data=it) }
		val merge: List<IDWrapped<out Response, out Component<out Response>>> = display+wrapped

		displayComponents.clear()
		session.send(Frame.Text(buildString {
			append("$messageType|")
			append(json.encodeToString(merge))
		}))

		val responseText = deferred.await()
		val parts = responseText.split('|', limit=2)

		require(parts[0] == "${messageType}_RESPONSE")

		val el = json.parseToJsonElement(parts[1])

		// decode each as an IDWrapped, but we don't know the underlying <T> because we store it in responseSerializer
		// thus we loop through the json element, pull the correct key, then manually deserialize
		// this avoids the frontend having to know the serial names for all the response types
		val convert = el.jsonArray.mapNotNull {
			val obj = it.jsonObject
			val id = obj["id"]!!.jsonPrimitive.content
			id to json.decodeFromString(
				wrapped.first { it.id == id }.data.responseSerializer,
				obj["data"]!!.jsonObject.toString()
			)
		}.toMap()

		return wrapped.map { convert.getOrDefault(it.id, null) }
	}

	override suspend fun updateStatus(status: String) {
		session.send(Frame.Text(buildString {
			append("UPDATESTATUS|")
			append(status)
		}))
	}
}