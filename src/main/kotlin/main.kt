import com.github.ajalt.clikt.core.PrintHelpMessage
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pipelinedag.FilesystemDB
import storywriter.StatusTracker
import storywriter.interaction.SelectAction
import storywriter.interaction.WebSocketUserInteraction
import storywriter.interaction.promptText
import storywriterws.*
import java.util.*
import kotlin.io.path.Path
import com.github.ajalt.clikt.core.parse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import llms.configs.ModelRegistry
import kotlin.system.exitProcess

@Serializable
data class WebsocketUpdate(
	val step: Int,
	val totalSteps: Int,
	val tokens: Int,
	val failures: Int? = null,
)

private val json = Json {
	encodeDefaults=false
	prettyPrint=false
}

enum class StartAction(val key: String) {
	PROMPT("Enter a simple thematic prompt"),
	VIGNETTE("Provide my own vignette"),
	RANDOM("Surprise me");

	companion object {
		fun fromKey(key: String): StartAction? {
			return entries.firstOrNull { it.key == key }
		}
	}
}

fun main(_args: Array<String>) {
	// touch this to force immediate config load
	ModelRegistry

	val pn = programName() ?: "storywriter"
	val args = try {
		ServerArgs(pn).apply { parse(_args) }
	} catch(_: PrintHelpMessage) {
		println(ServerArgs(pn).getFormattedHelp())
		exitProcess(0)
	}
	val db = FilesystemDB.fromPath(Path("output", "websocket.db"))

	val server = embeddedServer(
		Netty,
		host=args.address.host,
		port=args.address.port)
	{
		install(WebSockets)
		routing {
			webSocket("/story") {
				val input = WebSocketUserInteraction(this)

				val ctx = RunContext(
					runId=UUID.randomUUID().toString(),
					input=input,
					db=db,
				)

				launch {
					while(true) {
						delay(30000)
						send(Frame.Text("PING"))
					}
				}

				val callback = StatusTracker.registerCallback(ctx.runId) {
					val send = it.executing
						?: return@registerCallback

					input.updateStatus(json.encodeToString(WebsocketUpdate(
						step=send.currentStep?.stepIndex ?: 1,
						totalSteps=send.stepsToComplete,
						tokens=send.currentStep?.tokensUsed ?: 0,
						failures=send.currentStep?.failures,
					)))
				}

				try {
					ctx.runWithContext {
						val action = StartAction.fromKey(
							input.pollForSingle(SelectAction(StartAction.entries.map { it.key })).action
						)!!

						val startFrom = when(action) {
							StartAction.PROMPT -> InitialStart.UserInput
							StartAction.VIGNETTE -> InitialStart.UserProvidedVignette(
								input.promptText("paste your vignette...")
							)
							StartAction.RANDOM -> InitialStart.RandomTheme
						}

						val result = initialVignette(startFrom)

						when (result.action) {
							VignetteAction.FIRST_CHAPTER -> {
								firstChapter(result.prompt, result.vignette)
							}

							else -> throw NotImplementedError("unhandled initialVignette result action: ${result.action.key}")
						}
					}
				} catch(ex: Throwable) {
					send(Frame.Text("ERROR|${ex.message}"))
					throw ex
				} finally {
					callback.cleanup()
				}
			}
		}
	}

	try {
		server.start(wait = true)
	} finally {
		println("Application shutting down, saving database...")
		db.close()
	}
}
