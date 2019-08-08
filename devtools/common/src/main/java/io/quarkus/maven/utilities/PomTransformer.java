package io.quarkus.maven.utilities;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A utility to transform {@code pom.xml} files on the DOM level while keeping the original comments and formatting also
 * on places where common {@code javax.xml.transform.Transformer} or {@code javax.xml.parsers.DocumentBuilder} based
 * solutions tend to fail, such as
 * <ul>
 * <li>Order of XML declaration attributes</li>
 * <li>Whitespace after the XML declaration</li>
 * <li>Line breaks between element attributes</li>
 * <li>File final whitespace</li>
 * </ul>
 */
public class PomTransformer {

    static final Pattern[] POSTPROCESS_PATTERNS = new Pattern[] { Pattern.compile("(<\\?xml[^>]*\\?>)?(\\s*)<"),
            Pattern.compile("(\\s*)<project([^>]*)>"), Pattern.compile("\\s*$") };
    static final Pattern EOL_PATTERN = Pattern.compile("\r?\n");

    private final Path path;
    private final Charset charset;

    public PomTransformer(Path path, Charset charset) {
        super();
        this.path = path;
        this.charset = charset;
    }

    /**
     * Loads the document under {@link #path}, applies the given {@code transformations}, mitigates the formatting
     * issues caused by {@link Transformer} and finally stores the document back to the file under {@link #path}.
     *
     * @param transformations the {@link Transformation}s to apply
     */
    public void transform(Transformation... transformations) {
        transform(Arrays.asList(transformations));
    }

    /**
     * Loads the document under {@link #path}, applies the given {@code transformations}, mitigates the formatting
     * issues caused by {@link Transformer} and finally stores the document back to the file under {@link #path}.
     *
     * @param transformations the {@link Transformation}s to apply
     */
    public void transform(Collection<Transformation> transformations) {
        transform(transformations, path, () -> {
            try {
                return new String(Files.readAllBytes(path), charset);
            } catch (IOException e) {
                throw new RuntimeException(String.format("Could not read DOM from [%s]", path), e);
            }
        }, xml -> {
            try {
                Files.write(path, xml.getBytes(charset));
            } catch (IOException e) {
                throw new RuntimeException(String.format("Could not write DOM from [%s]", path), e);
            }
        });
    }

    static void transform(Collection<Transformation> edits, Path path, Supplier<String> source,
            Consumer<String> outConsumer) {
        final String src = source.get();

        final Document document;
        try {
            final DOMResult domResult = new DOMResult();
            TransformerFactory.newInstance().newTransformer()
                    .transform(new StreamSource(new StringReader(source.get())), domResult);
            document = (Document) domResult.getNode();
        } catch (TransformerException | TransformerFactoryConfigurationError e) {
            throw new RuntimeException(String.format("Could not read DOM from [%s]", path), e);
        }

        final XPath xPath = XPathFactory.newInstance().newXPath();
        final TransformationContext context = new TransformationContext(path, document,
                detectIndentation(document, xPath), xPath);
        for (Transformation edit : edits) {
            edit.perform(document, context);
        }
        String result;
        try {
            StringWriter out = new StringWriter();
            TransformerFactory.newInstance().newTransformer().transform(new DOMSource(document), new StreamResult(out));
            result = out.toString();

        } catch (TransformerException | TransformerFactoryConfigurationError e) {
            throw new RuntimeException(String.format("Could not write DOM from [%s]", path), e);
        }

        final String eol = detectEol(src);
        result = EOL_PATTERN.matcher(result).replaceAll(eol);
        result = postprocess(src, result);
        outConsumer.accept(result);
    }

    static String postprocess(String src, String result) {
        for (Pattern p : POSTPROCESS_PATTERNS) {
            final Matcher srcMatcher = p.matcher(src);
            if (srcMatcher.find()) {
                final String replacement = Matcher.quoteReplacement(srcMatcher.group());
                result = p.matcher(result).replaceFirst(replacement);
            }
        }
        return result;
    }

    static String detectIndentation(Node document, XPath xPath) {
        try {
            final String ws = (String) xPath.evaluate(anyNs("project") + "/*[1]/preceding-sibling::text()[last()]",
                    document, XPathConstants.STRING);
            if (ws != null && !ws.isEmpty()) {
                int i = ws.length() - 1;
                LOOP: while (i >= 0) {
                    switch (ws.charAt(i)) {
                        case ' ':
                        case '\t':
                            i--;
                            break;
                        default:
                            break LOOP;
                    }
                }
                return ws.substring(i + 1);
            }
            return "    ";
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    static String detectEol(String src) {
        return src.indexOf('\r') >= 0 ? "\r\n" : "\n";
    }

    /**
     * A generator of XPath 1.0 "any namespace" selector, such as
     * {@code /*:[local-name()='foo']/*:[local-name()='bar']}. In XPath 2.0, this would be just {@code /*:foo/*:bar},
     * but as of Java 13, there is only XPath 1.0 available in the JDK.
     *
     * @param elements namespace-less element names
     * @return am XPath 1.0 style selector
     */
    static String anyNs(String... elements) {
        StringBuilder sb = new StringBuilder();
        for (String e : elements) {
            sb.append("/*[local-name()='").append(e).append("']");
        }
        return sb.toString();
    }

    /**
     * A context of a set of {@link Transformation}s.
     */
    public static class TransformationContext {
        private final Path pomXmlPath;
        private final Document document;
        private final XPath xPath;
        private final String indentationString;

        public TransformationContext(Path pomXmlPath, Document document, String indentationString, XPath xPath) {
            super();
            this.pomXmlPath = pomXmlPath;
            this.document = document;
            this.indentationString = indentationString;
            this.xPath = xPath;
        }

        /**
         * @return the path to the {@code pom.xml} file that is being transformed
         */
        public Path getPomXmlPath() {
            return pomXmlPath;
        }

        /**
         * @return an {@link XPath} instance that can be used for querying the DOM of the transformed {@code pom.xml}
         *         file
         */
        public XPath getXPath() {
            return xPath;
        }

        /**
         * @return an indentation string (without newline characters) as it was autodetected using
         *         {@link PomTransformer#detectIndentation(Node, XPath)}
         */
        public String getIndentationString() {
            return indentationString;
        }

        /**
         * @param indentCount
         * @return a new indentation node containing a newline and {@code indentCount} times concatenated
         *         {@link #indentationString}
         */
        public Node indent(int indentCount) {
            final StringBuilder sb = new StringBuilder(1 + indentCount * indentationString.length());
            sb.append('\n');
            for (int i = 0; i < indentCount; i++) {
                sb.append(indentationString);
            }
            return document.createTextNode(sb.toString());
        }

        public Node textElement(String elementName, String value) {
            final Node result = document.createElement(elementName);
            result.appendChild(document.createTextNode(value));
            return result;
        }

    }

    /**
     * A transformation of a DOM
     */
    public interface Transformation {

        public static Transformation addModule(String module) {
            return (Document document, TransformationContext context) -> {
                try {
                    Node modules = (Node) context.getXPath().evaluate(anyNs("project", "modules"), document,
                            XPathConstants.NODE);
                    if (modules == null) {
                        final Node modulesIndent = context.indent(1);
                        modules = document.createElement("modules");
                        modules.appendChild(context.indent(1));

                        final Node build = (Node) context.getXPath().evaluate(anyNs("project", "build"), document,
                                XPathConstants.NODE);
                        if (build != null) {
                            Node ws = build.getPreviousSibling();
                            if (ws == null || ws.getNodeType() != Node.TEXT_NODE) {
                                ws = context.indent(1);
                                build.getParentNode().insertBefore(ws, build);
                            }
                            build.getParentNode().insertBefore(modulesIndent, ws);
                            build.getParentNode().insertBefore(modules, ws);
                        } else {
                            final Node project = (Node) context.getXPath().evaluate(anyNs("project"), document,
                                    XPathConstants.NODE);
                            if (project == null) {
                                throw new IllegalStateException(
                                        String.format("No <project> in file [%s]", context.getPomXmlPath()));
                            }
                            final NodeList projectChildren = project.getChildNodes();
                            final int len = projectChildren.getLength();
                            Node ws = null;
                            if (len == 0 || (ws = projectChildren.item(len - 1)).getNodeType() != Node.TEXT_NODE) {
                                ws = document.createTextNode("\n");
                                project.appendChild(ws);
                            }
                            project.insertBefore(modulesIndent, ws);
                            project.insertBefore(modules, ws);
                        }
                    }

                    final Node moduleNode = document.createElement("module");
                    moduleNode.appendChild(document.createTextNode(module));

                    final NodeList modulesChildren = modules.getChildNodes();
                    final int len = modulesChildren.getLength();
                    Node ws;
                    if (len == 0 || (ws = modulesChildren.item(len - 1)).getNodeType() != Node.TEXT_NODE) {
                        ws = context.indent(1);
                        modules.appendChild(ws);
                    }
                    modules.insertBefore(context.indent(2), ws);
                    modules.insertBefore(moduleNode, ws);
                } catch (XPathExpressionException | DOMException e) {
                    throw new RuntimeException(e);
                }
            };
        }

        public static Transformation addDependencyManagementIfNeeded() {
            return (Document document, TransformationContext context) -> {
                try {
                    Node dependencyManagementDeps = (Node) context.getXPath().evaluate(
                            anyNs("project", "dependencyManagement", "dependencies"), document, XPathConstants.NODE);
                    if (dependencyManagementDeps == null) {
                        Node dependencyManagement = (Node) context.getXPath()
                                .evaluate(anyNs("project", "dependencyManagement"), document, XPathConstants.NODE);
                        if (dependencyManagement == null) {
                            Node project = (Node) context.getXPath().evaluate(anyNs("project"), document,
                                    XPathConstants.NODE);
                            if (project == null) {
                                throw new IllegalStateException(
                                        String.format("//project not found in [%s]", context.getPomXmlPath()));
                            }
                            /* ideally before dependencies */
                            Node refNode = (Node) context.getXPath().evaluate(anyNs("project", "dependencies"),
                                    document, XPathConstants.NODE);
                            if (refNode == null) {
                                /* or before build */
                                refNode = (Node) context.getXPath().evaluate(anyNs("project", "build"),
                                        document, XPathConstants.NODE);
                            }
                            dependencyManagement = document.createElement("dependencyManagement");
                            Node ws;
                            if (refNode != null) {
                                ws = refNode.getPreviousSibling();
                                if (ws == null || ws.getNodeType() != Node.TEXT_NODE) {
                                    project.insertBefore(ws = context.indent(1), refNode);
                                }
                            } else {
                                ws = project.getLastChild();
                                if (ws == null || ws.getNodeType() != Node.TEXT_NODE) {
                                    project.appendChild(ws = context.indent(0));
                                }
                            }
                            project.insertBefore(dependencyManagement, ws);
                            project.insertBefore(context.indent(1), dependencyManagement);
                        }
                        dependencyManagementDeps = document.createElement("dependencies");
                        dependencyManagementDeps.appendChild(context.indent(2));
                        Node ws = dependencyManagement.getLastChild();
                        if (ws == null || ws.getNodeType() != Node.TEXT_NODE) {
                            dependencyManagement.appendChild(ws = context.indent(1));
                        }
                        dependencyManagement.insertBefore(dependencyManagementDeps, ws);
                        dependencyManagement.insertBefore(context.indent(2), dependencyManagementDeps);
                    }
                } catch (XPathExpressionException | DOMException e) {
                    throw new RuntimeException(e);
                }
            };
        }

        public static Transformation addManagedDependency(String groupId, String artifactId, String version) {
            return addManagedDependency(groupId, artifactId, version, null, null);
        }

        public static Transformation importBom(String groupId, String artifactId, String version) {
            return addManagedDependency(groupId, artifactId, version, "pom", "import");
        }

        public static Transformation addManagedDependency(String groupId, String artifactId, String version, String type,
                String scope) {
            return (Document document, TransformationContext context) -> {
                try {
                    addDependencyManagementIfNeeded().perform(document, context);
                    Node dependencyManagementDeps = (Node) context.getXPath().evaluate(
                            anyNs("project", "dependencyManagement", "dependencies"), document, XPathConstants.NODE);
                    final NodeList dependencyManagementDepsChildren = dependencyManagementDeps.getChildNodes();
                    Node ws = null;
                    if (dependencyManagementDepsChildren.getLength() > 0) {
                        ws = dependencyManagementDepsChildren.item(dependencyManagementDepsChildren.getLength() - 1);
                    }
                    if (ws == null || ws.getNodeType() != Node.TEXT_NODE) {
                        ws = context.indent(3);
                        dependencyManagementDeps.appendChild(ws);
                    }

                    dependencyManagementDeps.insertBefore(context.indent(3), ws);
                    final Node dep = document.createElement("dependency");
                    dep.appendChild(context.indent(4));
                    dep.appendChild(context.textElement("groupId", groupId));
                    dep.appendChild(context.indent(4));
                    dep.appendChild(context.textElement("artifactId", artifactId));
                    dep.appendChild(context.indent(4));
                    dep.appendChild(context.textElement("version", version));
                    if (type != null) {
                        dep.appendChild(context.indent(4));
                        dep.appendChild(context.textElement("type", type));
                    }
                    if (scope != null) {
                        dep.appendChild(context.indent(4));
                        dep.appendChild(context.textElement("scope", scope));
                    }
                    dep.appendChild(context.indent(3));
                    dependencyManagementDeps.insertBefore(dep, ws);
                } catch (XPathExpressionException | DOMException e) {
                    throw new RuntimeException(e);
                }
            };
        }

        /**
         * Perform this {@link Transformation} on the given {@code document}
         *
         * @param document the {@link Document} to transform
         * @param context the current {@link TransformationContext}
         */
        void perform(Document document, TransformationContext context);

    }
}
