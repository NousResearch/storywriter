package storywriter.scripts

import storywriter.interaction.TerminalInput
import storywriter.pipelines.StorywriterPipelines
import kotlin.collections.withIndex
import kotlin.io.print
import kotlin.io.println
import kotlin.io.readln
import kotlin.require
import kotlin.text.toIntOrNull

suspend fun main() {
	val input = TerminalInput()

	var (prompt, vignettes) = StorywriterPipelines.Vignette.initial
		.prepare()
		.context {
			ctx.set(vars.user, input)
		}
		.execute()
		.multi {
			collect(
				get { vars.prompt },
				get { vars.output }
			)
		}

	while(true) {
		for ((i, vignette) in vignettes.withIndex()) {
			println("# Vignette ${i+1}")
			println(vignette)
			println("------")
		}

		print("Variation on prompt number: ")
		val vignetteIndex = (readln().toIntOrNull() ?: continue) - 1
		require(vignetteIndex in 0 .. 2)

		vignettes = StorywriterPipelines.Vignette.variator
			.prepare()
			.context {
				ctx.set(vars.prompt, prompt)
				ctx.set(vars.vignette, vignettes[vignetteIndex])
			}
			.execute()
			.get { vars.output }
	}
}