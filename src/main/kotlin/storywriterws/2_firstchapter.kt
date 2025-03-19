package storywriterws

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import pipelinedag.IPipelineContext
import pipelinedag.PipelineContextSourceTracked
import storywriter.interaction.RequestComponent
import storywriter.interaction.Response
import storywriter.pipelines.StorywriterPipelines

@Serializable
@SerialName("Chapter")
data class Chapter(
	val chapter: String,
	val actions: List<String>
) : RequestComponent<ChapterResponse> {
	override val responseSerializer: KSerializer<ChapterResponse> = serializer<ChapterResponse>()
}

@Serializable
data class ChapterResponse(val action: String) : Response

// TODO: localizations on frontend
enum class ChapterAction(val key: String) {
	REGENERATE("Regenerate similar chapter"),
	VARIATION("Same theme, different story");
//	VARIATION("Variation");

	companion object {
		fun fromKey(key: String): ChapterAction? {
			return entries.firstOrNull { it.key == key }
		}
	}
}

suspend fun RunContext.firstChapter(prompt: String, vignette: String) {
	var reuse_ctx: IPipelineContext? = null

	while(true) {
		val result = StorywriterPipelines.ChapterGen.firstchapter
			.prepare(reuse_ctx)
			.context {
				ctx.set(vars.prompt, prompt)
				ctx.set(vars.vignette, vignette)
			}
			.executeAndSave(runId)
			.tracked()

		val poll = input.pollForSingle(
			Chapter(
				result.ctx.get(result.vars.output),
				ChapterAction.entries.map { it.key }
			)
		)
		val action = ChapterAction.fromKey(poll.action)

		when(action) {
			ChapterAction.REGENERATE -> {
				val copy = PipelineContextSourceTracked.from(result.ctx.copy())
				copy.remove(result.vars.output)
				reuse_ctx = copy
			}
			ChapterAction.VARIATION -> {
				reuse_ctx = null
				continue
			}
			else -> {
				TODO("unhandled chapteraction key")
			}
		}
	}
}
