package io.quarkus.qute.runtime;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import io.quarkus.qute.ParserConfig;
import io.quarkus.qute.runtime.QuteRecorder.TemplateInfo;

/**
 * The template data that grows with the number of templates. The build writes it to a generated
 * resource instead of recording it, so that an application with tens of thousands of templates
 * does not exceed the constant pool of one generated class.
 * <p>
 * The resource is UTF-8 text, so that it can be inspected for debugging:
 *
 * <pre>
 * qute-context-data 1
 * template items.html
 * source file:///app/src/main/resources/templates/items.html
 * template tags/hello.html
 * content 12
 * Hello {name}
 * variant items items.html items.txt
 * parser-config = items.html
 * </pre>
 *
 * A {@code content} line gives the number of characters of the template content, which follows on
 * the next lines. In the other lines, the values are separated by a space, and {@code %}, the
 * whitespace and the control characters in a value are percent-encoded.
 *
 * @param templates the templates by path
 * @param variants the variants by base path
 * @param nonDefaultParserConfigs the template paths of each non-default parser config
 */
public record QuteContextData(Map<String, TemplateInfo> templates, Map<String, List<String>> variants,
        Map<ParserConfig, Set<String>> nonDefaultParserConfigs) {

    /**
     * The name of the generated resource.
     */
    public static final String RESOURCE = "META-INF/quarkus-qute-context.txt";

    private static final String HEADER = "qute-context-data 1";
    private static final String TEMPLATE = "template";
    private static final String SOURCE = "source";
    private static final String CONTENT = "content";
    private static final String VARIANT = "variant";
    private static final String PARSER_CONFIG = "parser-config";
    private static final String DEFAULT_COMMAND = "-";

    /**
     * Write the data in a stable order, so that the same templates give the same bytes.
     *
     * @return the UTF-8 text of the resource
     */
    public byte[] write() {
        StringBuilder out = new StringBuilder();
        out.append(HEADER).append('\n');
        for (TemplateInfo template : new TreeMap<>(templates).values()) {
            line(out, TEMPLATE, List.of(template.path));
            if (template.source != null) {
                line(out, SOURCE, List.of(template.source));
            }
            if (template.content != null) {
                out.append(CONTENT).append(' ').append(template.content.length()).append('\n')
                        .append(template.content).append('\n');
            }
        }
        for (Map.Entry<String, List<String>> e : new TreeMap<>(variants).entrySet()) {
            List<String> values = new ArrayList<>(e.getValue().size() + 1);
            values.add(e.getKey());
            values.addAll(e.getValue());
            line(out, VARIANT, values);
        }
        List<Map.Entry<ParserConfig, Set<String>>> configs = new ArrayList<>(nonDefaultParserConfigs.entrySet());
        configs.sort(Comparator.comparing(e -> command(e.getKey())));
        for (Map.Entry<ParserConfig, Set<String>> e : configs) {
            out.append(PARSER_CONFIG).append(' ').append(command(e.getKey()));
            for (String path : new TreeSet<>(e.getValue())) {
                out.append(' ').append(encode(path));
            }
            out.append('\n');
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Read the data that {@link #write()} produced.
     *
     * @param bytes the UTF-8 text of the resource
     * @return the data
     * @throws IllegalStateException if the text is not valid
     */
    public static QuteContextData read(byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8);
        Map<String, TemplateInfo> templates = new HashMap<>();
        Map<String, List<String>> variants = new HashMap<>();
        Map<ParserConfig, Set<String>> configs = new HashMap<>();
        int end = text.indexOf('\n');
        if (end < 0 || !text.substring(0, end).equals(HEADER)) {
            throw invalid("missing or unsupported header");
        }
        int pos = end + 1;
        String path = null;
        String source = null;
        String content = null;
        while (pos < text.length()) {
            end = text.indexOf('\n', pos);
            if (end < 0) {
                throw invalid("missing end of line");
            }
            String line = text.substring(pos, end);
            pos = end + 1;
            String[] parts = line.split(" ", -1);
            switch (parts[0]) {
                case TEMPLATE -> {
                    if (path != null) {
                        templates.put(path, new TemplateInfo(path, source, content));
                    }
                    path = decode(value(parts));
                    source = null;
                    content = null;
                }
                case SOURCE -> {
                    inTemplate(path, line);
                    source = decode(value(parts));
                }
                case CONTENT -> {
                    inTemplate(path, line);
                    int length = length(value(parts));
                    if (length < 0 || (long) pos + length >= text.length() || text.charAt(pos + length) != '\n') {
                        throw invalid("the content of " + path + " does not match its length");
                    }
                    content = text.substring(pos, pos + length);
                    pos += length + 1;
                }
                case VARIANT -> {
                    atLeastOneValue(parts);
                    List<String> values = new ArrayList<>(parts.length - 2);
                    for (int i = 2; i < parts.length; i++) {
                        values.add(decode(parts[i]));
                    }
                    variants.put(decode(parts[1]), values);
                }
                case PARSER_CONFIG -> {
                    atLeastOneValue(parts);
                    Set<String> paths = new HashSet<>();
                    for (int i = 2; i < parts.length; i++) {
                        paths.add(decode(parts[i]));
                    }
                    Character command = parts[1].equals(DEFAULT_COMMAND) ? null : character(decode(parts[1]));
                    configs.put(parserConfig(command), paths);
                }
                default -> throw invalid("unknown line: " + line);
            }
        }
        if (path != null) {
            templates.put(path, new TemplateInfo(path, source, content));
        }
        return new QuteContextData(templates, variants, configs);
    }

    private static void line(StringBuilder out, String keyword, Collection<String> values) {
        out.append(keyword);
        for (String value : values) {
            out.append(' ').append(encode(value));
        }
        out.append('\n');
    }

    private static void inTemplate(String path, String line) {
        if (path == null) {
            throw invalid("no template before: " + line);
        }
    }

    private static void atLeastOneValue(String[] parts) {
        if (parts.length < 2) {
            throw invalid("expected at least one value: " + parts[0]);
        }
    }

    private static int length(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw invalid("invalid content length: " + value);
        }
    }

    private static ParserConfig parserConfig(Character command) {
        try {
            return new ParserConfig(command);
        } catch (IllegalArgumentException e) {
            throw invalid("invalid expression command: " + e.getMessage());
        }
    }

    private static char character(String value) {
        if (value.length() != 1) {
            throw invalid("invalid expression command: " + value);
        }
        return value.charAt(0);
    }

    private static String value(String[] parts) {
        if (parts.length != 2) {
            throw invalid("expected one value: " + String.join(" ", parts));
        }
        return parts[1];
    }

    private static String command(ParserConfig config) {
        Character command = config.expressionCommand();
        if (command == null) {
            return DEFAULT_COMMAND;
        }
        String encoded = encode(command.toString());
        return encoded.equals(DEFAULT_COMMAND) ? "%2d" : encoded;
    }

    private static String encode(String value) {
        StringBuilder encoded = null;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '%' || c <= ' ' || c == 0x7F) {
                if (encoded == null) {
                    encoded = new StringBuilder(value.length() + 8).append(value, 0, i);
                }
                encoded.append('%').append(Character.forDigit(c >> 4, 16)).append(Character.forDigit(c & 0xF, 16));
            } else if (encoded != null) {
                encoded.append(c);
            }
        }
        return encoded == null ? value : encoded.toString();
    }

    private static String decode(String value) {
        if (value.indexOf('%') < 0) {
            return value;
        }
        StringBuilder decoded = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '%') {
                decoded.append(c);
            } else if (i + 2 < value.length() && Character.digit(value.charAt(i + 1), 16) >= 0
                    && Character.digit(value.charAt(i + 2), 16) >= 0) {
                decoded.append((char) Integer.parseInt(value.substring(i + 1, i + 3), 16));
                i += 2;
            } else {
                throw invalid("invalid percent-encoding: " + value);
            }
        }
        return decoded.toString();
    }

    private static IllegalStateException invalid(String reason) {
        return new IllegalStateException("Invalid " + RESOURCE + ": " + reason);
    }

}
