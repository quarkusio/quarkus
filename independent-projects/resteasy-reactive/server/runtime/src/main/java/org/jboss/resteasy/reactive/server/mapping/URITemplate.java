package org.jboss.resteasy.reactive.server.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class URITemplate implements Dumpable, Comparable<URITemplate> {

    public final String template;

    public final String stem;

    /**
     * The number of characters that are literals in the path. According to the spec we need to sort by this.
     */
    public final int literalCharacterCount;

    public final int capturingGroups;

    public final int complexExpressions;

    /**
     * The components, first one is always the stem, so if the stem has been matched it can be ignored
     */
    public final TemplateComponent[] components;

    public final boolean prefixMatch;

    public URITemplate(String template, boolean prefixMatch) {
        this.prefixMatch = prefixMatch;
        if (!template.startsWith("/")) {
            template = "/" + template;
        }
        this.template = template;
        List<TemplateComponent> components = new ArrayList<>();
        String name = null;
        String stem = null;
        int litChars = 0;
        int capGroups = 0;
        int complexGroups = 0;
        int bracesCount = 0;
        StringBuilder sb = new StringBuilder();
        int state = 0; //0 = start, 1 = parsing name, 2 = parsing regex
        for (int i = 0; i < template.length(); ++i) {
            char c = template.charAt(i);
            switch (state) {
                case 0:
                    if (c == '{') {
                        state = 1;
                        if (sb.length() > 0) {
                            String literal = sb.toString();
                            if (components.isEmpty()) {
                                stem = literal;
                            }
                            components.add(new TemplateComponent(Type.LITERAL, literal, null, null, null));
                        }
                        sb.setLength(0);
                    } else {
                        litChars++;
                        sb.append(c);
                    }
                    break;
                case 1:
                    if (c == '}') {
                        state = 0;
                        if (sb.length() > 0) {
                            capGroups++;
                            if (i + 1 == template.length() || template.charAt(i + 1) == '/') {
                                components.add(new TemplateComponent(Type.DEFAULT_REGEX, null, sb.toString(), null, null));
                            } else {
                                components.add(new TemplateComponent(Type.CUSTOM_REGEX, "[^/]+?", sb.toString(),
                                        null, null));
                            }
                        } else {
                            throw new IllegalArgumentException("Invalid template " + template);
                        }
                        sb.setLength(0);
                    } else if (c == ':') {
                        name = sb.toString();
                        sb.setLength(0);
                        state = 2;
                    } else {
                        sb.append(c);
                    }
                    break;
                case 2:
                    if (c == '}' && bracesCount == 0) {
                        state = 0;
                        if (sb.length() > 0) {
                            capGroups++;
                            complexGroups++;
                            components
                                    .add(new TemplateComponent(Type.CUSTOM_REGEX, sb.toString(), name, null,
                                            null));
                        } else {
                            throw new IllegalArgumentException("Invalid template " + template);
                        }
                        sb.setLength(0);
                    } else {
                        sb.append(c);
                        if (c == '{') {
                            bracesCount++;
                        } else if (c == '}') {
                            bracesCount--;
                        }
                    }
                    break;
            }
        }
        switch (state) {
            case 0:
                if (sb.length() > 0) {
                    String literal = sb.toString();
                    if (components.isEmpty()) {
                        stem = literal;
                    }
                    components.add(new TemplateComponent(Type.LITERAL, literal, null, null, null));
                }
                break;
            case 1:
            case 2:
                throw new IllegalArgumentException("Invalid template " + template);
        }
        if (bracesCount > 0) {
            throw new IllegalArgumentException("Invalid template " + template + " Unmatched { braces");
        }

        //coalesce the components
        //once we have a CUSTOM_REGEX everything goes out the window, so we need to turn the remainder of the
        //template into a single CUSTOM_REGEX
        List<String> nameAggregator = null;
        StringBuilder regexAggregator = null;
        Iterator<TemplateComponent> it = components.iterator();
        while (it.hasNext()) {
            TemplateComponent component = it.next();
            if (nameAggregator != null) {
                it.remove();
                if (component.type == Type.LITERAL) {
                    regexAggregator.append(Pattern.quote(component.literalText));
                } else if (component.type == Type.DEFAULT_REGEX) {
                    regexAggregator.append("(?<").append(component.name).append(">[^/]+?)");
                    nameAggregator.add(component.name);
                } else if (component.type == Type.CUSTOM_REGEX) {
                    regexAggregator.append("(?<").append(component.name).append(">").append(component.literalText.trim())
                            .append(")");
                    nameAggregator.add(component.name);
                }
            } else if (component.type == Type.CUSTOM_REGEX) {
                it.remove();
                regexAggregator = new StringBuilder();
                nameAggregator = new ArrayList<>();
                regexAggregator.append("(?<").append(component.name).append(">").append(component.literalText.trim())
                        .append(")");
                nameAggregator.add(component.name);
            }
        }
        if (nameAggregator != null) {
            if (!this.prefixMatch) {
                regexAggregator.append("$");
            }
            components.add(new TemplateComponent(Type.CUSTOM_REGEX, null, null, Pattern.compile(regexAggregator.toString()),
                    nameAggregator.toArray(new String[0])));
        }
        this.stem = stem;
        this.literalCharacterCount = litChars;
        this.components = components.toArray(new TemplateComponent[0]);
        this.capturingGroups = capGroups;
        this.complexExpressions = complexGroups;

    }

    public URITemplate(String template, String stem, int literalCharacterCount,
            int capturingGroups, int complexExpressions, TemplateComponent[] components, boolean prefixMatch) {
        this.template = template;
        this.stem = stem;
        this.literalCharacterCount = literalCharacterCount;
        this.capturingGroups = capturingGroups;
        this.complexExpressions = complexExpressions;
        this.components = components;
        this.prefixMatch = prefixMatch;
    }

    @Override
    public int compareTo(URITemplate uriTemplate) {
        int val = stem.compareTo(uriTemplate.stem);
        if (val != 0) {
            return val;
        }
        val = Integer.compare(literalCharacterCount, uriTemplate.literalCharacterCount);
        if (val != 0) {
            return val;
        }
        val = Integer.compare(capturingGroups, uriTemplate.capturingGroups);
        if (val != 0) {
            return val;
        }
        val = Integer.compare(complexExpressions, uriTemplate.complexExpressions);
        if (val != 0) {
            return val;
        }
        return template.compareTo(uriTemplate.template);
    }

    public int countPathParamNames() {
        int classTemplateNameCount = 0;
        for (URITemplate.TemplateComponent i : components) {
            if (i.name != null) {
                classTemplateNameCount++;
            } else if (i.names != null) {
                classTemplateNameCount += i.names.length;
            }
        }
        return classTemplateNameCount;
    }

    public enum Type {
        /**
         * An actual literal
         */
        LITERAL,
        /**
         * The default regex, that matches any path segment up to a /
         */
        DEFAULT_REGEX,
        /**
         * A custom regex, that actually needs to be resolved via a Pattern. This may match additional segments.
         */
        CUSTOM_REGEX
    }

    public static class TemplateComponent implements Dumpable {

        /**
         * The type of component.
         */
        public final Type type;

        /**
         * The actual text of the segment
         */
        public final String literalText;

        /**
         * The parameter name to map the actual contents to
         */
        public final String name;

        /**
         * The pattern for custom regex. This pattern must start with ^ so it will only match from the very start.
         */
        public final Pattern pattern;

        /**
         * The names of all the capturing groups. Only used for CUSTOM_REGEX
         */
        public final String[] names;

        public TemplateComponent(Type type, String literalText, String name, Pattern pattern, String[] names) {
            this.type = type;
            this.literalText = literalText;
            this.name = name;
            this.pattern = pattern;
            this.names = names;
        }

        @Override
        public String toString() {
            return "TemplateComponent{ name: " + name + ", type: " + type + ", literalText: " + literalText + ", pattern: "
                    + pattern + "}";
        }

        @Override
        public void dump(int level) {
            indent(level);
            System.err.println("TemplateComponent");
            indent(level + 1);
            System.err.println("name: " + name);
            indent(level + 1);
            System.err.println("type: " + type);
            indent(level + 1);
            System.err.println("literalText: " + literalText);
            indent(level + 1);
            System.err.println("pattern: " + pattern);
        }
    }

    @Override
    public String toString() {
        return "URITemplate{ stem: " + stem + ", template: " + template + ", literalCharacterCount: " + literalCharacterCount
                + ", components: " + Arrays.toString(components) + " }";
    }

    @Override
    public void dump(int level) {
        indent(level);
        System.err.println("URITemplate");
        indent(level + 1);
        System.err.println("stem: " + stem);
        indent(level + 1);
        System.err.println("template: " + template);
        indent(level + 1);
        System.err.println("literalCharacterCount: " + literalCharacterCount);
        indent(level + 1);
        System.err.println("components: ");
        for (TemplateComponent component : components) {
            component.dump(level + 2);
        }
    }

}
