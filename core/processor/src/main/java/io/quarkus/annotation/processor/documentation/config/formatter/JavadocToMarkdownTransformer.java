package io.quarkus.annotation.processor.documentation.config.formatter;

import java.util.regex.Pattern;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.description.JavadocDescription;
import com.github.javaparser.javadoc.description.JavadocDescriptionElement;
import com.github.javaparser.javadoc.description.JavadocInlineTag;

import io.quarkus.annotation.processor.documentation.config.model.JavadocFormat;

public class JavadocToMarkdownTransformer {

    private static final Pattern START_OF_LINE = Pattern.compile("^", Pattern.MULTILINE);

    public static String toMarkdown(String javadoc, JavadocFormat format) {
        if (javadoc == null || javadoc.isBlank()) {
            return null;
        }

        if (format == JavadocFormat.MARKDOWN) {
            return javadoc;
        } else if (format == JavadocFormat.JAVADOC) {
            // the parser expects all the lines to start with "* "
            // we add it as it has been previously removed
            Javadoc parsedJavadoc = StaticJavaParser.parseJavadoc(START_OF_LINE.matcher(javadoc).replaceAll("* "));

            // HTML is valid Javadoc but we need to drop the Javadoc tags e.g. {@link ...}
            return simplifyJavadoc(parsedJavadoc.getDescription());
        }

        // it's Asciidoc, the fun begins...
        return "";
    }

    /**
     * This is not definitely not perfect but it can be used to filter the Javadoc included in Markdown.
     * <p>
     * We will need to discuss further how to handle passing the Javadoc to the IDE.
     * In Quarkus, we have Asciidoc, standard Javadoc and soon we might have Markdown javadoc.
     */
    private static String simplifyJavadoc(JavadocDescription javadocDescription) {
        StringBuilder sb = new StringBuilder();

        for (JavadocDescriptionElement javadocDescriptionElement : javadocDescription.getElements()) {
            if (javadocDescriptionElement instanceof JavadocInlineTag) {
                JavadocInlineTag inlineTag = (JavadocInlineTag) javadocDescriptionElement;
                String content = inlineTag.getContent().trim();
                switch (inlineTag.getType()) {
                    case CODE:
                    case VALUE:
                    case LITERAL:
                    case SYSTEM_PROPERTY:
                    case LINK:
                    case LINKPLAIN:
                        sb.append("<code>");
                        sb.append(escapeHtml(content));
                        sb.append("</code>");
                        break;
                    default:
                        sb.append(content);
                        break;
                }
            } else {
                sb.append(javadocDescriptionElement.toText());
            }
        }

        return sb.toString().trim();
    }

    private static String escapeHtml(String s) {
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > 127 || c == '"' || c == '\'' || c == '<' || c == '>' || c == '&') {
                out.append("&#");
                out.append((int) c);
                out.append(';');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
