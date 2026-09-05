package io.quarkus.devtools.javadoc

import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.CoreJavadocOptions
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

internal fun Project.registerStrictJavadoc() {
    val mainSourceSet = extensions.getByType<SourceSetContainer>().named(SourceSet.MAIN_SOURCE_SET_NAME)

    tasks.register<Javadoc>("strictJavadoc") {
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        description = "Generates warning-clean API documentation with all relevant doclint checks enabled."
        source(mainSourceSet.map { it.allJava })
        classpath = mainSourceSet.get().output.plus(mainSourceSet.get().compileClasspath)
        destinationDir = layout.buildDirectory.dir("docs/strictJavadoc").get().asFile
        setFailOnError(true)
        StrictJavadocOptions.applyTo(options as CoreJavadocOptions)
    }
}
