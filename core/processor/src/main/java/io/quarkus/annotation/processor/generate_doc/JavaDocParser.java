package io.quarkus.annotation.processor.generate_doc;

import static io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil.hyphenate;

import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import com.github.javaparser.javadoc.description.JavadocDescription;
import com.github.javaparser.javadoc.description.JavadocInlineTag;

import io.quarkus.annotation.processor.Constants;

final class JavaDocParser {
    private static final String HASH = "#";
    private static final String STAR = "*";
    private static final String S_NODE = "s";
    private static final String UNDERSCORE = "_";
    private static final String NEW_LINE = "\n";
    private static final String LINK_NODE = "a";
    private static final String BOLD_NODE = "b";
    private static final String BIG_NODE = "big";
    private static final String DEL_NODE = "del";
    private static final String ITALICS_NODE = "i";
    private static final String TEXT_NODE = "#text";
    private static final String UNDERLINE_NODE = "u";
    private static final String NEW_LINE_NODE = "br";
    private static final String PARAGRAPH_NODE = "p";
    private static final String SMALL_NODE = "small";
    private static final String EMPHASIS_NODE = "em";
    private static final String LIST_ITEM_NODE = "li";
    private static final String HREF_ATTRIBUTE = "href";
    private static final String STRIKE_NODE = "strike";
    private static final String SUB_SCRIPT_NODE = "sub";
    private static final String ORDERED_LIST_NODE = "ol";
    private static final String SUPER_SCRIPT_NODE = "sup";
    private static final String UN_ORDERED_LIST_NODE = "ul";

    private static final String INLINE_JAVA_DOC_TAG_FORMAT = "`%s`";

    private static final String BIG_ASCIDOC_STYLE = "[.big]";
    private static final String LINK_ATTRIBUTE_FORMAT = "[%s]";
    private static final String SUB_SCRIPT_ASCIDOC_STYLE = "~";
    private static final String SUPER_SCRIPT_ASCIDOC_STYLE = "^";
    private static final String SMALL_ASCIDOC_STYLE = "[.small]";
    private static final String ORDERED_LIST_ITEM_ASCIDOC_STYLE = " . ";
    private static final String UNORDERED_LIST_ITEM_ASCIDOC_STYLE = " - ";
    private static final String UNDERLINE_ASCIDOC_STYLE = "[.underline]";
    private static final String LINE_THROUGH_ASCIDOC_STYLE = "[.line-through]";

    public String parse(String javaDoc) {
        if (javaDoc == null) {
            return Constants.EMPTY;
        }

        final Document document = Jsoup.parse(javaDoc);

        final StringBuilder docBuilder = new StringBuilder();
        parseJavaDoc(document.body(), docBuilder);

        final String doc = docBuilder.toString();
        if (doc.trim().isEmpty()) {
            return Constants.EMPTY;
        }

        return doc;
    }

    private void parseJavaDoc(Node root, StringBuilder docBuilder) {
        for (Node node : root.childNodes()) {
            switch (node.nodeName()) {
                case PARAGRAPH_NODE:
                    docBuilder.append(NEW_LINE);
                    parseJavaDoc(node, docBuilder);
                    break;
                case ORDERED_LIST_NODE:
                case UN_ORDERED_LIST_NODE:
                    parseJavaDoc(node, docBuilder);
                    break;
                case LIST_ITEM_NODE:
                    final String marker = node.parent().nodeName().equals(ORDERED_LIST_NODE)
                            ? ORDERED_LIST_ITEM_ASCIDOC_STYLE
                            : UNORDERED_LIST_ITEM_ASCIDOC_STYLE;
                    docBuilder.append(NEW_LINE);
                    docBuilder.append(marker);
                    parseJavaDoc(node, docBuilder);
                    break;
                case LINK_NODE:
                    final String link = node.attr(HREF_ATTRIBUTE);
                    docBuilder.append(link);
                    final StringBuilder caption = new StringBuilder();
                    parseJavaDoc(node, caption);
                    docBuilder.append(String.format(LINK_ATTRIBUTE_FORMAT, caption.toString().trim()));
                    break;
                case BOLD_NODE:
                case EMPHASIS_NODE:
                    docBuilder.append(STAR);
                    parseJavaDoc(node, docBuilder);
                    docBuilder.append(STAR);
                    break;
                case ITALICS_NODE:
                    docBuilder.append(UNDERSCORE);
                    parseJavaDoc(node, docBuilder);
                    docBuilder.append(UNDERSCORE);
                    break;
                case UNDERLINE_NODE:
                    docBuilder.append(UNDERLINE_ASCIDOC_STYLE);
                    docBuilder.append(HASH);
                    parseJavaDoc(node, docBuilder);
                    docBuilder.append(HASH);
                    break;
                case SMALL_NODE:
                    docBuilder.append(SMALL_ASCIDOC_STYLE);
                    docBuilder.append(HASH);
                    parseJavaDoc(node, docBuilder);
                    docBuilder.append(HASH);
                    break;
                case BIG_NODE:
                    docBuilder.append(BIG_ASCIDOC_STYLE);
                    docBuilder.append(HASH);
                    parseJavaDoc(node, docBuilder);
                    docBuilder.append(HASH);
                    break;
                case SUB_SCRIPT_NODE:
                    docBuilder.append(SUB_SCRIPT_ASCIDOC_STYLE);
                    parseJavaDoc(node, docBuilder);
                    docBuilder.append(SUB_SCRIPT_ASCIDOC_STYLE);
                    break;
                case SUPER_SCRIPT_NODE:
                    docBuilder.append(SUPER_SCRIPT_ASCIDOC_STYLE);
                    parseJavaDoc(node, docBuilder);
                    docBuilder.append(SUPER_SCRIPT_ASCIDOC_STYLE);
                    break;
                case DEL_NODE:
                case S_NODE:
                case STRIKE_NODE:
                    docBuilder.append(LINE_THROUGH_ASCIDOC_STYLE);
                    docBuilder.append(HASH);
                    parseJavaDoc(node, docBuilder);
                    docBuilder.append(HASH);
                    break;
                case NEW_LINE_NODE:
                    docBuilder.append(NEW_LINE);
                    break;
                case TEXT_NODE:
                    final TextNode textNode = (TextNode) node;
                    docBuilder.append(getJavaDocDescription(textNode));
                    break;
                default: {
                    parseJavaDoc(node, docBuilder);
                }
            }
        }
    }

    private String getJavaDocDescription(TextNode node) {
        JavadocDescription javadocDescription = JavadocDescription.parseText(node.text());

        return javadocDescription
                .getElements()
                .stream()
                .map((javadocDescriptionElement) -> {
                    if (javadocDescriptionElement instanceof JavadocInlineTag) {
                        JavadocInlineTag inlineTag = (JavadocInlineTag) javadocDescriptionElement;
                        String content = inlineTag.getContent().trim();
                        switch (inlineTag.getType()) {
                            case CODE:
                            case VALUE:
                            case LITERAL:
                            case SYSTEM_PROPERTY:
                                return String.format(INLINE_JAVA_DOC_TAG_FORMAT, content);
                            case LINK:
                            case LINKPLAIN:
                                if (content.startsWith(HASH)) {
                                    content = hyphenate(content.substring(1));
                                }
                                return String.format(INLINE_JAVA_DOC_TAG_FORMAT, content);
                            default:
                                return content;
                        }
                    } else {
                        return javadocDescriptionElement.toText();
                    }
                }).collect(Collectors.joining());
    }
}
