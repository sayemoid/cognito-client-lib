package utils

fun htmlToPlainText(html: String): String {
	var text = ""
	var inTag = false

	for (char in html) {
		if (char == '<') {
			inTag = true
		} else if (char == '>') {
			inTag = false
		} else if (!inTag) {
			text += char
		}
	}
	return text.trim()
}

fun htmlToPlainTextV2(html: String): String {
	// Quick check: if no tags are present, return the text as is
	if ('<' !in html && '>' !in html) {
		return html.trim()
	}

	val textBuilder = StringBuilder()
	var inTag = false

	for (char in html) {
		when (char) {
			'<' -> inTag = true
			'>' -> inTag = false
			else -> if (!inTag) textBuilder.append(char)
		}
	}

	return textBuilder.toString().trim()
}

fun String.asPlainTextFromHtml(): String = htmlToPlainTextV2(this)

fun String.splitIntoParagraphs(): List<String> {
	return this.split("\\R+".toRegex()).filter { it.isNotBlank() }
}

data class MarkdownSegment(val isCodeBlock: Boolean, val content: String) {
	override fun toString(): String {
		return "MarkdownSegment(isCodeBlock=$isCodeBlock, content='$content')"
	}
}

fun String.splitMarkdownSegments(): List<MarkdownSegment> {
	val segments = mutableListOf<MarkdownSegment>()
	val lines = this.lines()
	var inCodeBlock = false
	val buffer = mutableListOf<String>()

	// Process each line
	for (line in lines) {
		// Check if the line is a code fence. Accept optional language identifier.
		if (line.trim().matches("^```.*".toRegex())) {
			if (inCodeBlock) {
				// End code block: add accumulated lines as a code block segment.
				segments.add(
					MarkdownSegment(
						isCodeBlock = true,
						content = buffer.joinToString("\n")
					)
				)
				buffer.clear()
			} else {
				// Start code block:
				// Flush any accumulated normal text.
				if (buffer.isNotEmpty()) {
					segments.add(
						MarkdownSegment(
							isCodeBlock = false,
							content = buffer.joinToString("\n")
						)
					)
					buffer.clear()
				}
			}
			// Toggle code block state.
			inCodeBlock = !inCodeBlock
		} else {
			buffer.add(line)
		}
	}
	// Flush remaining text
	if (buffer.isNotEmpty()) {
		segments.add(
			MarkdownSegment(
				isCodeBlock = inCodeBlock,
				content = buffer.joinToString("\n")
			)
		)
	}
	return segments.flatMap { segment ->
		if (segment.isCodeBlock) {
			listOf(segment)
		} else {
			segment.content.splitIntoParagraphs()
				.map {
					MarkdownSegment(isCodeBlock = false, content = it)
				}
		}
	}
}


fun String.isMarkdown(): Boolean = listOf(
	"#+\\s".toRegex(), // Headings (#, ##, ###)
	"\\*\\*.*?\\*\\*".toRegex(), // Bold (**text**)
	"\\*.*?\\*".toRegex(), // Italic (*text*)
	"`[^`]+`".toRegex(), // Inline code (`code`)
	"```[a-zA-Z]*[\\s\\S]+?```".toRegex(), // Code blocks (```yaml ... ```)
	"-\\s+".toRegex(), // Unordered list (- item)
	"\\d+\\.\\s+".toRegex(), // Ordered list (1. item)
	"\\[.*?]\\(.*?\\)".toRegex() // Links [text](url)
).any {
	it.containsMatchIn(this)
}


fun levenshtein(a: String, b: String): Int {
	val costs = IntArray(b.length + 1) { it }
	for (i in 1..a.length) {
		var previousCost = i - 1
		costs[0] = i
		for (j in 1..b.length) {
			val currentCost = costs[j]
			val cost = if (a[i - 1] == b[j - 1]) 0 else 1
			costs[j] = minOf(costs[j] + 1, costs[j - 1] + 1, previousCost + cost)
			previousCost = currentCost
		}
	}
	return costs[b.length]
}

fun String.matchAccuracy(text: String): Double {
	val distance = levenshtein(this.lowercase(), text.lowercase())
	val maxLen = maxOf(this.length, text.length)
	// If both strings are empty, return 100% match.
	if (maxLen == 0) return 100.0
	val similarity = (maxLen - distance).toDouble() / maxLen
	return similarity * 100
}
