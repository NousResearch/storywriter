package storywriter.interaction

import kotlinx.serialization.modules.SerializersModule

import io.github.classgraph.ClassGraph
import kotlinx.serialization.*
import kotlinx.serialization.modules.*
import kotlin.reflect.KClass
import kotlinx.serialization.InternalSerializationApi

@OptIn(InternalSerializationApi::class)
fun buildSerializersModule(): SerializersModule {
	val scanResult = ClassGraph()
		.enableAllInfo()
		.scan()

	val componentClasses = scanResult.getClassesImplementing(Component::class.qualifiedName)
	val responseClasses = scanResult.getClassesImplementing(Response::class.qualifiedName)

	return SerializersModule {
		polymorphic(Component::class) {
			for (classInfo in componentClasses) {
				val clazz = classInfo.loadClass()
				val kclass = clazz.kotlin

				if (!kclass.isAbstract && kclass.annotations.any { it is Serializable }) {
					try {
						val serializer = kclass.serializer()
						@Suppress("UNCHECKED_CAST")
						subclass(kclass as KClass<Component<*>>, serializer as KSerializer<Component<*>>)
					} catch (ex: Exception) {
						println("No serializer found for Component type: ${kclass.qualifiedName}")
					}
				}
			}
		}
		polymorphic(Response::class) {
			for (classInfo in responseClasses) {
				val clazz = classInfo.loadClass()
				val kclass = clazz.kotlin

				if (!kclass.isAbstract && kclass.annotations.any { it is Serializable }) {
					try {
						val serializer = kclass.serializer()
						@Suppress("UNCHECKED_CAST")
						subclass(kclass as KClass<Response>, serializer as KSerializer<Response>)
					} catch (ex: Exception) {
						println("No serializer found for Response type: ${kclass.qualifiedName}")
					}
				}
			}
		}
	}
}