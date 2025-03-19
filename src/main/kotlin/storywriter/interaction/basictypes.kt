package storywriter.interaction

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@Serializable
@SerialName("GlobalInput")
data class GlobalInput(
	val placeholder: String? = null,
) : RequestComponent<GlobalInputResponse> {
	override val responseSerializer: KSerializer<GlobalInputResponse> = serializer<GlobalInputResponse>()
}

@Serializable
data class GlobalInputResponse(val text: String) : Response

@Serializable
@SerialName("TextPrompt")
data class TextPrompt(val text: String) : DisplayComponent

@Serializable
@SerialName("GlobalNumericInput")
data class GlobalNumericInput(
	val min: Int,
	val max: Int,
	val placeholder: String? = null,
) : RequestComponent<GlobalNumericInputResponse> {
	override val responseSerializer: KSerializer<GlobalNumericInputResponse> = serializer<GlobalNumericInputResponse>()
}

@Serializable
data class GlobalNumericInputResponse(val value: Int) : Response

@Serializable
@SerialName("LargeText")
data class LargeText(
	val text: String,
) : DisplayComponent

@Serializable
@SerialName("ShowVignettes")
data class ShowVignettes(
	val vignettes: List<String>
) : DisplayComponent

@Serializable
@SerialName("SelectAction")
data class SelectAction(
	val actions: List<String>
) : RequestComponent<SelectActionResponse> {
	override val responseSerializer: KSerializer<SelectActionResponse> = serializer<SelectActionResponse>()
}

@Serializable
data class SelectActionResponse(val action: String) : Response

suspend fun UserInteraction.promptText(placeholder: String? = null): String {
	return this.pollForSingle(GlobalInput(placeholder)).text
}

//suspend fun UserInteraction.promptNumeric(placeholder: String? = null): Int {
//	return this.pollForSingle(GlobalNumericInput(placeholder)).value
//}