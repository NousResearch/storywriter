package storywriter.pipelines

import llms.asString
import pipelinedag.PipelineVariables
import pipelinedag.simplePipeline
import pipelinedag.extractCodeBlocks
import storywriter.interaction.UserInteraction
import storywriter.Templates
import storywriter.interaction.promptText

class VariationVariables : PipelineVariables() {
	val user by type<UserInteraction>()
	val prompt by string()
	val vignette by string()
	val output by list<String>()

	override val inputs = inputs {
		option {
			ifMissingAnyOf(user, prompt) {
				required(user)
			}
		}
	}

	override val outputs = outputs {
		option(output)
	}
}

internal val variator = simplePipeline<VariationVariables>("vignette_variation") {
	retryPolicy(defaultRetryPolicy)

	step("Get user vignette") {
		consumes(vars.user)
		produces(vars.vignette)
		execute {
			set(vars.vignette, vars.user.value().promptText("vignette"))
		}
	}

	step("Variation on vignette") {
		consumes(vars.prompt)
		consumes(vars.vignette)
		produces(vars.output)
		execute {
			val data = Templates.vignette_variator {
				variable("USER_STORY_PROMPT", vars.prompt.value())
				variable("USER_VIGNETTE", vars.vignette.value())
			}.asString()
			val ret = validateVignettes(extractCodeBlocks(data))

			this.set(vars.output, ret)
		}
	}
}