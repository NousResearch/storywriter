package storywriterws

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import storywriter.interaction.RequestComponent
import storywriter.interaction.Response
import storywriter.pipelines.StorywriterPipelines
import storywriter.pipelines.vignette_to_theme

@Serializable
@SerialName("ChooseVignette")
data class ChooseVignette(
	val prompt: String? = null,
	val vignettes: List<String>,
	val actions: List<String>
) : RequestComponent<ChooseVignetteResponse> {
	override val responseSerializer: KSerializer<ChooseVignetteResponse> = serializer<ChooseVignetteResponse>()
}

@Serializable
data class ChooseVignetteResponse(val vignette_id: Int, val action: String) : Response

// TODO: make this inherit a sealed class parent, then the main loop can disambiguate by that parent
//       then the whole thing becomes a clean state machine
data class InitialResult(
	val prompt: String,
	val vignette: String,
	val action: VignetteAction,
)

// TODO: localizations on frontend
enum class VignetteAction(val key: String) {
	VARIATION("Variation"),
	FIRST_CHAPTER("Generate first chapter");

	companion object {
		fun fromKey(key: String): VignetteAction? {
			return entries.firstOrNull { it.key == key }
		}
	}
}

sealed class InitialStart {
	object RandomTheme : InitialStart()
	object UserInput : InitialStart()
	data class UserProvidedVignette(val vignette: String) : InitialStart()
}

suspend fun RunContext.initialVignette(startFrom: InitialStart): InitialResult {
	var (prompt, last_three) = run {
		if(startFrom is InitialStart.UserProvidedVignette) {
			val prompt = vignette_to_theme
				.prepare()
				.context {
					ctx.set(vars.user, input)
					ctx.set(vars.vignette, startFrom.vignette)
				}
				.executeAndSave(runId)
				.get { vars.output }

			return@run Pair(prompt, listOf(startFrom.vignette))
		}

		StorywriterPipelines.Vignette.initial
			.prepare()
			.context {
				ctx.set(vars.user, input)
				ctx.set(vars.fromRandom, false)

				when(startFrom) {
					is InitialStart.UserInput -> {}
					is InitialStart.RandomTheme -> ctx.set(vars.fromRandom, true)
					else -> {}
				}
			}
			.executeAndSave(runId)
			.multi {
				collect(
					get { vars.prompt },
					get { vars.output }
				)
			}
	}

	while(true) {
		val result = input.pollForSingle(ChooseVignette(
			prompt,
			last_three,
			VignetteAction.entries.map { it.key }
		))
		val action = VignetteAction.fromKey(result.action)
			// TODO: log, throw, something - frontend is broken or user is malicious
			?: continue
		val selected = last_three[result.vignette_id]

		when(action) {
			VignetteAction.VARIATION -> {
				last_three = StorywriterPipelines.Vignette.variator
					.prepare()
					.context {
						ctx.set(vars.prompt, prompt)
						ctx.set(vars.vignette, selected)
					}
					.executeAndSave(runId)
					.get { vars.output }
			}
			else -> {
				return InitialResult(prompt, selected, action)
			}
		}
	}
}
