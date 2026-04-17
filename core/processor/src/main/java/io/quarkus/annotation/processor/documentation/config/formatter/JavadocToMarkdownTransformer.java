package io.quarkus.annotation.processor.documentation.config.formatter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.description.JavadocDescription;
import com.github.javaparser.javadoc.description.JavadocDescriptionElement;
import com.github.javaparser.javadoc.description.JavadocInlineTag;

import io.quarkus.annotation.processor.documentation.config.model.JavadocFormat;

public class JavadocToMarkdownTransformer {

    private static final Pattern START_OF_LINE = Pattern.compile("^", Pattern.MULTILINE);

    // AsciiDoc code block: ---- ... ----
    private static final Pattern CODE_BLOCK = Pattern.compile("^-{4,}\\n([\\s\\S]*?)\\n-{4,}$", Pattern.MULTILINE);

    // AsciiDoc link: link:url[text] or https?://url[text]
    private static final Pattern LINK = Pattern.compile("(?:link:)?(https?://[^\\[\\s]+)\\[([^\\]]*)\\]");

    // AsciiDoc bold: *text* (constrained – not preceded/followed by *)
    private static final Pattern BOLD = Pattern.compile("(?<!\\*)\\*(?!\\*|\\s)([^*]+?)(?<!\\s)\\*(?!\\*)");

    // AsciiDoc italic: _text_ (constrained)
    private static final Pattern ITALIC = Pattern.compile("(?<!_)_(?!_|\\s)([^_]+?)(?<!\\s)_(?!_)");

    // AsciiDoc unordered list item at the start of a line
    private static final Pattern UNORDERED_LIST_ITEM = Pattern.compile("^\\* (.+)$", Pattern.MULTILINE);

    // AsciiDoc ordered list item at the start of a line
    private static final Pattern ORDERED_LIST_ITEM = Pattern.compile("^\\. (.+)$", Pattern.MULTILINE);

    public static String toMarkdown(String javadoc, JavadocFormat format) {
        if (javadoc == null || javadoc.isBlank()) {
            return null;
        }

        if (format == JavadocFormat.MARKDOWN) {
            return javadoc;
        } else if (format == JavadocFormat.JAVADOC) {
            // the parser expects all the lines to start with "* "
            // we add it as it has been previously removed
            Javadoc parsedJavadoc = StaticJavaParser.parseJavadoc(START_OF_LINE.matcher(javadoc).replaceAll("* "), false);

            // HTML is valid Javadoc but we need to drop the Javadoc tags e.g. {@link ...}
            return simplifyJavadoc(parsedJavadoc.getDescription());
        }

        // it's Asciidoc, the fun begins...
        return asciidocToMarkdown(javadoc);
    }

    /**
     * Converts a subset of AsciiDoc syntax to Markdown.
     * <p>
     * Handles the most common constructs found in Quarkus configuration documentation:
     * definition lists, inline formatting (bold, italic), links, code blocks, and lists.
     */
    static String asciidocToMarkdown(String asciidoc) {
        if (asciidoc == null || asciidoc.isBlank()) {
            return asciidoc;
        }

        String result = asciidoc;

        // Code blocks first to prevent further processing of their content.
        // AsciiDoc: ---- ... ---- -> Markdown: ``` ... ```
        result = CODE_BLOCK.matcher(result).replaceAll("```\n$1\n```");

        // Definition lists: AsciiDoc uses "term::" syntax.
        // "term::" on its own line -> **term**: <next-line content>
        // "term:: inline definition"  -> **term**: inline definition
        result = convertDefinitionLists(result);

        // Links: link:url[text] or https://url[text] -> [text](url)
        Matcher linkMatcher = LINK.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (linkMatcher.find()) {
            String url = linkMatcher.group(1);
            String text = linkMatcher.group(2);
            String replacement = text.isBlank() ? url : "[" + text + "](" + url + ")";
            linkMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        linkMatcher.appendTail(sb);
        result = sb.toString();

        // Inline bold: *text* -> **text**
        result = BOLD.matcher(result).replaceAll("**$1**");

        // Inline italic: _text_ -> *text*
        result = ITALIC.matcher(result).replaceAll("*$1*");

        // Ordered lists: ". item" -> "1. item"
        result = ORDERED_LIST_ITEM.matcher(result).replaceAll("1. $1");

        // Unordered lists: "* item" -> "- item"
        // This must come after bold conversion since *text* is now **text**
        result = UNORDERED_LIST_ITEM.matcher(result).replaceAll("- $1");

        return result.trim();
    }

    /**
     * Converts AsciiDoc definition lists to Markdown bold-term format.
     * <p>
     * AsciiDoc definition lists use the {@code ::} delimiter:
     * <pre>
     * term::
     * The definition of the term.
     *
     * another-term:: An inline definition.
     * </pre>
     * These are converted to:
     * <pre>
     * **term**: The definition of the term.
     *
     * **another-term**: An inline definition.
     * </pre>
     */
    private static String convertDefinitionLists(String asciidoc) {
        String[] lines = asciidoc.split("\n", -1);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int ddIdx = line.indexOf("::");

            if (ddIdx < 0 || isInsideUrl(line, ddIdx)) {
                // Not a definition list line
                result.append(line);
            } else {
                String term = line.substring(0, ddIdx).trim();
                String inlineDef = line.substring(ddIdx + 2).trim();

                result.append("**").append(term).append("**");

                if (!inlineDef.isEmpty()) {
                    // Definition on the same line: "term:: definition"
                    result.append(": ").append(inlineDef);
                } else if (i + 1 < lines.length && !lines[i + 1].isBlank()) {
                    // Definition on the next line
                    result.append(": ").append(lines[i + 1].trim());
                    i++;
                } else {
                    // No definition text found
                    result.append(":");
                }
            }

            if (i < lines.length - 1) {
                result.append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Returns {@code true} if the {@code ::} at the given index is part of a URL
     * (i.e., preceded by {@code http} or {@code https}), to avoid misidentifying
     * URL schemes as AsciiDoc definition list delimiters.
     */
    private static boolean isInsideUrl(String line, int ddIdx) {
        return ddIdx >= 5 && line.substring(0, ddIdx).endsWith("http")
                || ddIdx >= 6 && line.substring(0, ddIdx).endsWith("https");
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
            if (javadocDescriptionElement instanceof JavadocInlineTag inlineTag) {
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
