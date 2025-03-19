package storywriter

import kotlinx.serialization.json.Json
import pipelinedag.FilesystemDB
import pipelinedag.PipelineVariables
import pipelinedag.SerializedPipelineVariable
import pipelinedag.deserializer
import kotlin.io.path.Path

private val json = Json {
	encodeDefaults=false
	prettyPrint=false
}

private class MockVars : PipelineVariables() {
	val string by list<String>()

	override val inputs = inputs {}
	override val outputs = outputs {}
}

fun countPromptLength(db: FilesystemDB) {
	var sum = 0
	var count = 0
	var largest = 0
	var smallest = 0

	db.historicalMap.descendingMap()?.forEach { key, value ->
		val raw: SerializedPipelineVariable = json.decodeFromString(value)

		val varkey = MockVars().string
		// TODO: make an interface for scanning/requesting specific variables, batches, etc?
		if(raw.key == "output") {

			val raw = varkey.deserializer().invoke(raw.value, json)
			val cast = varkey.castValue(raw)

			cast?.forEach {
				println(it)
				println()

				sum += it.length
				count += 1

				if(it.length > largest)
					largest = it.length

				if(smallest == 0 || (it.length < smallest && it.isNotEmpty()))
					smallest = it.length
			}
		}
	}

	println("avg: ${sum / count}, largest: $largest, smallest: $smallest")
}

fun printWholeDB(db: FilesystemDB) {
	db.historicalMap.descendingMap()?.forEach { key, value ->
		println("Key: ${key}, Value: ${value}")
//		readln()
	}
}

fun main() {
	val db = FilesystemDB.fromPath(Path("output", "websocket.db"))

//	countPromptLength(db)
	printWholeDB(db)

	db.close()
}