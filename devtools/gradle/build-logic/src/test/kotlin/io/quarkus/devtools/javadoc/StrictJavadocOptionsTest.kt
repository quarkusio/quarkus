package io.quarkus.devtools.javadoc

import java.nio.file.Files
import java.nio.file.Path

import org.assertj.core.api.Assertions.assertThat
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.CoreJavadocOptions
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class StrictJavadocOptionsTest {

    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun enablesAllRelevantDoclintChecksAndTreatsWarningsAsErrors() {
        val options = StandardJavadocDocletOptions()

        StrictJavadocOptions.applyTo(options)
        val optionFile = temporaryDirectory.resolve("javadoc.options").toFile()
        options.write(optionFile)

        assertStrictOptions(optionFile.toPath())
    }

    @Test
    fun registersStrictTaskWithTheMainSourceSetAndIsolatedOutput() {
        val project = ProjectBuilder.builder()
            .withProjectDir(temporaryDirectory.toFile())
            .build()
        project.pluginManager.apply("java-library")
        project.registerStrictJavadoc()
        val main = project.extensions.getByType<SourceSetContainer>().named("main").get()
        val sourceFile = temporaryDirectory.resolve("src/main/java/example/Documented.java")
        Files.createDirectories(sourceFile.parent)
        Files.writeString(sourceFile, "package example; public final class Documented {}")

        val task = project.tasks.named<Javadoc>("strictJavadoc").get()
        val optionFile = temporaryDirectory.resolve("registered-javadoc.options")
        (task.options as CoreJavadocOptions).write(optionFile.toFile())

        assertThat(task.source.files)
            .containsExactlyInAnyOrderElementsOf(main.allJava.files)
            .contains(sourceFile.toFile())
        assertThat(task.classpath.files)
            .containsAll(main.output.files)
            .containsAll(main.compileClasspath.files)
        assertThat(task.destinationDir)
            .isEqualTo(project.layout.buildDirectory.dir("docs/strictJavadoc").get().asFile)
        assertThat(task.isFailOnError).isTrue()
        assertStrictOptions(optionFile)
    }

    private fun assertStrictOptions(optionFile: Path) {
        val lines = optionFile.toFile().readLines().map(String::trim).filter(String::isNotEmpty)
        assertThat(lines).contains("-Werror")
        assertThat(lines.filter { it.startsWith("-Xdoclint:") })
            .containsExactly("-Xdoclint:all,-missing")
        assertThat(lines).doesNotContain("-quiet")
    }
}
