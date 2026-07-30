package io.quarkus.devtools.docs

import java.nio.file.Files
import java.nio.file.Path

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

@DisableCachingByDefault(because = "This verification task has no outputs")
abstract class VerifyDeveloperDocs : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val docsDirectory: DirectoryProperty

    @TaskAction
    fun verifyDocumentation() {
        val failures = DeveloperDocsVerifier.verify(docsDirectory.get().asFile.toPath())
        if (failures.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Developer documentation verification failed:")
                    failures.forEach { appendLine("  - $it") }
                }.trimEnd()
            )
        }
    }
}

internal object DeveloperDocsVerifier {

    private val inlineLink = Regex("""(!?)\[[^]\r\n]*]\(([^)\r\n]+)\)""")
    private val parenthesizedInlineDestination = Regex("""!?\[[^]\r\n]*]\([^)\r\n]*\(""")
    private val referenceDefinition = Regex("""(?m)^ {0,3}\[([^]\r\n]+)]:[ \t]*(\S.*)$""")
    private val fullReferenceLink = Regex("""(!?)\[([^]\r\n]+)]\[([^]\r\n]*)]""")
    private val shortcutReferenceLink = Regex("""(?<!])(!?)\[([^]\r\n]+)](?![\[(:])""")
    private val htmlLink = Regex("""(?is)<\s*(?:a\b[^>]*\bhref|img\b[^>]*\bsrc)\s*=""")
    private val windowsAbsolutePath = Regex("""^[A-Za-z]:[/\\].*""")
    private val uriScheme = Regex("""^[A-Za-z][A-Za-z0-9+.-]*:.*""")

    fun verify(docsDirectory: Path): List<String> {
        val root = docsDirectory.toAbsolutePath().normalize()
        val failures = mutableListOf<String>()
        if (!Files.isDirectory(root)) {
            return listOf("documentation directory does not exist: $root")
        }

        val pages = Files.walk(root).use { paths ->
            paths.filter { it.isRegularFile() && it.extension.equals("md", ignoreCase = true) }
                .sorted()
                .toList()
        }
        val readme = root.resolve("README.md")
        if (!readme.isRegularFile()) {
            failures += "README.md is missing"
        }

        val readmePageLinks = mutableMapOf<Path, Int>()
        pages.forEach { page ->
            val parsed = parsePage(page)
            failures += parsed.failures.map { "${root.relativize(page)} $it" }
            parsed.links.forEach linkLoop@{ link ->
                val destination = link.destination
                val displayPage = root.relativize(page)
                if (isAbsoluteLocalPath(destination)) {
                    failures += "$displayPage links to an absolute local path: $destination"
                    return@linkLoop
                }
                if (isExternal(destination) || destination.startsWith("#")) {
                    return@linkLoop
                }
                if (referencesDocsWip(destination)) {
                    failures += "$displayPage links into docs-wip: $destination"
                    return@linkLoop
                }

                val targetText = destination.substringBefore('#').substringBefore('?')
                if (targetText.isBlank()) {
                    return@linkLoop
                }
                val target = page.parent.resolve(targetText).normalize()
                if (!target.startsWith(root) || !Files.exists(target)) {
                    failures += "$displayPage links to a missing relative target: $destination"
                    return@linkLoop
                }
                if (page == readme && target.isRegularFile()
                    && link.countsForIndex
                    && target.extension.equals("md", ignoreCase = true)
                ) {
                    readmePageLinks.compute(target) { _, count -> (count ?: 0) + 1 }
                }
            }
        }

        pages.filter { it != readme }.forEach { page ->
            when (val links = readmePageLinks[page] ?: 0) {
                0 -> failures += "${root.relativize(page)} is not linked from README.md"
                1 -> Unit
                else -> failures += "${root.relativize(page)} is linked $links times from README.md; expected exactly once"
            }
        }
        return failures.distinct()
    }

    private fun parsePage(page: Path): ParsedPage {
        val text = page.readText()
        val failures = mutableListOf<String>()
        if (parenthesizedInlineDestination.containsMatchIn(text)) {
            failures += "uses an unsupported parenthesized inline link destination"
        }
        if (htmlLink.containsMatchIn(text)) {
            failures += "uses an unsupported HTML link or image"
        }

        val definitions = linkedMapOf<String, String>()
        referenceDefinition.findAll(text).forEach { match ->
            val label = normalizeLabel(match.groupValues[1])
            val destination = normalizeDestination(match.groupValues[2])
            if (definitions.putIfAbsent(label, destination) != null) {
                failures += "defines reference label '${match.groupValues[1]}' more than once"
            }
        }

        val links = mutableListOf<DocumentationLink>()
        definitions.values.forEach { destination ->
            if (destination.isNotEmpty()) {
                links += DocumentationLink(destination, false)
            }
        }
        inlineLink.findAll(text)
            .filter { !it.groupValues[2].contains('(') }
            .forEach { match ->
                val destination = normalizeDestination(match.groupValues[2])
                if (destination.isNotEmpty()) {
                    links += DocumentationLink(destination, match.groupValues[1].isEmpty())
                }
            }
        fullReferenceLink.findAll(text).forEach { match ->
            val rawLabel = match.groupValues[3].ifEmpty { match.groupValues[2] }
            val destination = definitions[normalizeLabel(rawLabel)]
            if (destination == null) {
                failures += "uses undefined reference label '$rawLabel'"
            } else {
                links += DocumentationLink(destination, match.groupValues[1].isEmpty())
            }
        }
        shortcutReferenceLink.findAll(text).forEach { match ->
            val destination = definitions[normalizeLabel(match.groupValues[2])] ?: return@forEach
            links += DocumentationLink(destination, match.groupValues[1].isEmpty())
        }
        return ParsedPage(links, failures)
    }

    private fun normalizeLabel(label: String): String =
        label.trim().lowercase().replace(Regex("""\s+"""), " ")

    private fun normalizeDestination(rawDestination: String): String {
        val destination = rawDestination.trim()
        if (destination.startsWith("<")) {
            val closingBracket = destination.indexOf('>')
            return if (closingBracket > 0) destination.substring(1, closingBracket) else destination
        }
        return destination.substringBefore(' ').substringBefore('\t')
    }

    private fun isAbsoluteLocalPath(destination: String): Boolean {
        val normalized = destination.replace('\\', '/')
        return normalized.startsWith("/")
            || normalized.startsWith("~/")
            || normalized.startsWith("file:", ignoreCase = true)
            || windowsAbsolutePath.matches(destination)
    }

    private fun referencesDocsWip(destination: String): Boolean =
        destination.replace('\\', '/')
            .split('/', '#', '?')
            .any { it.equals("docs-wip", ignoreCase = true) }

    private fun isExternal(destination: String): Boolean = uriScheme.matches(destination)

    private data class DocumentationLink(val destination: String, val countsForIndex: Boolean)

    private data class ParsedPage(val links: List<DocumentationLink>, val failures: List<String>)
}
