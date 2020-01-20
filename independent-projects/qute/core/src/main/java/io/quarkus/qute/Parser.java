package io.quarkus.qute;

import io.quarkus.qute.Results.Result;
import io.quarkus.qute.SectionHelperFactory.ParametersInfo;
import io.quarkus.qute.TemplateNode.Origin;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 * Simple non-reusable parser.
 */
class Parser implements Function<String, Expression> {

    private static final Logger LOGGER = Logger.getLogger(Parser.class);
    private static final String ROOT_HELPER_NAME = "$root";
    private final EngineImpl engine;

    private static final char START_DELIMITER = '{';
    private static final char END_DELIMITER = '}';
    private static final char COMMENT_DELIMITER = '!';
    // Linux, BDS, etc.
    private static final char LINE_SEPARATOR_LF = '\n';
    // Mac OS 9, ZX Spectrum :-), etc.
    private static final char LINE_SEPARATOR_CR = '\r';
    // DOS, OS/2, Microsoft Windows, etc. use CRLF

    private StringBuilder buffer;
    private State state;
    private int line;
    private final Deque<SectionNode.Builder> sectionStack;
    private final Deque<SectionBlock.Builder> sectionBlockStack;
    private final Deque<ParametersInfo> paramsStack;
    private final Deque<Map<String, String>> typeInfoStack;
    private int sectionBlockIdx;
    private boolean ignoreContent;
    private String templateId;

    public Parser(EngineImpl engine) {
        this.engine = engine;
        this.state = State.TEXT;
        this.buffer = new StringBuilder();
        this.sectionStack = new ArrayDeque<>();
        this.sectionStack
                .addFirst(SectionNode.builder(ROOT_HELPER_NAME, new OriginImpl(line, templateId)).setEngine(engine)
                        .setHelperFactory(new SectionHelperFactory<SectionHelper>() {
                            @Override
                            public SectionHelper initialize(SectionInitContext context) {
                                return new SectionHelper() {

                                    @Override
                                    public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
                                        return context.execute();
                                    }
                                };
                            }

                        }));
        this.sectionBlockStack = new ArrayDeque<>();
        this.sectionBlockStack.addFirst(SectionBlock.builder(SectionHelperFactory.MAIN_BLOCK_NAME, this));
        this.sectionBlockIdx = 0;
        this.paramsStack = new ArrayDeque<>();
        this.paramsStack.addFirst(ParametersInfo.EMPTY);
        this.typeInfoStack = new ArrayDeque<>();
        this.typeInfoStack.addFirst(new HashMap<>());
        this.line = 1;
    }

    Template parse(Reader reader) {
        long start = System.currentTimeMillis();
        templateId = engine.generateId();
        reader = ensureBufferedReader(reader);
        try {
            int val;
            while ((val = reader.read()) != -1) {
                processCharacter((char) val);
            }

            if (buffer.length() > 0) {
                if (state == State.TEXT) {
                    // Flush the last text segment
                    flushText();
                } else {
                    throw new IllegalStateException(
                            "Unexpected non-text buffer at the end of the document (probably unterminated tag):" +
                                    buffer);
                }
            }

            SectionNode.Builder root = sectionStack.peek();
            if (root == null) {
                throw new IllegalStateException("No root section found!");
            }
            if (!root.helperName.equals(ROOT_HELPER_NAME)) {
                throw new IllegalStateException("The last section on the stack is not a root but: " + root.helperName);
            }
            SectionBlock.Builder part = sectionBlockStack.peek();
            if (part == null) {
                throw new IllegalStateException("No root section part found!");
            }
            root.addBlock(part.build());
            Template template = new TemplateImpl(engine, root.build(), templateId);
            LOGGER.tracef("Parsing finished in %s ms", System.currentTimeMillis() - start);
            return template;

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void processCharacter(char character) {
        switch (state) {
            case TEXT:
                text(character);
                break;
            case TAG_INSIDE:
                tag(character);
                break;
            case COMMENT:
                comment(character);
                break;
            case TAG_CANDIDATE:
                tagCandidate(character);
                break;
            default:
                throw new IllegalStateException("Unknown parsing state");
        }
    }

    private void text(char character) {
        if (character == START_DELIMITER) {
            state = State.TAG_CANDIDATE;
        } else {
            if (isLineSeparator(character)) {
                line++;
            }
            buffer.append(character);
        }
    }

    private void comment(char character) {
        if (character == END_DELIMITER && buffer.length() > 0 && buffer.charAt(buffer.length() - 1) == COMMENT_DELIMITER) {
            // End of comment
            state = State.TEXT;
            buffer = new StringBuilder();
        } else {
            buffer.append(character);
        }
    }

    private void tag(char character) {
        if (character == END_DELIMITER) {
            flushTag();
        } else {
            buffer.append(character);
        }
    }

    private void tagCandidate(char character) {
        if (Character.isWhitespace(character)) {
            buffer.append(START_DELIMITER).append(character);
            if (isLineSeparator(character)) {
                line++;
            }
            state = State.TEXT;
        } else if (character == START_DELIMITER) {
            buffer.append(START_DELIMITER).append(START_DELIMITER);
            state = State.TEXT;
        } else {
            // Real tag start, flush text if any
            flushText();
            state = character == COMMENT_DELIMITER ? State.COMMENT : State.TAG_INSIDE;
            buffer.append(character);
        }
    }

    private boolean isLineSeparator(char character) {
        return character == LINE_SEPARATOR_CR
                || (character == LINE_SEPARATOR_LF
                        && (buffer.length() == 0 || buffer.charAt(buffer.length() - 1) != LINE_SEPARATOR_CR));
    }

    private void flushText() {
        if (buffer.length() > 0 && !ignoreContent) {
            SectionBlock.Builder block = sectionBlockStack.peek();
            block.addNode(new TextNode(buffer.toString(), new OriginImpl(line, templateId)));
        }
        this.buffer = new StringBuilder();
    }

    private void flushTag() {
        state = State.TEXT;
        String content = buffer.toString();

        if (content.charAt(0) == Tag.SECTION.getCommand()) {

            boolean isEmptySection = false;
            if (content.charAt(content.length() - 1) == Tag.SECTION_END.command) {
                content = content.substring(0, content.length() - 1);
                isEmptySection = true;
            }

            Iterator<String> iter = splitSectionParams(content);
            if (!iter.hasNext()) {
                throw new IllegalStateException("No helper name");
            }
            String sectionName = iter.next();
            sectionName = sectionName.substring(1, sectionName.length());

            SectionNode.Builder lastSection = sectionStack.peek();
            // Add a section block if the section name matches a section block label or does not map to any section helper and the last section treats unknown subsections as blocks
            if (lastSection != null && lastSection.factory.getBlockLabels().contains(sectionName)
                    || (lastSection.factory.treatUnknownSectionsAsBlocks()
                            && engine.getSectionHelperFactory(sectionName) == null)) {

                // Section block
                if (!ignoreContent) {
                    // E.g. {#else if valid}
                    // Build the previous block
                    sectionStack.peek().addBlock(sectionBlockStack.pop().build());
                }
                // Add the new block
                SectionBlock.Builder block = SectionBlock.builder("" + sectionBlockIdx++, this);
                sectionBlockStack.addFirst(block.setLabel(sectionName));
                processParams(sectionName, iter);

                // Initialize the block
                Map<String, String> typeInfos = typeInfoStack.peek();
                Map<String, String> result = sectionStack.peek().factory.initializeBlock(typeInfos, block);
                if (!result.isEmpty()) {
                    Map<String, String> newTypeInfos = new HashMap<>();
                    newTypeInfos.putAll(typeInfos);
                    newTypeInfos.putAll(result);
                    typeInfoStack.addFirst(newTypeInfos);
                } else {
                    typeInfoStack.addFirst(typeInfos);
                }

                ignoreContent = false;

            } else {
                // New section
                SectionHelperFactory<?> factory = engine.getSectionHelperFactory(sectionName);
                if (factory == null) {
                    throw new IllegalStateException("No section helper for: " + sectionName);
                }
                paramsStack.addFirst(factory.getParameters());
                SectionBlock.Builder mainBlock = SectionBlock.builder(SectionHelperFactory.MAIN_BLOCK_NAME, this);
                sectionBlockStack.addFirst(mainBlock);
                processParams(SectionHelperFactory.MAIN_BLOCK_NAME, iter);

                // Init section block
                Map<String, String> typeInfos = typeInfoStack.peek();
                Map<String, String> result = factory.initializeBlock(typeInfos, mainBlock);
                SectionNode.Builder sectionNode = SectionNode.builder(sectionName, new OriginImpl(-1, templateId))
                        .setEngine(engine)
                        .setHelperFactory(factory);

                if (isEmptySection) {
                    sectionNode.addBlock(mainBlock.build());
                    // Remove params from the stack
                    paramsStack.pop();
                    // Remove the block from the stack
                    sectionBlockStack.pop();
                    // Add node to the parent block
                    sectionBlockStack.peek().addNode(sectionNode.build());
                } else {
                    if (!result.isEmpty()) {
                        // The section modifies the type info stack
                        Map<String, String> newTypeInfos = new HashMap<>();
                        newTypeInfos.putAll(typeInfos);
                        newTypeInfos.putAll(result);
                        typeInfoStack.addFirst(newTypeInfos);
                    } else {
                        typeInfoStack.addFirst(typeInfos);
                    }
                    sectionStack.addFirst(sectionNode);
                }
            }
        } else if (content.charAt(0) == Tag.SECTION_END.getCommand()) {
            SectionBlock.Builder block = sectionBlockStack.peek();
            SectionNode.Builder section = sectionStack.peek();
            String name = content.substring(1, content.length());
            if (block != null && !block.getLabel().equals(SectionHelperFactory.MAIN_BLOCK_NAME)
                    && !section.helperName.equals(name)) {
                // Block end
                if (!name.isEmpty() && !block.getLabel().equals(name)) {
                    throw new IllegalStateException(
                            "Section block end tag does not match the start tag. Start: " + block.getLabel() + ", end: "
                                    + name);
                }
                section.addBlock(sectionBlockStack.pop().build());
                ignoreContent = true;
            } else {
                // Section end
                if (!name.isEmpty() && !section.helperName.equals(name)) {
                    throw new IllegalStateException(
                            "Section end tag does not match the start tag. Start: " + section.helperName + ", end: " + name);
                }
                section = sectionStack.pop();
                if (!ignoreContent) {
                    section.addBlock(sectionBlockStack.pop().build());
                }
                sectionBlockStack.peek().addNode(section.build());
            }

            // Remove the last type info map from the stack
            typeInfoStack.pop();

        } else if (content.charAt(0) == Tag.PARAM.getCommand()) {

            // {@org.acme.Foo foo}
            Map<String, String> typeInfos = typeInfoStack.peek();
            int spaceIdx = content.indexOf(" ");
            String key = content.substring(spaceIdx + 1, content.length());
            String value = content.substring(1, spaceIdx);
            typeInfos.put(key, "[" + value + "]");

        } else {
            sectionBlockStack.peek().addNode(new ExpressionNode(apply(content), engine, new OriginImpl(line, templateId)));
        }
        this.buffer = new StringBuilder();
    }

    private void processParams(String label, Iterator<String> iter) {
        Map<String, String> params = new HashMap<>();
        List<Parameter> factoryParams = paramsStack.peek().get(label);
        List<String> paramValues = new ArrayList<>();

        while (iter.hasNext()) {
            paramValues.add(iter.next());
        }
        if (paramValues.size() > factoryParams.size()) {
            LOGGER.debugf("Too many params [label=%s, params=%s, factoryParams=%s]", label, paramValues, factoryParams);
        }
        if (paramValues.size() < factoryParams.size()) {
            for (String param : paramValues) {
                int equalsPosition = getFirstDeterminingEqualsCharPosition(param);
                if (equalsPosition != -1) {
                    // Named param
                    params.put(param.substring(0, equalsPosition), param.substring(equalsPosition + 1,
                            param.length()));
                } else {
                    // Positional param - first non-default section param
                    for (Parameter factoryParam : factoryParams) {
                        if (factoryParam.defaultValue == null && !params.containsKey(factoryParam.name)) {
                            params.put(factoryParam.name, param);
                            break;
                        }
                    }
                }
            }
        } else {
            for (String param : paramValues) {
                int equalsPosition = getFirstDeterminingEqualsCharPosition(param);
                if (equalsPosition != -1) {
                    // Named param
                    params.put(param.substring(0, equalsPosition), param.substring(equalsPosition + 1,
                            param.length()));
                } else {
                    // Positional param - first non-default section param
                    for (Parameter factoryParam : factoryParams) {
                        if (!params.containsKey(factoryParam.name)) {
                            params.put(factoryParam.name, param);
                            break;
                        }
                    }
                }
            }
        }

        factoryParams.stream().filter(p -> p.defaultValue != null).forEach(p -> params.putIfAbsent(p.name, p.defaultValue));

        // TODO validate params
        List<Parameter> undeclaredParams = factoryParams.stream().filter(p -> !p.optional && !params.containsKey(p.name))
                .collect(Collectors.toList());
        if (!undeclaredParams.isEmpty()) {
            throw new IllegalStateException("Undeclared section params: " + undeclaredParams);
        }

        params.forEach(sectionBlockStack.peek()::addParameter);
    }

    /**
     *
     * @param part
     * @return the index of an equals char outside of any string literal,
     *         <code>-1</code> if no such char is found
     */
    static int getFirstDeterminingEqualsCharPosition(String part) {
        boolean stringLiteral = false;
        for (int i = 0; i < part.length(); i++) {
            if (isStringLiteralSeparator(part.charAt(i))) {
                if (i == 0) {
                    // The first char is a string literal separator
                    return -1;
                }
                stringLiteral = !stringLiteral;
            } else {
                if (!stringLiteral && part.charAt(i) == '=' && (i != 0) && (i < (part.length() - 1))) {
                    return i;
                }
            }
        }
        return -1;
    }

    private Reader ensureBufferedReader(Reader reader) {
        return reader instanceof BufferedReader ? reader
                : new BufferedReader(
                        reader);
    }

    static Iterator<String> splitSectionParams(String content) {

        boolean stringLiteral = false;
        boolean listLiteral = false;
        boolean space = false;
        List<String> parts = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == ' ') {
                if (!space) {
                    if (!stringLiteral && !listLiteral) {
                        if (buffer.length() > 0) {
                            parts.add(buffer.toString());
                            buffer = new StringBuilder();
                        }
                        space = true;
                    } else {
                        buffer.append(content.charAt(i));
                    }
                }
            } else {
                if (!listLiteral
                        && isStringLiteralSeparator(content.charAt(i))) {
                    stringLiteral = !stringLiteral;
                } else if (!stringLiteral
                        && isListLiteralStart(content.charAt(i))) {
                    listLiteral = true;
                } else if (!stringLiteral
                        && isListLiteralEnd(content.charAt(i))) {
                    listLiteral = false;
                }
                space = false;
                buffer.append(content.charAt(i));
            }
        }

        if (buffer.length() > 0) {
            if (stringLiteral || listLiteral) {
                throw new IllegalStateException(
                        "Unterminated string or array literal detected");
            }
            parts.add(buffer.toString());
        }
        return parts.iterator();
    }

    static boolean isListLiteralStart(char character) {
        return character == '[';
    }

    static boolean isListLiteralEnd(char character) {
        return character == ']';
    }

    enum Tag {

        EXPRESSION(null),
        SECTION('#'),
        SECTION_END('/'),
        SECTION_BLOCK(':'),
        PARAM('@'),
        ;

        private final Character command;

        private Tag(Character command) {
            this.command = command;
        }

        public Character getCommand() {
            return command;
        }

    }

    enum State {

        TEXT,
        TAG_INSIDE,
        TAG_CANDIDATE,
        COMMENT,

    }

    public static Expression parseExpression(String value, Map<String, String> typeInfos, Origin origin) {
        if (value == null || value.isEmpty()) {
            return Expression.EMPTY;
        }
        if (typeInfos == null) {
            typeInfos = Collections.emptyMap();
        }
        String namespace = null;
        List<String> parts;
        Object literal = Result.NOT_FOUND;
        int namespaceIdx = value.indexOf(':');
        int spaceIdx = value.indexOf(' ');
        int bracketIdx = value.indexOf('(');
        String typeCheckInfo = null;
        if (namespaceIdx != -1 && (spaceIdx == -1 || namespaceIdx < spaceIdx)
                && (bracketIdx == -1 || namespaceIdx < bracketIdx)) {
            parts = Expressions.splitParts(value.substring(namespaceIdx + 1, value.length()));
            namespace = value.substring(0, namespaceIdx);
        } else {
            parts = Expressions.splitParts(value);
            if (parts.size() == 1) {
                literal = LiteralSupport.getLiteral(parts.get(0));
            }
        }
        if (literal == Result.NOT_FOUND) {
            if (namespace != null) {
                // TODO use constants!
                typeCheckInfo = "[" + Expressions.TYPECHECK_NAMESPACE_PLACEHOLDER + "]";
                typeCheckInfo += "." + parts.stream().collect(Collectors.joining("."));
            } else if (typeInfos.containsKey(parts.get(0))) {
                typeCheckInfo = typeInfos.get(parts.get(0));
                if (typeCheckInfo != null) {
                    if (parts.size() == 2) {
                        typeCheckInfo += "." + parts.get(1);
                    } else if (parts.size() > 2) {
                        typeCheckInfo += "." + parts.stream().skip(1).collect(Collectors.joining("."));
                    }
                }
            }
        }
        return new Expression(namespace, parts, literal, typeCheckInfo, origin);
    }

    static boolean isSeparator(char candidate) {
        return candidate == '.' || candidate == '[' || candidate == ']';
    }

    /**
     *
     * @param character
     * @return <code>true</code> if the char is a string literal separator,
     *         <code>false</code> otherwise
     */
    static boolean isStringLiteralSeparator(char character) {
        return character == '"' || character == '\'';
    }

    static boolean isBracket(char character) {
        return character == '(' || character == ')';
    }

    @Override
    public Expression apply(String value) {
        return parseExpression(value, typeInfoStack.peek(), new OriginImpl(line, templateId));
    }

    static class OriginImpl implements Origin {

        private final int line;
        private final String templateId;

        OriginImpl(int line, String templateId) {
            this.line = line;
            this.templateId = templateId;
        }

        @Override
        public int getLine() {
            return line;
        }

        @Override
        public String getTemplateId() {
            return templateId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(line, templateId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            OriginImpl other = (OriginImpl) obj;
            return line == other.line && Objects.equals(templateId, other.templateId);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("OriginImpl [line=").append(line).append(", templateId=").append(templateId).append("]");
            return builder.toString();
        }

    }

}
