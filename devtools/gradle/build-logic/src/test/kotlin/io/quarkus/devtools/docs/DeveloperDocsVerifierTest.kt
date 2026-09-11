package io.quarkus.devtools.docs

import java.nio.file.Path

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DeveloperDocsVerifierTest {

    @Test
    fun acceptsValidIndexAndRelativeLinks() {
        assertThat(verify("valid")).isEmpty()
    }

    @Test
    fun rejectsMissingRelativeTarget() {
        assertThat(verify("missing-target"))
            .containsExactly("guide.md links to a missing relative target: missing.md")
    }

    @Test
    fun rejectsAbsoluteLocalPath() {
        assertThat(verify("absolute-path"))
            .containsExactly("guide.md links to an absolute local path: /home/developer/notes.md")
    }

    @Test
    fun rejectsDocsWipLink() {
        assertThat(verify("docs-wip"))
            .containsExactly("guide.md links into docs-wip: ../docs-wip/investigation.md")
    }

    @Test
    fun rejectsUnindexedPage() {
        assertThat(verify("unindexed"))
            .containsExactly("extra.md is not linked from README.md")
    }

    @Test
    fun rejectsDuplicateReadmeLink() {
        assertThat(verify("duplicate-index"))
            .containsExactly("guide.md is linked 2 times from README.md; expected exactly once")
    }

    @Test
    fun acceptsReferenceStyleLinks() {
        assertThat(verify("reference-style")).isEmpty()
    }

    @Test
    fun validatesImageTargets() {
        assertThat(verify("image")).isEmpty()
    }

    @Test
    fun rejectsMissingImageTarget() {
        assertThat(verify("missing-image"))
            .containsExactly("guide.md links to a missing relative target: missing.svg")
    }

    @Test
    fun rejectsHtmlLinks() {
        assertThat(verify("html-link"))
            .containsExactly("guide.md uses an unsupported HTML link or image")
    }

    @Test
    fun rejectsParenthesizedInlineDestinations() {
        assertThat(verify("parenthesized-inline"))
            .containsExactly("guide.md uses an unsupported parenthesized inline link destination")
    }

    private fun verify(fixture: String): List<String> {
        val resource = requireNotNull(javaClass.getResource("/developer-docs/$fixture")) {
            "Missing fixture $fixture"
        }
        return DeveloperDocsVerifier.verify(Path.of(resource.toURI()))
    }
}
