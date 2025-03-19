package storywriter

import llms.*
import llms.configs.ModelRegistry
import pipelinedag.PipelineBuilder
import pipelinedag.PipelineVariables
import pipelinedag.pipeline
import pipelinedag.extractCodeBlocks

suspend fun main() {
	val completion = ModelRegistry.get("claude-sonnet")

	class Variables : PipelineVariables() {
		val vignettes by list<String>()
		val bestPick by string()
		val output by string()

		override val inputs = inputs {
			option(vignettes)
		}

		override val outputs = outputs {
			option(output)
		}
	}
	val vars = Variables()

	fun PipelineBuilder.chooseRandomly() {
		step("Choose vignette randomly") {
			consumes(vars.vignettes)
			produces(vars.bestPick)
			execute {
				this.set(vars.bestPick, vars.vignettes.value().random())
			}
		}
	}

	fun PipelineBuilder.chooseBest() {
		step("Choose the best vignette") {
			consumes(vars.vignettes)
			produces(vars.bestPick)
			execute {
				val ret = extractCodeBlocks(completion.templateString("best_pick") {
					for ((i, vignette) in vars.vignettes.value().withIndex()) {
						variable("VIGNETTE$i", vignette)
					}
				})

				require(ret.size == 1)
				val chosen = ret[0].toIntOrNull() ?: error("got bad response from LLM during pick_best:\n\n $ret")
				require(chosen in 1 .. 5)

				this.set(vars.bestPick, vars.vignettes.value()[chosen-1])
			}
		}
	}

	val p = pipeline("pipeline_test") {
		step("Create initial vignettes") {
			produces(vars.vignettes)
			execute {
				val ret = extractCodeBlocks(completion.templateString("five_vignettes"))
				require(ret.size == 5)
				this.set(vars.vignettes, ret)
			}
		}

		chooseRandomly()
//		chooseBest()

		step("Convert to Shakespeare") {
			consumes(vars.bestPick)
			produces(vars.output)
			execute {
				val ret = extractCodeBlocks(
					completion.templateString("shakespeare") {
						variable("VIGNETTE", vars.bestPick.value())
					}
				)

				require(ret.size == 1)
				this.set(vars.output, ret[0])
			}
		}
	}

	val ctx = p.execute()
	println(ctx.get(vars.output))
	println("Finished")
}