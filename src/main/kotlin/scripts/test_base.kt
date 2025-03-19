package scripts

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import llms.*
import llms.configs.ModelRegistry
import llms.configs.YamlModelConfig
import java.io.File

private val CONFIG_FOLDER = File("config")
private val AUTH_CONFIG = File(CONFIG_FOLDER, "auth.yaml")
private val MODELS_CONFIG = File(CONFIG_FOLDER, "models.yaml")

suspend fun main() {
	ModelRegistry.setProvider(YamlModelConfig.loadFrom(AUTH_CONFIG, MODELS_CONFIG))

	val model = ModelRegistry.get("llama-3.1-405b-base")!!

	val resp = model.multiTurn(
		listOf(
			Message("# The Story"),
		),
		InferenceParameters(stopSequences=listOf("<|end_of_text|>"))
	).toList()

	val text = resp.joinToString {
		when(it) {
			is TextCompletion -> it.content
			else -> throw NotImplementedError()
		}
	}

	println(text)
}