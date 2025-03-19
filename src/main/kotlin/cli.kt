import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option

data class ServerAddress(val host: String, val port: Int)

fun programName(): String? =System.getProperty("sun.java.command")?.split("\\s+".toRegex())?.get(0)

class ServerArgs(name: String) : CliktCommand(name) {
	val address: ServerAddress by option(
		"--host",
		help = "Server host with optional :port (e.g. 0.0.0.0:8080 or 127.0.0.1). defaults to 127.0.0.1:8080"
	).convert { input ->
		val parts = input.split(":")
		if (parts.size == 1) {
			ServerAddress(parts[0], 8080)
		} else if (parts.size == 2) {
			ServerAddress(parts[0], parts[1].toIntOrNull() ?: 8080)
		} else {
			error("Invalid format. Expected host or host:port")
		}
	}.default(ServerAddress("127.0.0.1", 8080))

	override fun run() {}
}