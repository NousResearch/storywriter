package storywriter.pipelines

import llms.asString
import pipelinedag.PipelineVariables
import pipelinedag.extractXMLBlock
import pipelinedag.simplePipeline
import storywriter.interaction.UserInteraction
import storywriter.Templates
import storywriter.interaction.promptText

class VTCVars : PipelineVariables() {
	val user by type<UserInteraction>()

	val prompt by string()
	val vignette by string()

	val storystruct by string()
	val outline by string()
	val firstpass by string()
	val feedback by string()

	val output by string()

	override val inputs = inputs {
		option {
			ifMissingAnyOf(prompt, vignette) {
				required(user)
			}
		}
	}

	override val outputs = outputs {
		option(output)
	}
}

internal val vignette_to_chapter = simplePipeline<VTCVars>("vignette_to_chapter") {
	retryPolicy(defaultRetryPolicy)

	step("Get user prompt") {
		consumes(vars.user)
		produces(vars.prompt)
		execute {
			set(vars.vignette, vars.user.value().promptText())
		}
	}

	step("Get user vignette") {
		consumes(vars.user)
		produces(vars.vignette)
		execute {
			set(vars.vignette, vars.user.value().promptText("vignette"))
		}
	}

	step("Generate story struct") {
		consumes(vars.prompt, vars.vignette)
		produces(vars.storystruct)
		execute {
			val data = Templates.storystruct {
				variable("USER_STORY_PROMPT", vars.prompt.value())
				variable("USER_VIGNETTE", vars.vignette.value())
			}.asString()

			this.set(vars.storystruct, data)
		}
	}

	step("Outline story") {
		consumes(vars.storystruct)
		produces(vars.outline)
		execute {
			val data = Templates.outline {
				variable("STORY_STRUCT", vars.storystruct.value())
			}.asString()

//			val result = extractXMLBlock(data, "outline")
//				?: error("failed to find outline")

			this.set(vars.outline, data)
		}
	}

	step("Generate first pass") {
		consumes(vars.prompt, vars.vignette, vars.outline)
		produces(vars.output)
		execute {
			val data = Templates.firstpass {
				variable("USER_STORY_PROMPT", vars.prompt.value())
				variable("USER_VIGNETTE", vars.vignette.value())
				variable("PLANNING", vars.outline.value())
			}.asString()

			this.set(vars.output, data)
		}
	}

//	step("Feedback") {
//		consumes(vars.prompt, vars.vignette, vars.firstpass)
//		produces(vars.feedback)
//		execute {
//			val data = Templates.story_feedback {
//				variable("USER_STORY_PROMPT", vars.prompt.value())
//				variable("USER_VIGNETTE", vars.vignette.value())
//				variable("STORY", vars.firstpass.value())
//			}.asString()
//
//			this.set(vars.feedback, data)
//		}
//	}
//
//	step("Rewrite") {
//		consumes(vars.prompt, vars.vignette, vars.firstpass, vars.feedback)
//		produces(vars.output)
//		execute {
//			val data = Templates.rewrite {
//				variable("USER_STORY_PROMPT", vars.prompt.value())
//				variable("USER_VIGNETTE", vars.vignette.value())
//				variable("FEEDBACK", vars.feedback.value())
//				variable("STORY", vars.firstpass.value())
//			}.asString()
//
//			this.set(vars.output, data)
//		}
//	}
}