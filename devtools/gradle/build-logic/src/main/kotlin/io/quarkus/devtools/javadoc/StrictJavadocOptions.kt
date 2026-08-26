package io.quarkus.devtools.javadoc

import org.gradle.external.javadoc.CoreJavadocOptions

internal object StrictJavadocOptions {

    fun applyTo(options: CoreJavadocOptions) {
        options.encoding = "UTF-8"
        options.setOutputLevel(null)
        options.addBooleanOption("Werror", true)
        options.addBooleanOption("Xdoclint:all,-missing", true)
    }
}
