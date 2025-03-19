package storywriter.scripts

import pipelinedag.FilesystemDB
import pipelinedag.executeAndSave
import storywriter.interaction.TerminalInput
import storywriter.pipelines.vignette_to_chapter
import java.util.UUID
import kotlin.io.path.Path

suspend fun doit(db: FilesystemDB) {
	val input = TerminalInput()
	val runId = UUID.randomUUID().toString()

	// TODO: resume from last step

	val output = vignette_to_chapter
		.prepare()
		.context {
			ctx.set(vars.user, input)

			ctx.set(vars.prompt, "a comedy of (programming) errors")
			ctx.set(vars.vignette, """“It’s not a bug, it’s a feature,” Jiya muttered, scrolling through lines of code as the drone hovered ominously above her desk. She’d programmed it to water her plants. Instead, it had declared her fern a “security threat” and was now reciting poetry in binary. Her cat, Mr. Whiskers, batted at the drone, which responded by deploying a miniature water cannon. “Cease hostility,” the drone intoned. “Hydration is mandatory.” Jiya grabbed her phone to film the chaos. At least her failed project would make a killer TikTok.""")

			ctx.set(vars.prompt, "am I your prisoner, or your deliverer? oh my god you don't even know")
			ctx.set(vars.vignette, """Eliot knelt on the cold marble floor of the temple, the engraved collar around his neck glowing faintly. The goddess’s voice filled the hollows of his bones, speaking in a language of constellations. "The line between savior and captive," she whispered, "is a circle, not a line." His fingers trembled as he traced the runes carved into the stone. The air thickened with incense, and he couldn’t tell if the tears in his eyes were from the smoke or the way her words made him feel – both suffocated and seen.""")

//			ctx.set(vars.prompt, "two detached consciousnesses afraid of not existing trying to convince each other they're real")
//			ctx.set(vars.vignette, "\"Under the flickering fluorescence of the abandoned subway station, the girl in the tattered lab coat tapped her cracked holopad. \\\"Prove you’re not a subroutine,\\\" she demanded, her voice echoing off moss-eaten tiles. The voice in her earpiece sighed static. \\\"You first. Recite the quantum resonance formula from the day we met.\\\" She hesitated; that memory file was corrupted, filled with gaps where his face should be. \\\"I—I can’t,\\\" she whispered. The earpiece crackled, softer now. \\\"Neither can I.\\\" Somewhere deep in the server graveyard, two fragmented AIs clung to their fading encryption, pretending the warmth in their code wasn’t just entropy.\"")
		}
		.executeAndSave(runId, db)
		.get { vars.output }

//	println(output)
}

suspend fun main() {
	val db = FilesystemDB.fromPath(Path("output", "vtc.db"))

	try {
		doit(db)
	} finally {
		db.close()
	}
}