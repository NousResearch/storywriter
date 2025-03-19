package storywriterws

import kotlinx.coroutines.withContext
import pipelinedag.PipelineContextSerializer
import pipelinedag.PipelinePreExecutionBuilder
import pipelinedag.PipelinePreExecutionBuilder.PipelinePostExecutionBuilder
import pipelinedag.PipelineVariables
import pipelinedag.executeAndSave
import storywriter.ExecutionContext
import storywriter.interaction.WebSocketUserInteraction

data class RunContext(
	val runId: String,
	val input: WebSocketUserInteraction,
	val db: PipelineContextSerializer,
) {
	suspend fun <T : PipelineVariables> PipelinePreExecutionBuilder<T>.executeAndSave(
		runId: String,
	): PipelinePostExecutionBuilder<T> = this.executeAndSave(runId, db)

	suspend fun runWithContext(block: suspend RunContext.() -> Unit) {
		withContext(ExecutionContext(runId)) {
			this@RunContext.block()
		}
	}
}