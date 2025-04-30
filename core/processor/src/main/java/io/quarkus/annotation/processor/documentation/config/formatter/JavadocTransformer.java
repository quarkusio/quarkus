package io.quarkus.annotation.processor.documentation.config.formatter;

import io.quarkus.annotation.processor.documentation.config.model.JavadocFormat;

public final class JavadocTransformer {

    private JavadocTransformer() {
    }

    public static String transform(String javadoc, JavadocFormat fromFormat, JavadocFormat toFormat) {
        switch (toFormat) {
            case ASCIIDOC:
                return JavadocToAsciidocTransformer.toAsciidoc(javadoc, fromFormat);
            case MARKDOWN:
                return JavadocToMarkdownTransformer.toMarkdown(javadoc, fromFormat);
            default:
                throw new IllegalArgumentException("Converting to " + toFormat + " is not supported");
        }
    }
}
