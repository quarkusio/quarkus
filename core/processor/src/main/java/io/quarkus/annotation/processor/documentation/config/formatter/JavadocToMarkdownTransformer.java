package io.quarkus.annotation.processor.documentation.config.formatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.description.JavadocDescription;
import com.github.javaparser.javadoc.description.JavadocDescriptionElement;
import com.github.javaparser.javadoc.description.JavadocInlineTag;

import io.quarkus.annotation.processor.documentation.config.model.JavadocFormat;
import io.quarkus.annotation.processor.util.Strings;

public class JavadocToMarkdownTransformer {

    private static final Pattern START_OF_LINE = Pattern.compile("^", Pattern.MULTILINE);

    private static final Map<String, String> ADMONITIONS = Map.of(
            "CAUTION", "🔥",
            "IMPORTANT", "❗",
            "NOTE", "📌",
            "TIP", "💡",
            "WARNING", "⚠️");

    private static final Pattern HEADER_PATTERN = Pattern.compile("^(=+) (.+)$");
    private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("^(\\*+|\\.+) (.+)$");
    private static final Pattern IMAGE_BLOCK_PATTERN = Pattern.compile("^image::([^\\s]+)\\[(.*)\\]$");
    private static final Pattern IMAGE_INLINE_PATTERN = Pattern.compile("image:([^\\s]+)\\[(.*)\\]");
    private static final Pattern ADMONITION_BLOCK_START_PATTERN = Pattern
            .compile("^\\[(" + String.join("|", ADMONITIONS.keySet()) + ")\\]$");
    private static final String ADMONITION_BLOCK_DELIMITER = "====";
    private static final Pattern ADMONITION_INLINE_PATTERN = Pattern
            .compile("^(" + String.join("|", ADMONITIONS.keySet()) + "): (.*)$");
    private static final Pattern BOLD_PATTERN = Pattern.compile("(?<=^|\\s)\\*(.+?)\\*(?=\\s|$)");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("__(.+?)__");
    private static final Pattern BLOCK_TITLE_PATTERN = Pattern.compile("^\\.([a-z0-9].*)$");
    private static final Pattern SOURCE_BLOCK_START_PATTERN = Pattern.compile("^\\[source(?:,[ ]*([a-z]+))?.*\\]$");
    private static final Pattern SOURCE_BLOCK_DELIMITER_PATTERN = Pattern.compile("^(-----*)$");
    private static final Pattern QUOTE_BLOCK_START_PATTERN = Pattern.compile("^\\[quote(?:, (.*?))?(?:, (.*?))?]$");
    private static final Pattern QUOTE_BLOCK_DELIMITER_PATTERN = Pattern.compile("^(_____*)$");
    private static final Pattern LINK_PATTERN = Pattern.compile("(?:link:)([^\\[]+)\\[(.*?)\\]");
    private static final Pattern URL_PATTERN = Pattern.compile("\\b(http[^\\[]+)\\[(.*?)\\]");
    private static final Pattern XREF_PATTERN = Pattern.compile("xref:([^\\[]+)\\[(.*?)\\]");
    private static final Pattern ICON_PATTERN = Pattern.compile("\\bicon:([a-z0-9_-]+)\\[(?:role=([a-z0-9_-]+))?\\](?=\\s|$)");

    public static String toMarkdown(String javadoc, JavadocFormat format) {
        if (javadoc == null || javadoc.isBlank()) {
            return null;
        }

        switch (format) {
            case MARKDOWN:
                return javadoc;
            case JAVADOC:
                // the parser expects all the lines to start with "* "
                // we add it as it has been previously removed
                Javadoc parsedJavadoc = StaticJavaParser.parseJavadoc(START_OF_LINE.matcher(javadoc).replaceAll("* "));

                // HTML is valid Javadoc but we need to drop the Javadoc tags e.g. {@link ...}
                return simplifyJavadoc(parsedJavadoc.getDescription());
            case ASCIIDOC:
                return asciidocToMarkdown(javadoc);
            default:
                throw new IllegalArgumentException("Converting from " + format + " to Markdown is not supported");
        }
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

    /**
     * This obviously don't handle the whole complexity of Asciidoc but should handle most cases.
     * <p>
     * One thing that might be worth adding is support for titles for source blocks and admonitions but we can add it later on.
     * <p>
     * It doesn't support tables (yet).
     */
    private static String asciidocToMarkdown(String asciidoc) {
        List<String> lines = asciidoc.lines().toList();
        List<String> result = new ArrayList<>();
        String currentAdmonition = null;
        boolean inAdmonitionPreamble = false;
        boolean inAdmonitionBlock = false;
        String currentSourceBlockLanguage = null;
        boolean inSourcePreamble = false;
        boolean inSourceBlock = false;
        String currentSourceBlockTitle = null;
        String currentSourceBlockDelimiter = null;
        boolean inQuoteBlock = false;
        boolean quoteStarted = false;
        String currentQuoteBlockDelimiter = null;
        String quoteAuthor = null;
        String quoteSource = null;

        String linePrefix = "";

        for (String line : lines) {
            String markdownLine = line;

            if (inAdmonitionPreamble) {
                if (ADMONITION_BLOCK_DELIMITER.equals(line)) {
                    inAdmonitionBlock = true;
                    inAdmonitionPreamble = false;
                    result.add("> [!" + currentAdmonition + "]");
                    continue;
                } else {
                    // we haven't found a proper delimiter so we ignore the admonition altogether
                    inAdmonitionPreamble = false;
                }
            }

            if (inAdmonitionBlock) {
                if (ADMONITION_BLOCK_DELIMITER.equals(line)) {
                    inAdmonitionBlock = false;
                    currentAdmonition = null;
                    linePrefix = "";
                    continue;
                } else {
                    linePrefix = "> ";
                }
            }

            if (inSourcePreamble) {
                Matcher blockTitleMatcher = BLOCK_TITLE_PATTERN.matcher(line);
                if (blockTitleMatcher.matches()) {
                    currentSourceBlockTitle = blockTitleMatcher.group(1);
                }
            }

            if (inSourceBlock) {
                if (currentSourceBlockDelimiter.equals(line)) {
                    // End of source block
                    result.add(linePrefix + "```");
                    inSourcePreamble = false;
                    inSourceBlock = false;
                    currentSourceBlockLanguage = null;
                    currentSourceBlockDelimiter = null;
                    currentSourceBlockTitle = null;
                    continue;
                } else {
                    // Inside source block
                    result.add(linePrefix + markdownLine);
                    continue;
                }
            }

            Matcher sourceBlockStartMatcher = SOURCE_BLOCK_START_PATTERN.matcher(line);
            if (sourceBlockStartMatcher.matches()) {
                if (!Strings.isBlank(sourceBlockStartMatcher.group(1))) {
                    currentSourceBlockLanguage = sourceBlockStartMatcher.group(1).trim();
                }
                inSourcePreamble = true;
                // Skip the start marker
                continue;
            }

            Matcher sourceBlockDelimiterMatcher = SOURCE_BLOCK_DELIMITER_PATTERN.matcher(line);
            if (sourceBlockDelimiterMatcher.matches()) {
                currentSourceBlockDelimiter = sourceBlockDelimiterMatcher.group(0);
                // Start of code block
                if (!Strings.isBlank(currentSourceBlockTitle)) {
                    result.add(linePrefix + "**" + currentSourceBlockTitle + "**");
                    result.add(linePrefix + "");
                }
                result.add(
                        linePrefix + "```" + (!Strings.isBlank(currentSourceBlockLanguage) ? currentSourceBlockLanguage : ""));
                inSourcePreamble = false;
                inSourceBlock = true;
                continue;
            }

            if (inQuoteBlock) {
                Matcher quoteBlockDelimiterMatcher = QUOTE_BLOCK_DELIMITER_PATTERN.matcher(line);
                if (!quoteStarted && quoteBlockDelimiterMatcher.matches()) {
                    currentQuoteBlockDelimiter = quoteBlockDelimiterMatcher.group(0);
                    continue;
                } else if (line.equals(currentQuoteBlockDelimiter)) {
                    // End of quote block
                    if (quoteAuthor != null || quoteSource != null) {
                        result.add(linePrefix + ">");
                        result.add(linePrefix + "> — " + (quoteAuthor != null ? quoteAuthor : "")
                                + (quoteSource != null ? ", " + quoteSource : ""));
                    }
                    inQuoteBlock = false;
                    quoteStarted = false;
                    currentQuoteBlockDelimiter = null;
                    continue;
                } else {
                    // Inside quote block
                    result.add(linePrefix + "> " + line);
                    quoteStarted = true;
                    continue;
                }
            }

            Matcher quoteBlockStartMatcher = QUOTE_BLOCK_START_PATTERN.matcher(line);
            if (quoteBlockStartMatcher.matches()) {
                // Start of quote block
                quoteAuthor = quoteBlockStartMatcher.group(1);
                quoteSource = quoteBlockStartMatcher.group(2);
                inQuoteBlock = true;
                continue;
            }

            Matcher admonitionBlockStartMatcher = ADMONITION_BLOCK_START_PATTERN.matcher(line);
            if (admonitionBlockStartMatcher.matches()) {
                currentAdmonition = admonitionBlockStartMatcher.group(1);
                inAdmonitionPreamble = true;
                // Skip the start marker
                continue;
            }

            // Convert headings
            Matcher headingMatcher = HEADER_PATTERN.matcher(line);
            if (headingMatcher.find()) {
                int level = headingMatcher.group(1).length();
                String text = headingMatcher.group(2);
                markdownLine = "#".repeat(level) + " " + text;
            }

            // Convert list items
            Matcher listItemMatcher = LIST_ITEM_PATTERN.matcher(line);
            if (listItemMatcher.find()) {
                String marker = listItemMatcher.group(1);
                String text = listItemMatcher.group(2);
                if (marker.startsWith("*")) {
                    markdownLine = "- " + text;
                } else if (marker.startsWith(".")) {
                    markdownLine = "1. " + text;
                }
            }

            // Convert italic and bold
            markdownLine = convertInline(markdownLine, ITALIC_PATTERN, "*");
            markdownLine = convertInline(markdownLine, BOLD_PATTERN, "**");

            // Inline Admonitions
            if (!inAdmonitionBlock) {
                Matcher admonitionInlineMatcher = ADMONITION_INLINE_PATTERN.matcher(line);
                if (admonitionInlineMatcher.find()) {
                    String admonition = admonitionInlineMatcher.group(1);
                    if (ADMONITIONS.containsKey(admonition)) {
                        markdownLine = "> " + ADMONITIONS.get(admonition) + " " + admonitionInlineMatcher.group(2);
                    } else {
                        markdownLine = "> " + markdownLine;
                    }
                }
            }

            // Convert block images
            Matcher blockImageMatcher = IMAGE_BLOCK_PATTERN.matcher(line);
            if (blockImageMatcher.find()) {
                String target = blockImageMatcher.group(1);
                String altText = blockImageMatcher.group(2);
                markdownLine = "![" + altText + "](" + target + ")";
            }

            // Convert inline images
            Matcher inlineImageMatcher = IMAGE_INLINE_PATTERN.matcher(line);
            if (inlineImageMatcher.find()) {
                String target = inlineImageMatcher.group(1);
                String altText = inlineImageMatcher.group(2);
                markdownLine = line.replace(inlineImageMatcher.group(), "![" + altText + "](" + target + ")");
            }

            // Convert links
            markdownLine = convertLinksAndXrefs(markdownLine, LINK_PATTERN, "link");
            // Convert direct URL links
            markdownLine = convertLinksAndXrefs(markdownLine, URL_PATTERN, "url");
            // Convert xrefs
            markdownLine = convertLinksAndXrefs(markdownLine, XREF_PATTERN, "xref");

            // Convert icons
            markdownLine = convertIcons(markdownLine);

            result.add(linePrefix + markdownLine);
        }

        return result.stream().collect(Collectors.joining("\n"));
    }

    private static String convertInline(String line, Pattern pattern, String markdownDelimiter) {
        Matcher matcher = pattern.matcher(line);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, markdownDelimiter + matcher.group(1) + markdownDelimiter);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String convertLinksAndXrefs(String line, Pattern pattern, String type) {
        Matcher matcher = pattern.matcher(line);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            if (type.equals("link") || type.equals("url")) {
                matcher.appendReplacement(sb, "[" + matcher.group(2) + "](" + matcher.group(1) + ")");
            } else if (type.equals("xref")) {
                String xref = matcher.group(1);
                if (xref.contains(".adoc")) {
                    xref = "https://quarkus.io/guides/" + xref.replace(".adoc", "");
                } else {
                    xref = "#" + xref;
                }

                matcher.appendReplacement(sb, "[" + matcher.group(2) + "](" + xref + ")");
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String convertIcons(String line) {
        Matcher matcher = ICON_PATTERN.matcher(line);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String icon = matcher.group(1);
            String emoji;

            switch (icon) {
                case "check":
                    emoji = "✅";
                    break;
                case "times":
                    emoji = "❌";
                    break;
                default:
                    // TODO we probably need to collect the errors and log them instead
                    throw new IllegalArgumentException("Icon " + matcher.group(1) + " is not mapped.");
            }

            matcher.appendReplacement(sb, emoji);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
