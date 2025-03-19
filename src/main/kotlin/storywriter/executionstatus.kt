package storywriter

import pipelinedag.Pipeline
import kotlin.coroutines.CoroutineContext

data class CurrentPipelineStep(
	val stepIndex: Int,
	val tokensUsed: Int,
	val failures: Int? = null,
)

data class CurrentPipeline(
	val pipeline: Pipeline,
	val stepsToComplete: Int,
	val currentStep: CurrentPipelineStep? = null,
)

data class PipelineExecutionStatus(
	val executing: CurrentPipeline? = null,
)

data class ExecutionContext(val id: String) : CoroutineContext.Element {
	companion object Key : CoroutineContext.Key<ExecutionContext>
	override val key: CoroutineContext.Key<*> get() = Key

	private var status: PipelineExecutionStatus = PipelineExecutionStatus()

	fun getStatus() = status

	suspend fun setStatus(status: PipelineExecutionStatus) {
		this.status = status
		StatusTracker.notifyUpdate(id, status)
	}

	suspend fun addTokenCount(count: Int) {
		val status = getStatus()
		val currentStep = status.executing?.currentStep
			?: return

		setStatus(status.copy(
			executing=status.executing.copy(
				currentStep=currentStep.copy(tokensUsed=currentStep.tokensUsed + count)
			)
		))
	}
}

typealias StatusUpdateCallback = suspend (status: PipelineExecutionStatus) -> Unit

data class StatusCallback(val id: String, val callback: StatusUpdateCallback) {
	fun cleanup() = StatusTracker.removeCallback(id, callback)
}

object StatusTracker {
	private val callbacks = mutableMapOf<String, MutableList<StatusUpdateCallback>>()

	fun registerCallback(id: String, callback: StatusUpdateCallback): StatusCallback {
		callbacks.getOrPut(id) {mutableListOf()}.add(callback)
		return StatusCallback(id, callback)
	}

	fun removeCallback(id: String, callback: StatusUpdateCallback): Boolean =
		callbacks.getOrDefault(id, mutableListOf()).remove(callback)

	suspend fun notifyUpdate(id: String, status: PipelineExecutionStatus) {
		val callbacks = callbacks[id]
			?: return

		for(callback in callbacks) {
			callback(status)
		}
	}
}