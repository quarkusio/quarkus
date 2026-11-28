package io.quarkus.annotation.processor.documentation.config.formatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private static final Map<String, String> ICONS = Map.of(
            "check", "✔️",
            "times", "✖️",
            "question", "❓",
            "exclamation", "❗",
            "info", "ℹ️",
            "warning", "⚠️");

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
    private static final Pattern ICON_PATTERN = Pattern.compile("icon:([a-zA-Z0-9_-]+)(\\[[^\\]]*\\])?");
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_.-]+)\\}");
    private static final Pattern LABELED_LIST_ITEM_PATTERN = Pattern.compile("^(.+?[^:\\s])::\\s*(.*)$");
    private static final Pattern TABLE_COLS_PATTERN = Pattern.compile("^\\[cols=([0-9]+)[^\\]]*\\]$");
    private static final Pattern TABLE_DELIMITER_PATTERN = Pattern.compile("^([|!])===+$");

    private static final String TABLE_CELL_SPEC_CHARS = "0123456789.+<>^adehlms";

    public static String toMarkdown(String javadoc, JavadocFormat format) {
        return toMarkdown(javadoc, format, Map.of());
    }

    /**
     * @param attributes the Asciidoc attributes ({code name -> value}) that may be referenced
     *        in the Javadoc as {@code {name}}; typically coming from the Maven properties
     *        of the extension being processed
     */
    public static String toMarkdown(String javadoc, JavadocFormat format, Map<String, String> attributes) {
        if (javadoc == null || javadoc.isBlank()) {
            return null;
        }

        switch (format) {
            case MARKDOWN:
                return javadoc;
            case JAVADOC:
                // the parser expects all the lines to start with "* "
                // we add it as it has been previously removed
                Javadoc parsedJavadoc = StaticJavaParser.parseJavadoc(START_OF_LINE.matcher(javadoc).replaceAll("* "),
                        false);

                // HTML is valid Javadoc but we need to drop the Javadoc tags e.g. {@link ...}
                return simplifyJavadoc(parsedJavadoc.getDescription());
            case ASCIIDOC:
                return asciidocToMarkdown(javadoc, attributes == null ? Map.of() : attributes);
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

    /**
     * This obviously does not handle the whole complexity of Asciidoc but should handle most cases.
     * <p>
     * One thing that might be worth adding is support for titles for source blocks and admonitions but we can add it later on.
     * <p>
     * Tables are rendered as HTML: Markdown tables cannot express colspans, rowspans or
     * alignments and the Asciidoc tables found in our config documentation use all of them.
     */
    private static String asciidocToMarkdown(String asciidoc, Map<String, String> attributes) {
        List<String> lines = substituteAttributes(asciidoc, attributes).lines().toList();
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
        int pendingTableCols = -1;
        boolean inLabeledListDefinition = false;

        String linePrefix = "";

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
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

            Matcher tableDelimiterMatcher = TABLE_DELIMITER_PATTERN.matcher(line);
            if (tableDelimiterMatcher.matches()) {
                // Asciidoc tables are rendered as HTML: colspans, rowspans and cell
                // alignments cannot be expressed in a Markdown table
                i = renderTable(lines, i, tableDelimiterMatcher.group(1).charAt(0), pendingTableCols, linePrefix, result);
                pendingTableCols = -1;
                continue;
            }

            Matcher tableColsMatcher = TABLE_COLS_PATTERN.matcher(line);
            if (tableColsMatcher.matches() && i + 1 < lines.size()
                    && TABLE_DELIMITER_PATTERN.matcher(lines.get(i + 1)).matches()) {
                // column declaration preceding a table, it is consumed by the table itself
                pendingTableCols = Integer.parseInt(tableColsMatcher.group(1));
                continue;
            }

            Matcher admonitionBlockStartMatcher = ADMONITION_BLOCK_START_PATTERN.matcher(line);
            if (admonitionBlockStartMatcher.matches()) {
                currentAdmonition = admonitionBlockStartMatcher.group(1);
                inAdmonitionPreamble = true;
                // Skip the start marker
                continue;
            }

            Matcher labeledListItemMatcher = LABELED_LIST_ITEM_PATTERN.matcher(line);
            boolean labeledListItem = labeledListItemMatcher.matches()
                    && !isBlockMacro(labeledListItemMatcher.group(1));

            if (inLabeledListDefinition && !labeledListItem) {
                if (line.isBlank()) {
                    inLabeledListDefinition = false;
                } else {
                    // continuation of the definition
                    result.add(linePrefix + "  " + applyInlineConversions(line));
                    continue;
                }
            }

            if (labeledListItem) {
                // `term`:: definition (dt/dd constructs)
                result.add(linePrefix + "- **" + applyInlineConversions(labeledListItemMatcher.group(1).trim()) + "**:"
                        + (labeledListItemMatcher.group(2).isBlank() ? ""
                                : " " + applyInlineConversions(labeledListItemMatcher.group(2).trim())));
                inLabeledListDefinition = labeledListItemMatcher.group(2).isBlank();
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

            result.add(linePrefix + applyInlineConversions(markdownLine));
        }

        return result.stream().map(String::stripTrailing).collect(Collectors.joining("\n"));
    }

    /**
     * Substitutes the known Asciidoc attributes ({code {name}}) with their values.
     * Unknown attributes are left untouched.
     */
    private static String substituteAttributes(String asciidoc, Map<String, String> attributes) {
        if (attributes.isEmpty()) {
            return asciidoc;
        }

        Matcher attributeMatcher = ATTRIBUTE_PATTERN.matcher(asciidoc);
        StringBuffer sb = new StringBuffer();
        while (attributeMatcher.find()) {
            String value = attributes.get(attributeMatcher.group(1));
            attributeMatcher.appendReplacement(sb,
                    value != null ? Matcher.quoteReplacement(value) : attributeMatcher.group());
        }
        attributeMatcher.appendTail(sb);
        return sb.toString();
    }

    private static final Set<String> BLOCK_MACROS = Set.of(
            "image", "include", "ifdef", "ifndef", "endif", "ifeval", "footnote",
            "pass", "comment", "video", "audio", "xref");

    /**
     * Block macros such as {@code image::foo[bar]} or {@code include::file[]} must not be
     * mistaken for labeled list items.
     */
    private static boolean isBlockMacro(String term) {
        return BLOCK_MACROS.contains(term.trim());
    }

    /**
     * Applies all the inline conversions (italic, bold, inline images, icons, links and xrefs).
     */
    private static String applyInlineConversions(String text) {
        // Convert italic and bold
        text = convertInline(text, ITALIC_PATTERN, "*");
        text = convertInline(text, BOLD_PATTERN, "**");

        // Convert inline images
        Matcher inlineImageMatcher = IMAGE_INLINE_PATTERN.matcher(text);
        if (inlineImageMatcher.find()) {
            String target = inlineImageMatcher.group(1);
            String altText = inlineImageMatcher.group(2);
            text = text.replace(inlineImageMatcher.group(), "![" + altText + "](" + target + ")");
        }

        // Convert icons
        Matcher iconMatcher = ICON_PATTERN.matcher(text);
        StringBuffer iconBuffer = new StringBuffer();
        while (iconMatcher.find()) {
            String icon = ICONS.get(iconMatcher.group(1));
            iconMatcher.appendReplacement(iconBuffer,
                    icon != null ? Matcher.quoteReplacement(icon) : iconMatcher.group(1));
        }
        iconMatcher.appendTail(iconBuffer);
        text = iconBuffer.toString();

        // Convert links
        text = convertLinksAndXrefs(text, LINK_PATTERN, "link");
        // Convert direct URL links
        text = convertLinksAndXrefs(text, URL_PATTERN, "url");
        // Convert xrefs
        text = convertLinksAndXrefs(text, XREF_PATTERN, "xref");

        return text;
    }

    /**
     * Parses the table starting after the opening delimiter at {@code delimiterIndex}
     * and appends its HTML rendering to {@code result}.
     *
     * @return the index of the closing delimiter
     */
    private static int renderTable(List<String> lines, int delimiterIndex, char separator, int cols, String linePrefix,
            List<String> result) {
        List<List<TableCell>> rows = new ArrayList<>();
        List<TableCell> currentRow = new ArrayList<>();
        TableCell currentCell = null;
        int i = delimiterIndex + 1;

        for (; i < lines.size(); i++) {
            String line = lines.get(i);

            if (TABLE_DELIMITER_PATTERN.matcher(line).matches()) {
                break;
            }

            if (line.isBlank()) {
                // a blank line ends the current row
                if (currentCell != null) {
                    currentRow.add(currentCell);
                    currentCell = null;
                }
                if (!currentRow.isEmpty()) {
                    rows.add(currentRow);
                    currentRow = new ArrayList<>();
                }
                continue;
            }

            List<TableCell> lineCells = parseCellLine(line, separator);
            if (!lineCells.isEmpty()) {
                for (TableCell cell : lineCells) {
                    if (currentCell != null) {
                        currentRow.add(currentCell);
                    }
                    currentCell = cell;
                    if (cols > 0 && rowColumnCount(currentRow) >= cols) {
                        rows.add(currentRow);
                        currentRow = new ArrayList<>();
                    }
                }
            } else if (currentCell != null) {
                // continuation of the current cell content
                currentCell.appendLine(line);
            } else {
                // content of a cell without a separator e.g. `| first cell`
                currentCell = new TableCell("", line);
            }
        }

        if (currentCell != null) {
            currentRow.add(currentCell);
        }
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }

        result.add(linePrefix + "<table>");
        for (List<TableCell> row : rows) {
            StringBuilder rowHtml = new StringBuilder(linePrefix + "<tr>");
            for (TableCell cell : row) {
                rowHtml.append(cell.toHtml());
            }
            rowHtml.append("</tr>");
            result.add(rowHtml.toString());
        }
        result.add(linePrefix + "</table>");

        return i;
    }

    /**
     * Cell spec tokens: {@code N+} colspan, {@code .N+} rowspan, {@code <}/{@code >}/{@code ^}
     * horizontal alignment, {@code .<}/{@code .>}/{@code .^} vertical alignment and the
     * single-letter styles {@code a}, {@code d}, {@code e}, {@code h}, {@code l}, {@code m}, {@code s}.
     */
    private static final Pattern TABLE_CELL_SPEC_TOKEN_PATTERN = Pattern
            .compile("\\.\\d+\\+|\\d+\\+|\\.[<>^]|[<>^]|[adehlms]");
    private static final Pattern TABLE_CELL_SPEC_PATTERN = Pattern
            .compile("^(\\.\\d+\\+|\\d+\\+|\\.[<>^]|[<>^]|[adehlms])+$");

    /**
     * Parses a line containing one or more cells, e.g. {@code .2+h!Strategy} or
     * {@code |Name |Type}. A cell is an optional spec immediately followed by the
     * separator and then the cell content; the spec of a subsequent cell is the
     * spec-character suffix preceding its separator (e.g. {@code a 2+|b}).
     *
     * @return the cells found, or an empty list if the line does not start a cell
     */
    private static List<TableCell> parseCellLine(String line, char separator) {
        List<TableCell> cells = new ArrayList<>();
        int n = line.length();

        int specEnd = 0;
        while (specEnd < n && TABLE_CELL_SPEC_CHARS.indexOf(line.charAt(specEnd)) >= 0) {
            specEnd++;
        }
        if (specEnd >= n || line.charAt(specEnd) != separator) {
            return cells;
        }
        String spec = line.substring(0, specEnd);
        if (!spec.isEmpty() && !TABLE_CELL_SPEC_PATTERN.matcher(spec).matches()) {
            // e.g. `hell!x`: not a valid cell spec so not a cell start
            return cells;
        }

        String pendingSpec = spec;
        int contentStart = specEnd + 1;

        while (true) {
            int nextSep = line.indexOf(separator, contentStart);
            if (nextSep < 0 || nextSep == n - 1) {
                // last cell of the line; a trailing separator is just a terminator
                cells.add(new TableCell(pendingSpec, nextSep < 0 ? line.substring(contentStart)
                        : line.substring(contentStart, nextSep)));
                break;
            }

            String segment = line.substring(contentStart, nextSep);
            int k = segment.length();
            while (k > 0 && TABLE_CELL_SPEC_CHARS.indexOf(segment.charAt(k - 1)) >= 0) {
                k--;
            }
            String suffix = segment.substring(k);
            if (!suffix.isEmpty() && !suffix.equals(segment)
                    && TABLE_CELL_SPEC_PATTERN.matcher(suffix).matches()) {
                cells.add(new TableCell(pendingSpec, segment.substring(0, k)));
                pendingSpec = suffix;
            } else {
                cells.add(new TableCell(pendingSpec, segment));
                pendingSpec = "";
            }
            contentStart = nextSep + 1;
        }

        return cells;
    }

    private static int rowColumnCount(List<TableCell> row) {
        int span = 0;
        for (TableCell cell : row) {
            span += cell.colspan;
        }
        return span;
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

    /**
     * A single Asciidoc table cell: an optional spec ({@code colspan}, {@code rowspan},
     * alignment and style markers such as {@code h} for header cells) followed by the
     * cell separator and the cell content.
     */
    private static class TableCell {

        private int colspan = 1;
        private int rowspan = 1;
        private String align = null;
        private String valign = null;
        private boolean header = false;
        private String styleTag = null;
        private final StringBuilder content = new StringBuilder();

        TableCell(String spec, String firstLine) {
            parseSpec(spec);
            content.append(firstLine);
        }

        void appendLine(String line) {
            content.append(' ').append(line.trim());
        }

        private void parseSpec(String spec) {
            Matcher specMatcher = TABLE_CELL_SPEC_TOKEN_PATTERN.matcher(spec);
            while (specMatcher.find()) {
                String token = specMatcher.group();
                if (token.endsWith("+")) {
                    int span = Integer.parseInt(token.substring(0, token.length() - 1).replace(".", ""));
                    if (token.startsWith(".")) {
                        rowspan = span;
                    } else {
                        colspan = span;
                    }
                } else if (token.startsWith(".")) {
                    switch (token.charAt(1)) {
                        case '<' -> valign = "top";
                        case '>' -> valign = "bottom";
                        case '^' -> valign = "middle";
                    }
                } else {
                    switch (token.charAt(0)) {
                        case '<' -> align = "left";
                        case '>' -> align = "right";
                        case '^' -> align = "center";
                        case 'h' -> header = true;
                        case 'e' -> styleTag = "em";
                        case 's' -> styleTag = "strong";
                        case 'm' -> styleTag = "code";
                        default -> {
                        }
                    }
                }
            }
        }

        String toHtml() {
            StringBuilder html = new StringBuilder();
            html.append(header ? "<th" : "<td");
            if (colspan > 1) {
                html.append(" colspan=\"").append(colspan).append('"');
            }
            if (rowspan > 1) {
                html.append(" rowspan=\"").append(rowspan).append('"');
            }
            if (align != null) {
                html.append(" align=\"").append(align).append('"');
            }
            if (valign != null) {
                html.append(" valign=\"").append(valign).append('"');
            }
            html.append('>');

            String text = applyInlineConversions(content.toString().trim());
            if (styleTag != null) {
                html.append('<').append(styleTag).append('>').append(text).append("</").append(styleTag).append('>');
            } else {
                html.append(text);
            }

            html.append(header ? "</th>" : "</td>");
            return html.toString();
        }
    }
}
