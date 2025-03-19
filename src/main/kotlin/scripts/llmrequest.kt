package scripts

import llms.*
import llms.configs.ModelRegistry

suspend fun main() {
	val model = ModelRegistry.get("claude3-opus")!!

	val chat = listOf(
		Message("hi what's up"),
		Message("the sky???", MessageRole.Assistant),
	)

	val inferenceParams = InferenceParameters(
		logProbs=true,
		systemPrompt=listOf(TextBlock("whatever", cacheHint=CacheHint.Cache))
	)

	val response = model.multiTurn(chat, inferenceParams) {
		logProbs(InferenceFlag.Preferred)
	}

	response.collect { result ->
		when (result) {
			is LogProbCompletion -> {
				result.tokens.forEach { token ->
					val probability = "%.2f".format(token.selected.logprob)
					val tokenText = token.selected.token
					val options = token.options.joinToString(", ") { it.token.replace("\n", "\\n") }
					println("$probability ${tokenText.padEnd(20)} [$options]")
				}
			}
			is TextCompletion -> println(result.content)
		}
	}

}

