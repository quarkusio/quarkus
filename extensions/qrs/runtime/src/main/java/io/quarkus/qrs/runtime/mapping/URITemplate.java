package io.quarkus.qrs.runtime.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class URITemplate implements Dumpable {

    public final String template;

    public final String stem;

    /**
     * The number of characters that are literals in the path. According to the spec we need to sort by this.
     */
    public final int literalCharacterCount;

    /**
     * The components, first one is always the stem, so if the stem has been matched it can be ignored
     */
    public final TemplateComponent[] components;

    public URITemplate(String template) {
        if (!template.startsWith("/")) {
            template = "/" + template;
        }
        this.template = template;
        List<TemplateComponent> components = new ArrayList<>();
        String name = null;
        String stem = null;
        int litChars = 0;
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
                            components.add(new TemplateComponent(Type.LITERAL, literal, null, null));
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
                            components.add(new TemplateComponent(Type.DEFAULT_REGEX, null, sb.toString(), null));
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
                    if (c == '}') {
                        state = 0;
                        if (sb.length() > 0) {
                            components
                                    .add(new TemplateComponent(Type.CUSTOM_REGEX, null, name, Pattern.compile(sb.toString())));
                        } else {
                            throw new IllegalArgumentException("Invalid template " + template);
                        }
                        sb.setLength(0);
                    } else {
                        sb.append(c);
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
                    components.add(new TemplateComponent(Type.LITERAL, literal, null, null));
                }
                break;
            case 1:
            case 2:
                throw new IllegalArgumentException("Invalid template " + template);
        }
        this.stem = stem;
        this.literalCharacterCount = litChars;
        this.components = components.toArray(new TemplateComponent[0]);

    }

    public URITemplate(String template, String stem, int literalCharacterCount,
            TemplateComponent[] components) {
        this.template = template;
        this.stem = stem;
        this.literalCharacterCount = literalCharacterCount;
        this.components = components;
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

    public class TemplateComponent implements Dumpable {

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

        public TemplateComponent(Type type, String literalText, String name, Pattern pattern) {
            this.type = type;
            this.literalText = literalText;
            this.name = name;
            this.pattern = pattern;
        }
        
        @Override
        public String toString() {
            return "TemplateComponent{ name: "+name+", type: "+type+", literalText: "+literalText+", pattern: "+pattern+"}";
        }
        
        @Override
        public void dump(int level) {
            indent(level);
            System.err.println("TemplateComponent");
            indent(level+1);
            System.err.println("name: "+name);
            indent(level+1);
            System.err.println("type: "+type);
            indent(level+1);
            System.err.println("literalText: "+literalText);
            indent(level+1);
            System.err.println("pattern: "+pattern);
        }
    }

    @Override
    public String toString() {
        return "URITemplate{ stem: "+stem+", template: "+template+", literalCharacterCount: "+literalCharacterCount+", components: "+Arrays.toString(components)+" }";
    }
    
    @Override
    public void dump(int level) {
        indent(level);
        System.err.println("URITemplate");
        indent(level+1);
        System.err.println("stem: "+stem);
        indent(level+1);
        System.err.println("template: "+template);
        indent(level+1);
        System.err.println("literalCharacterCount: "+literalCharacterCount);
        indent(level+1);
        System.err.println("components: ");
        for (TemplateComponent component : components) {
            component.dump(level+2);
        }
    }
}
