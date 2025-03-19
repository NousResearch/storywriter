package storywriter.interaction

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.EmptySerializersModule

private inline fun <T> promptValue(prompt: String, converter: (String) -> T): T {
	while (true) {
		print(prompt)
		val input = readln()
		try {
			return converter(input)
		} catch (e: Exception) {
			println("Invalid input. Please try again.")
		}
	}
}

@OptIn(ExperimentalSerializationApi::class)
class TerminalDecoder : Decoder {
	override val serializersModule = EmptySerializersModule()

	override fun decodeBoolean(): Boolean = promptValue("Enter boolean: ") { it.toBoolean() }
	override fun decodeByte(): Byte = promptValue("Enter byte: ") { it.toByte() }
	override fun decodeChar(): Char = promptValue("Enter char: ") { it.first() }
	override fun decodeDouble(): Double = promptValue("Enter double: ") { it.toDouble() }
	override fun decodeFloat(): Float = promptValue("Enter float: ") { it.toFloat() }
	override fun decodeInt(): Int = promptValue("Enter int: ") { it.toInt() }
	override fun decodeLong(): Long = promptValue("Enter long: ") { it.toLong() }
	override fun decodeShort(): Short = promptValue("Enter short: ") { it.toShort() }
	override fun decodeString(): String = promptValue("Enter string: ") { it }
	override fun decodeNotNullMark(): Boolean {
		print("Is the next value not null? (y/n): ")
		return readln().trim().lowercase() in listOf("y", "yes")
	}

	override fun decodeNull(): Nothing? {
		println("Decoding null value.")
		return null
	}

	override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
		println("Select one of the following enum options:")
		for (i in 0 until enumDescriptor.elementsCount) {
			println("$i: ${enumDescriptor.getElementName(i)}")
		}
		return promptValue("Enter the number corresponding to your choice: ") { it.toInt() }
	}

	override fun decodeInline(descriptor: SerialDescriptor): Decoder = TerminalDecoder()

	override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
		TerminalCompositeDecoder(descriptor)
}

@OptIn(ExperimentalSerializationApi::class)
class TerminalCompositeDecoder(private val descriptor: SerialDescriptor) : CompositeDecoder {
	override val serializersModule = EmptySerializersModule()
	private var index = 0

	override fun decodeElementIndex(descriptor: SerialDescriptor): Int =
		if (index < descriptor.elementsCount) index++ else CompositeDecoder.DECODE_DONE

	override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String =
		promptValue("Enter value for '${descriptor.getElementName(index)}' (string): ") { it }
	override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int =
		promptValue("Enter value for '${descriptor.getElementName(index)}' (int): ") { it.toInt() }
	override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean =
		promptValue("Enter value for '${descriptor.getElementName(index)}' (boolean): ") { it.toBoolean() }
	override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double =
		promptValue("Enter value for '${descriptor.getElementName(index)}' (double): ") { it.toDouble() }
	override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float =
		promptValue("Enter value for '${descriptor.getElementName(index)}' (float): ") { it.toFloat() }
	override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long =
		promptValue("Enter value for '${descriptor.getElementName(index)}' (long): ") { it.toLong() }
	override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short =
		promptValue("Enter value for '${descriptor.getElementName(index)}' (short): ") { it.toShort() }
	override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte =
		promptValue("Enter value for '${descriptor.getElementName(index)}' (byte): ") { it.toByte() }
	override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char =
		promptValue("Enter value for '${descriptor.getElementName(index)}' (char): ") { it.first() }

	override fun <T> decodeSerializableElement(
		descriptor: SerialDescriptor,
		index: Int,
		deserializer: DeserializationStrategy<T>,
		previousValue: T?
	): T {
		println("Decoding complex element '${descriptor.getElementName(index)}':")
		return deserializer.deserialize(TerminalDecoder())
	}

	override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder {
		print("Enter inline value for '${descriptor.getElementName(index)}': ")
		return TerminalDecoder()
	}

	// TODO: this kinda sucks, fix it
	override fun <T : Any> decodeNullableSerializableElement(
		descriptor: SerialDescriptor,
		index: Int,
		deserializer: DeserializationStrategy<T?>,
		previousValue: T?
	): T? {
		print("Enter value for '${descriptor.getElementName(index)}' (nullable, leave blank for null): ")
		val input = readln()
		return if (input.isBlank()) {
			null
		} else {
			deserializer.deserialize(TerminalDecoder())
		}
	}

	override fun endStructure(descriptor: SerialDescriptor) { /* no-op */ }
}

@Serializable
private data class Person(val name: String, val age: Int, val nickname: String? = null)

fun main() {
	println("Please enter values for a Person:")
	val person = Person.serializer().deserialize(TerminalDecoder())
	println("You entered: $person")
}

class TerminalInput : UserInteraction {
	override fun addDisplayComponents(vararg components: DisplayComponent) {
		TODO("Not yet implemented")
	}
	//	override suspend fun <Req, Result : Any> pollFor(
//		request: Req,
//		request_serializer: KSerializer<Req>,
//		result_serializer: KSerializer<Result>
//	): Result {
//		println(request)
//		return result_serializer.deserialize(TerminalDecoder())
//	}

	override suspend fun pollForRaw(
		messageType: String,
		requests: List<RequestComponent<*>>
	): List<Response> {
		TODO("Not yet implemented")
	}

	override suspend fun updateStatus(status: String) {
		TODO("Not yet implemented")
	}
}