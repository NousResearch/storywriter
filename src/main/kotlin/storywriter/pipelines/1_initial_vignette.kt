package storywriter.pipelines

import llms.asString
import pipelinedag.PipelineVariables
import pipelinedag.simplePipeline
import pipelinedag.extractCodeBlocks
import storywriter.interaction.UserInteraction
import storywriter.Templates
import storywriter.interaction.SelectAction
import storywriter.interaction.TextPrompt
import storywriter.interaction.promptText

class InitialVariables : PipelineVariables() {
	val user by type<UserInteraction>()
	val fromRandom by boolean(true)
	val prompt by string()
	val output by list<String>()

	override val inputs = inputs {
		option {
			atLeastOneOf(user, fromRandom, prompt)
		}
	}

	override val outputs = outputs {
		option(output)
	}
}

internal val initial = simplePipeline<InitialVariables>("initial_vignette") {
	retryPolicy(defaultRetryPolicy)

	step("Get user prompt") {
		consumes(vars.fromRandom, vars.user)
		produces(vars.prompt)
		execute {
			if(vars.fromRandom.value()) {
				val words = (1 .. 5).map {ENGLISH_WORDS.random()}

				set(vars.prompt, Templates.random_theme {
					variable("WORDS", words.joinToString("\n"))
				}.asString().trim('"'))
			} else {
				set(vars.prompt, vars.user.value().promptText())
			}
		}
	}

	step("Create initial vignettes") {
		consumes(vars.prompt)
		produces(vars.output)
		execute {
			val data = Templates.vignette {
				variable("USER_STORY_PROMPT", vars.prompt.value())
			}.asString()

			val ret = validateVignettes(extractCodeBlocks(data))

			this.set(vars.output, ret)
		}
	}
}

class VignetteToThemeVars : PipelineVariables() {
	val user by type<UserInteraction>()
	val vignette by string()
	val theme_choices by list<String>()
	val output by string()

	override val inputs = inputs {
		option(user, vignette)
	}

	override val outputs = outputs {
		option(output)
	}
}

private val LET_ME_CHOOSE = "<I'll provide my own>"

internal val vignette_to_theme = simplePipeline<VignetteToThemeVars>("vignette_to_theme") {
	retryPolicy(defaultRetryPolicy)

	step("Extract theme") {
		consumes(vars.vignette)
		produces(vars.theme_choices)
		execute {
			val result = Templates.theme_extractor {
				variable("USER_VIGNETTE", vars.vignette.value())
			}.asString()
			val split = extractCodeBlocks(result).map { it.split("\n").last().trim() }
			require(split.size == 5) { "didn't get 5 themes back" }

			this.set(vars.theme_choices, split)
		}
	}

	step("Choose theme") {
		consumes(vars.user, vars.theme_choices)
		produces(vars.output)
		execute {
			vars.user.value().addDisplayComponents(TextPrompt("Which theme best fits your vignette?"))
			var result = vars.user.value().pollForSingle(SelectAction(
				vars.theme_choices.value() + listOf(LET_ME_CHOOSE)
			)).action

			if(result == LET_ME_CHOOSE) {
				result = vars.user.value().promptText("please enter the theme for your story..")
			}

			set(vars.output, result)
		}
	}
}