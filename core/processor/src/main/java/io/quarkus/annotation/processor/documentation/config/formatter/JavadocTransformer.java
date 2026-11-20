package io.quarkus.annotation.processor.documentation.config.formatter;

import java.util.Map;

import io.quarkus.annotation.processor.documentation.config.model.JavadocFormat;

public final class JavadocTransformer {

    private JavadocTransformer() {
    }

    public static String transform(String javadoc, JavadocFormat fromFormat, JavadocFormat toFormat) {
        return transform(javadoc, fromFormat, toFormat, Map.of());
    }

    /**
     * @param attributes the Asciidoc attributes ({@code name -> value}) that may be referenced
     *        in the Javadoc as {@code {name}}; typically coming from the Maven properties
     *        of the extension being processed
     */
    public static String transform(String javadoc, JavadocFormat fromFormat, JavadocFormat toFormat,
            Map<String, String> attributes) {
        switch (toFormat) {
            case ASCIIDOC:
                return JavadocToAsciidocTransformer.toAsciidoc(javadoc, fromFormat);
            case MARKDOWN:
                return JavadocToMarkdownTransformer.toMarkdown(javadoc, fromFormat, attributes);
            default:
                throw new IllegalArgumentException("Converting to " + toFormat + " is not supported");
        }
    }
}
