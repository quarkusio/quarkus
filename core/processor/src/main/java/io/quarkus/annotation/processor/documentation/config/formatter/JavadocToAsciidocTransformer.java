package io.quarkus.annotation.processor.documentation.config.formatter;

import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.description.JavadocDescriptionElement;
import com.github.javaparser.javadoc.description.JavadocInlineTag;

import io.quarkus.annotation.processor.documentation.config.model.JavadocFormat;
import io.quarkus.annotation.processor.documentation.config.util.ConfigNamingUtil;

public final class JavadocToAsciidocTransformer {

    private static final Pattern START_OF_LINE = Pattern.compile("^", Pattern.MULTILINE);
    private static final Pattern STARTING_SPACE = Pattern.compile("^ +");

    private static final String BACKTICK = "`";
    private static final String HASH = "#";
    private static final String STAR = "*";
    private static final String S_NODE = "s";
    private static final String UNDERSCORE = "_";
    private static final String NEW_LINE = "\n";
    private static final String LINK_NODE = "a";
    private static final String BOLD_NODE = "b";
    private static final String STRONG_NODE = "strong";
    private static final String BIG_NODE = "big";
    private static final String CODE_NODE = "code";
    private static final String DEL_NODE = "del";
    private static final String ITALICS_NODE = "i";
    private static final String EMPHASIS_NODE = "em";
    private static final String TEXT_NODE = "#text";
    private static final String UNDERLINE_NODE = "u";
    private static final String NEW_LINE_NODE = "br";
    private static final String PARAGRAPH_NODE = "p";
    private static final String SMALL_NODE = "small";
    private static final String LIST_ITEM_NODE = "li";
    private static final String HREF_ATTRIBUTE = "href";
    private static final String STRIKE_NODE = "strike";
    private static final String SUB_SCRIPT_NODE = "sub";
    private static final String ORDERED_LIST_NODE = "ol";
    private static final String SUPER_SCRIPT_NODE = "sup";
    private static final String UN_ORDERED_LIST_NODE = "ul";
    private static final String PREFORMATED_NODE = "pre";
    private static final String BLOCKQUOTE_NODE = "blockquote";

    private static final String BIG_ASCIDOC_STYLE = "[.big]";
    private static final String LINK_ATTRIBUTE_FORMAT = "[%s]";
    private static final String SUB_SCRIPT_ASCIDOC_STYLE = "~";
    private static final String SUPER_SCRIPT_ASCIDOC_STYLE = "^";
    private static final String SMALL_ASCIDOC_STYLE = "[.small]";
    private static final String ORDERED_LIST_ITEM_ASCIDOC_STYLE = " . ";
    private static final String UNORDERED_LIST_ITEM_ASCIDOC_STYLE = " - ";
    private static final String UNDERLINE_ASCIDOC_STYLE = "[.underline]";
    private static final String LINE_THROUGH_ASCIDOC_STYLE = "[.line-through]";
    private static final String HARD_LINE_BREAK_ASCIDOC_STYLE = " +\n";
    private static final String CODE_BLOCK_ASCIDOC_STYLE = "```";
    private static final String BLOCKQUOTE_BLOCK_ASCIDOC_STYLE = "[quote]\n____";
    private static final String BLOCKQUOTE_BLOCK_ASCIDOC_STYLE_END = "____";

    private JavadocToAsciidocTransformer() {
    }

    public static String toAsciidoc(String javadoc, JavadocFormat format) {
        return toAsciidoc(javadoc, format, false);
    }

    public static String toAsciidoc(String javadoc, JavadocFormat format, boolean inlineMacroMode) {
        if (javadoc == null || javadoc.isBlank()) {
            return null;
        }

        if (format == JavadocFormat.ASCIIDOC) {
            return javadoc;
        } else if (format == JavadocFormat.MARKDOWN) {
            throw new IllegalArgumentException("Conversion from Markdown to Asciidoc is not supported");
        }

        // the parser expects all the lines to start with "* "
        // we add it as it has been previously removed
        Javadoc parsedJavadoc = StaticJavaParser.parseJavadoc(START_OF_LINE.matcher(javadoc).replaceAll("* "));

        StringBuilder sb = new StringBuilder();

        for (JavadocDescriptionElement javadocDescriptionElement : parsedJavadoc.getDescription().getElements()) {
            if (javadocDescriptionElement instanceof JavadocInlineTag) {
                JavadocInlineTag inlineTag = (JavadocInlineTag) javadocDescriptionElement;
                String content = inlineTag.getContent().trim();
                switch (inlineTag.getType()) {
                    case CODE:
                    case VALUE:
                    case LITERAL:
                    case SYSTEM_PROPERTY:
                        sb.append('`');
                        appendEscapedAsciiDoc(sb, content, inlineMacroMode);
                        sb.append('`');
                        break;
                    case LINK:
                    case LINKPLAIN:
                        if (content.startsWith(HASH)) {
                            content = ConfigNamingUtil.hyphenate(content.substring(1));
                        }
                        sb.append('`');
                        appendEscapedAsciiDoc(sb, content, inlineMacroMode);
                        sb.append('`');
                        break;
                    default:
                        sb.append(content);
                        break;
                }
            } else {
                appendHtml(sb, Jsoup.parseBodyFragment(javadocDescriptionElement.toText()), inlineMacroMode);
            }
        }

        String asciidoc = trim(sb);

        return asciidoc.isBlank() ? null : asciidoc;
    }

    private static void appendHtml(StringBuilder sb, Node node, boolean inlineMacroMode) {
        for (Node childNode : node.childNodes()) {
            switch (childNode.nodeName()) {
                case PARAGRAPH_NODE:
                    newLine(sb);
                    newLine(sb);
                    appendHtml(sb, childNode, inlineMacroMode);
                    break;
                case PREFORMATED_NODE:
                    newLine(sb);
                    newLine(sb);
                    sb.append(CODE_BLOCK_ASCIDOC_STYLE);
                    newLine(sb);
                    for (Node grandChildNode : childNode.childNodes()) {
                        unescapeHtmlEntities(sb, grandChildNode.toString());
                    }
                    newLineIfNeeded(sb);
                    sb.append(CODE_BLOCK_ASCIDOC_STYLE);
                    newLine(sb);
                    newLine(sb);
                    break;
                case BLOCKQUOTE_NODE:
                    newLine(sb);
                    newLine(sb);
                    sb.append(BLOCKQUOTE_BLOCK_ASCIDOC_STYLE);
                    newLine(sb);
                    appendHtml(sb, childNode, inlineMacroMode);
                    newLineIfNeeded(sb);
                    sb.append(BLOCKQUOTE_BLOCK_ASCIDOC_STYLE_END);
                    newLine(sb);
                    newLine(sb);
                    break;
                case ORDERED_LIST_NODE:
                case UN_ORDERED_LIST_NODE:
                    newLine(sb);
                    appendHtml(sb, childNode, inlineMacroMode);
                    break;
                case LIST_ITEM_NODE:
                    final String marker = childNode.parent().nodeName().equals(ORDERED_LIST_NODE)
                            ? ORDERED_LIST_ITEM_ASCIDOC_STYLE
                            : UNORDERED_LIST_ITEM_ASCIDOC_STYLE;
                    newLine(sb);
                    sb.append(marker);
                    appendHtml(sb, childNode, inlineMacroMode);
                    break;
                case LINK_NODE:
                    final String link = childNode.attr(HREF_ATTRIBUTE);
                    sb.append("link:");
                    sb.append(link);
                    final StringBuilder caption = new StringBuilder();
                    appendHtml(caption, childNode, inlineMacroMode);
                    sb.append(String.format(LINK_ATTRIBUTE_FORMAT, trim(caption)));
                    break;
                case CODE_NODE:
                    sb.append(BACKTICK);
                    appendHtml(sb, childNode, inlineMacroMode);
                    sb.append(BACKTICK);
                    break;
                case BOLD_NODE:
                case STRONG_NODE:
                    sb.append(STAR);
                    appendHtml(sb, childNode, inlineMacroMode);
                    sb.append(STAR);
                    break;
                case EMPHASIS_NODE:
                case ITALICS_NODE:
                    sb.append(UNDERSCORE);
                    appendHtml(sb, childNode, inlineMacroMode);
                    sb.append(UNDERSCORE);
                    break;
                case UNDERLINE_NODE:
                    sb.append(UNDERLINE_ASCIDOC_STYLE);
                    sb.append(HASH);
                    appendHtml(sb, childNode, inlineMacroMode);
                    sb.append(HASH);
                    break;
                case SMALL_NODE:
                    sb.append(SMALL_ASCIDOC_STYLE);
                    sb.append(HASH);
                    appendHtml(sb, childNode, inlineMacroMode);
                    sb.append(HASH);
                    break;
                case BIG_NODE:
                    sb.append(BIG_ASCIDOC_STYLE);
                    sb.append(HASH);
                    appendHtml(sb, childNode, inlineMacroMode);
                    sb.append(HASH);
                    break;
                case SUB_SCRIPT_NODE:
                    sb.append(SUB_SCRIPT_ASCIDOC_STYLE);
                    appendHtml(sb, childNode, inlineMacroMode);
                    sb.append(SUB_SCRIPT_ASCIDOC_STYLE);
                    break;
                case SUPER_SCRIPT_NODE:
                    sb.append(SUPER_SCRIPT_ASCIDOC_STYLE);
                    appendHtml(sb, childNode, inlineMacroMode);
                    sb.append(SUPER_SCRIPT_ASCIDOC_STYLE);
                    break;
                case DEL_NODE:
                case S_NODE:
                case STRIKE_NODE:
                    sb.append(LINE_THROUGH_ASCIDOC_STYLE);
                    sb.append(HASH);
                    appendHtml(sb, childNode, inlineMacroMode);
                    sb.append(HASH);
                    break;
                case NEW_LINE_NODE:
                    sb.append(HARD_LINE_BREAK_ASCIDOC_STYLE);
                    break;
                case TEXT_NODE:
                    String text = ((TextNode) childNode).text();

                    if (text.isEmpty()) {
                        break;
                    }

                    // Indenting the first line of a paragraph by one or more spaces makes the block literal
                    // Please see https://docs.asciidoctor.org/asciidoc/latest/verbatim/literal-blocks/ for more info
                    // This prevents literal blocks f.e. after <br>
                    final var startingSpaceMatcher = STARTING_SPACE.matcher(text);
                    if (sb.length() > 0 && '\n' == sb.charAt(sb.length() - 1) && startingSpaceMatcher.find()) {
                        text = startingSpaceMatcher.replaceFirst("");
                    }

                    appendEscapedAsciiDoc(sb, text, inlineMacroMode);
                    break;
                default:
                    appendHtml(sb, childNode, inlineMacroMode);
                    break;
            }
        }
    }

    /**
     * Trim the content of the given {@link StringBuilder} holding also AsciiDoc had line break {@code " +\n"}
     * for whitespace in addition to characters <= {@code ' '}.
     *
     * @param sb the {@link StringBuilder} to trim
     * @return the trimmed content of the given {@link StringBuilder}
     */
    static String trim(StringBuilder sb) {
        int length = sb.length();
        int offset = 0;
        while (offset < length) {
            final char ch = sb.charAt(offset);
            if (ch == ' '
                    && offset + 2 < length
                    && sb.charAt(offset + 1) == '+'
                    && sb.charAt(offset + 2) == '\n') {
                /* Space followed by + and newline is AsciiDoc hard break that we consider whitespace */
                offset += 3;
                continue;
            } else if (ch > ' ') {
                /* Non-whitespace as defined by String.trim() */
                break;
            }
            offset++;
        }
        if (offset > 0) {
            sb.delete(0, offset);
        }
        if (sb.length() > 0) {
            offset = sb.length() - 1;
            while (offset >= 0) {
                final char ch = sb.charAt(offset);
                if (ch == '\n'
                        && offset - 2 >= 0
                        && sb.charAt(offset - 1) == '+'
                        && sb.charAt(offset - 2) == ' ') {
                    /* Space followed by + is AsciiDoc hard break that we consider whitespace */
                    offset -= 3;
                    continue;
                } else if (ch > ' ') {
                    /* Non-whitespace as defined by String.trim() */
                    break;
                }
                offset--;
            }
            if (offset < sb.length() - 1) {
                sb.setLength(offset + 1);
            }
        }
        return sb.toString();
    }

    private static StringBuilder newLineIfNeeded(StringBuilder sb) {
        trimText(sb, " \t\r\n");
        return sb.append(NEW_LINE);
    }

    private static StringBuilder newLine(StringBuilder sb) {
        /* Trim trailing spaces and tabs at the end of line */
        trimText(sb, " \t");
        return sb.append(NEW_LINE);
    }

    private static StringBuilder trimText(StringBuilder sb, String charsToTrim) {
        while (sb.length() > 0 && charsToTrim.indexOf(sb.charAt(sb.length() - 1)) >= 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb;
    }

    private static StringBuilder unescapeHtmlEntities(StringBuilder sb, String text) {
        int i = 0;
        /* trim leading whitespace */
        LOOP: while (i < text.length()) {
            switch (text.charAt(i++)) {
                case ' ':
                case '\t':
                case '\r':
                case '\n':
                    break;
                default:
                    i--;
                    break LOOP;
            }
        }
        for (; i < text.length(); i++) {
            final char ch = text.charAt(i);
            switch (ch) {
                case '&':
                    int start = ++i;
                    while (i < text.length() && text.charAt(i) != ';') {
                        i++;
                    }
                    if (i > start) {
                        final String abbrev = text.substring(start, i);
                        switch (abbrev) {
                            case "lt":
                                sb.append('<');
                                break;
                            case "gt":
                                sb.append('>');
                                break;
                            case "nbsp":
                                sb.append("{nbsp}");
                                break;
                            case "amp":
                                sb.append('&');
                                break;
                            default:
                                try {
                                    int code = Integer.parseInt(abbrev);
                                    sb.append((char) code);
                                } catch (NumberFormatException e) {
                                    throw new RuntimeException(
                                            "Could not parse HTML entity &" + abbrev + "; in\n\n" + text + "\n\n");
                                }
                                break;
                        }
                    }
                    break;
                case '\r':
                    if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                        /* Ignore \r followed by \n */
                    } else {
                        /* A Mac single \r: replace by \n */
                        sb.append('\n');
                    }
                    break;
                default:
                    sb.append(ch);

            }
        }
        return sb;
    }

    private static StringBuilder appendEscapedAsciiDoc(StringBuilder sb, String text, boolean inlineMacroMode) {
        boolean escaping = false;
        for (int i = 0; i < text.length(); i++) {
            final char ch = text.charAt(i);
            switch (ch) {
                case ']':
                    // don't escape closing square bracket in the attribute list of an inline element with passThrough
                    // https://docs.asciidoctor.org/asciidoc/latest/attributes/positional-and-named-attributes/#substitutions
                    if (inlineMacroMode) {
                        if (escaping) {
                            sb.append("++");
                            escaping = false;
                        }
                        sb.append("&#93;");
                        break;
                    }
                case '#':
                case '*':
                case '\\':
                case '{':
                case '}':
                case '[':
                case '|':
                    if (!escaping) {
                        sb.append("++");
                        escaping = true;
                    }
                    sb.append(ch);
                    break;
                case '+':
                    if (escaping) {
                        sb.append("++");
                        escaping = false;
                    }
                    sb.append("{plus}");
                    break;
                default:
                    if (escaping) {
                        sb.append("++");
                        escaping = false;
                    }
                    sb.append(ch);
            }
        }
        if (escaping) {
            sb.append("++");
        }
        return sb;
    }
}
