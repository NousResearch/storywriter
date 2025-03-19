package storywriter.pipelines

import io.ktor.util.toLowerCasePreservingASCIIRules
import llms.RetryPolicy
import java.time.Duration

// TODO: really need to namespace these exceptions or something
internal val defaultRetryPolicy = RetryPolicy(
	initialDelay = Duration.ofSeconds(1),
	shouldRetryForException = { rp, state, ex ->
		ex !is ExceptionInInitializerError
	}
)
//private val clean_regex = Regex("^(?i)(?:markdown|theme:.*?|title:.*?)(?:\\r?\\n)")

internal fun validateVignettes(vignettes: List<String>): List<String> {
	val vignettes = vignettes.map {
		it.split(Regex("\r?\n")).last().trim()
	}

	require(vignettes.size == 3)
	{ "got ${vignettes.size} instead of 3: $vignettes" }

	require(vignettes.all { it.length in 250 .. 1100 })
	{ "got responses of unacceptable length (${vignettes.joinToString(", ") {it.length.toString()}})" }

	// "The winner of the 2023 Transcend Short Vignette Contest is "The Bargain" by Kaelin Voss. Kaelin's story of a mortal..."
	require(vignettes.all { "transcend short" !in it.toLowerCasePreservingASCIIRules() })
	{ "LLM regurgitated our prompt back to us" }

	// yep it really does this sometimes
	require(vignettes.toSet().size == vignettes.size)
	{ "LLM regurgitated the same vignette more than once" }

	return vignettes
}