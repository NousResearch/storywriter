package storywriter.pipelines

object StorywriterPipelines {
	object Vignette {
		val initial = storywriter.pipelines.initial
		val variator = storywriter.pipelines.variator
	}

	object ChapterGen {
		val firstchapter = vignette_to_chapter
	}
}