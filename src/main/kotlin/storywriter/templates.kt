package storywriter

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import llms.*
import llms.configs.ModelRegistry
import storywriter.Models.Completions.deepseek_r1
import storywriter.Models.Instruct.claude3_opus
import kotlin.coroutines.coroutineContext
import kotlin.io.path.Path

// could in principle mock these later for testing
object Models {
	object Completions {
		val deepseek_r1 = ModelRegistry.get("deepseek-r1-completion")
	}

	object Instruct {
//		val deepseek_r1 = ModelRegistry.get("deepseek-r1")
		val claude3_opus = ModelRegistry.get("claude3-opus")
	}
}

typealias BoundTemplate = suspend (block: (PromptTemplateBuilder.() -> Unit)?) -> Flow<Completion>
private fun ChatModelAPI.forTemplate(first: String, vararg rest: String): BoundTemplate {
	val name =
		if(rest.isEmpty())
			Path(first)
		else
			Path(first, *rest.toList().toTypedArray())
	suspend fun wrappedTemplateCall(block: (PromptTemplateBuilder.() -> Unit)? = null): Flow<Completion> {
		val exc = coroutineContext[ExecutionContext]
		val pd = promptTemplate(name.toString(), block)
		var first = false

		return if(exc == null)
			this.templateStream(pd)
		else
			this.templateStream(pd)
				.map {
					val count = when(it) {
						is LogProbCompletion -> it.content.length
						is TextCompletion -> it.content.length
					}

					// prepended tokens don't count
					if(pd.prepend == null || first) {
						exc.addTokenCount(count)
					} else {
						first = true
					}

					it
				}
	}
	return ::wrappedTemplateCall
}

object Templates {
	val random_theme = claude3_opus.forTemplate("instruct", "random_theme")
	val theme_extractor = claude3_opus.forTemplate("instruct", "theme_extraction")

	val vignette = deepseek_r1.forTemplate("r1_completion", "1_vignette")
	val vignette_variator = deepseek_r1.forTemplate("r1_completion", "2_vignette_variator")
//	val storystruct = llama_405b_base.forTemplate("base", "3_storystruct")
//	val outline = claude3_opus.forTemplate("instruct", "4_outline")
	val storystruct = deepseek_r1.forTemplate("base", "3_storystruct")
	// TODO: deepseek <think> </think> support
	val outline = deepseek_r1.forTemplate("r1_completion", "4_outline")
//	val outline = hermes70b.forTemplate("hermes", "4_outline")
	val firstpass = deepseek_r1.forTemplate("r1_completion", "5_generate_firstpass")
	val story_feedback = claude3_opus.forTemplate("instruct", "6_feedback")
	val rewrite = deepseek_r1.forTemplate("base", "7_rewrite")
}