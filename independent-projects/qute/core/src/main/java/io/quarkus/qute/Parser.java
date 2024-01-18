package io.quarkus.qute;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.qute.Expression.Part;
import io.quarkus.qute.SectionHelperFactory.BlockInfo;
import io.quarkus.qute.SectionHelperFactory.MissingEndTagStrategy;
import io.quarkus.qute.SectionHelperFactory.ParametersInfo;
import io.quarkus.qute.SectionHelperFactory.ParserDelegate;
import io.quarkus.qute.TemplateNode.Origin;

/**
 * Simple non-reusable parser.
 */
class Parser implements ParserHelper, ParserDelegate, WithOrigin, ErrorInitializer {

    private static final Logger LOGGER = Logger.getLogger(Parser.class);
    static final String ROOT_HELPER_NAME = "$root";

    static final Origin SYNTHETIC_ORIGIN = new OriginImpl(0, 0, 0, "<<synthetic>>", "<<synthetic>>", Optional.empty());

    private static final char START_DELIMITER = '{';
    private static final char END_DELIMITER = '}';
    private static final char COMMENT_DELIMITER = '!';
    private static final char CDATA_START_DELIMITER = '|';
    private static final char CDATA_END_DELIMITER = '|';
    private static final char UNDERSCORE = '_';
    private static final char ESCAPE_CHAR = '\\';
    private static final char NAMESPACE_SEPARATOR = ':';

    // Linux, BDS, etc.
    private static final char LINE_SEPARATOR_LF = '\n';
    // Mac OS 9, ZX Spectrum :-), etc.
    private static final char LINE_SEPARATOR_CR = '\r';
    // DOS, OS/2, Microsoft Windows, etc. use CRLF

    static final char START_COMPOSITE_PARAM = '(';
    static final char END_COMPOSITE_PARAM = ')';

    private final EngineImpl engine;
    private final Reader reader;
    private final Optional<Variant> variant;
    private final String templateId;
    private final String generatedId;

    private StringBuilder buffer;
    private State state;
    private int line;
    private int lineCharacter;
    private final Deque<SectionNode.Builder> sectionStack;
    private final Deque<ParametersInfo> paramsStack;
    private final Deque<Scope> scopeStack;
    private int sectionBlockIdx;
    private boolean ignoreContent;
    private AtomicInteger expressionIdGenerator;
    private final List<Function<String, String>> contentFilters;
    private boolean hasLineSeparator;

    private TemplateImpl template;

    public Parser(EngineImpl engine, Reader reader, String templateId, String generatedId, Optional<Variant> variant) {
        this.engine = engine;
        this.templateId = templateId;
        this.generatedId = generatedId;
        this.variant = variant;
        this.reader = reader;

        this.state = State.TEXT;
        this.buffer = new StringBuilder();
        this.sectionStack = new ArrayDeque<>();
        this.sectionBlockIdx = 0;
        this.paramsStack = new ArrayDeque<>();
        this.paramsStack.addFirst(ParametersInfo.EMPTY);
        this.scopeStack = new ArrayDeque<>();
        this.scopeStack.addFirst(new Scope(null));
        this.line = 1;
        this.lineCharacter = 1;
        this.expressionIdGenerator = new AtomicInteger();
        this.contentFilters = new ArrayList<>(5);
    }

    static class RootSectionHelperFactory implements SectionHelperFactory<SectionHelper> {

        @Override
        public SectionHelper initialize(SectionInitContext context) {
            return new SectionHelper() {

                @Override
                public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
                    return context.execute();
                }
            };
        }
    }

    private Template currentTemplate() {
        if (template == null) {
            throw new IllegalStateException("Template [" + templateId + "] is not parsed yet");
        }
        return template;
    }

    Template parse() {

        SectionNode.Builder rootBuilder = SectionNode.builder(ROOT_HELPER_NAME, syntheticOrigin(), this, this)
                .setEngine(engine)
                .setHelperFactory(ROOT_SECTION_HELPER_FACTORY);
        sectionStack.addFirst(rootBuilder);

        // Add synthetic nodes for param declarations added by parser hooks
        Map<String, String> bindings = scopeStack.peek().getBindings();
        if (bindings != null && !bindings.isEmpty()) {
            for (Entry<String, String> e : bindings.entrySet()) {
                rootBuilder.currentBlock().addNode(
                        new ParameterDeclarationNode(e.getValue(), e.getKey(), null, syntheticOrigin()));
            }
        }

        long start = System.nanoTime();
        Reader r = reader;

        try {
            if (!contentFilters.isEmpty()) {
                String contents = toString(reader);
                for (Function<String, String> filter : contentFilters) {
                    contents = filter.apply(contents);
                }
                r = new StringReader(contents);
            }

            int val;
            while ((val = r.read()) != -1) {
                processCharacter((char) val);
                lineCharacter++;
            }

            if (buffer.length() > 0) {
                if (state == State.TEXT || state == State.LINE_SEPARATOR) {
                    // Flush the last text segment
                    flushText();
                } else {
                    String reason = null;
                    ErrorCode code = null;
                    if (state == State.TAG_INSIDE_STRING_LITERAL) {
                        reason = "unterminated string literal";
                        code = ParserError.UNTERMINATED_STRING_LITERAL;
                    } else if (state == State.TAG_INSIDE) {
                        // First handle the optional end tags and if an unterminated section is found the then throw an exception
                        SectionNode.Builder section = sectionStack.peek();
                        if (!section.helperName.equals(ROOT_HELPER_NAME)) {
                            SectionNode.Builder unterminated = handleOptionalEngTags(section, ROOT_HELPER_NAME);
                            if (unterminated != null) {
                                reason = "unterminated section";
                                code = ParserError.UNTERMINATED_SECTION;
                            }
                        } else {
                            reason = "unterminated expression";
                            code = ParserError.UNTERMINATED_EXPRESSION;
                        }
                    } else {
                        reason = "unexpected state [" + state + "]";
                        code = ParserError.GENERAL_ERROR;
                    }
                    if (code != null) {
                        throw error(code,
                                "unexpected non-text buffer at the end of the template - {reason}: {buffer}")
                                .argument("reason", reason)
                                .argument("buffer", buffer)
                                .build();
                    }
                }
            }

            // Note that this also handles the param declarations with default values, i.e. synthetic {#let} sections
            handleOptionalEngTags(sectionStack.peek(), ROOT_HELPER_NAME);

            SectionNode.Builder root = sectionStack.peek();
            if (root == null) {
                throw error(ParserError.GENERAL_ERROR, "no root section found").build();
            }
            if (!root.helperName.equals(ROOT_HELPER_NAME)) {
                throw error(ParserError.UNTERMINATED_SECTION, "unterminated section [{tag}] detected")
                        .argument("tag", root.helperName)
                        .build();
            }
            template = new TemplateImpl(engine, root.build(this::currentTemplate), templateId, generatedId,
                    variant);

            Set<TemplateNode> nodesToRemove = Collections.emptySet();
            if (hasLineSeparator && engine.removeStandaloneLines) {
                nodesToRemove = new HashSet<>();
                List<List<TemplateNode>> lines = readLines(template.root);
                for (List<TemplateNode> line : lines) {
                    if (isStandalone(line)) {
                        for (TemplateNode node : line) {
                            if (node instanceof SectionNode) {
                                continue;
                            }
                            nodesToRemove.add(node);
                        }
                    }
                }
                if (nodesToRemove.isEmpty()) {
                    nodesToRemove = Collections.emptySet();
                }
            }
            template.root.optimizeNodes(nodesToRemove);

            LOGGER.tracef("Parsing finished in %s ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
            return template;

        } catch (IOException e) {
            throw new TemplateException(e);
        }
    }

    private void processCharacter(char character) {
        switch (state) {
            case TEXT:
                text(character);
                break;
            case ESCAPE:
                escape(character);
                break;
            case TAG_INSIDE:
                tag(character);
                break;
            case TAG_INSIDE_STRING_LITERAL:
                tagStringLiteral(character);
                break;
            case COMMENT:
                comment(character);
                break;
            case CDATA:
                cdata(character);
                break;
            case TAG_CANDIDATE:
                tagCandidate(character);
                break;
            case LINE_SEPARATOR:
                lineSeparator(character);
                break;
            default:
                throw error(ParserError.GENERAL_ERROR, "unknown parsing state: {state}").argument("state", state).build();
        }
    }

    private void escape(char character) {
        if (character != START_DELIMITER && character != END_DELIMITER) {
            // Invalid escape sequence is just ignored
            buffer.append(ESCAPE_CHAR);
        }
        buffer.append(character);
        state = State.TEXT;
    }

    private void text(char character) {
        if (character == START_DELIMITER) {
            state = State.TAG_CANDIDATE;
        } else if (character == ESCAPE_CHAR) {
            state = State.ESCAPE;
        } else if (isLineSeparatorStart(character)) {
            flushText();
            buffer.append(character);
            state = State.LINE_SEPARATOR;
        } else {
            buffer.append(character);
        }
    }

    private void lineSeparator(char character) {
        if (character == LINE_SEPARATOR_LF && buffer.length() > 0 && buffer.charAt(buffer.length() - 1) == LINE_SEPARATOR_CR) {
            // CRLF
            buffer.append(character);
            flushNextLine();
            state = State.TEXT;
        } else {
            flushNextLine();
            state = State.TEXT;
            processCharacter(character);
        }
        // Parsing of one-line templates can be optimized
        hasLineSeparator = true;
    }

    private void comment(char character) {
        if (character == END_DELIMITER && buffer.length() > 0 && buffer.charAt(buffer.length() - 1) == COMMENT_DELIMITER) {
            // End of comment
            state = State.TEXT;
            buffer = new StringBuilder();
            if (engine.removeStandaloneLines) {
                // Add a dummy comment block to detect standalone lines
                sectionStack.peek().currentBlock().addNode(COMMENT_NODE);
            }
        } else {
            buffer.append(character);
        }
    }

    private void cdata(char character) {
        if (character == END_DELIMITER && buffer.length() > 0
                && isCdataEnd(buffer.charAt(buffer.length() - 1))) {
            // End of cdata
            state = State.TEXT;
            buffer.deleteCharAt(buffer.length() - 1);
            flushText();
        } else {
            buffer.append(character);
        }
    }

    private boolean isCdataEnd(char character) {
        return character == CDATA_END_DELIMITER;
    }

    private void tag(char character) {
        if (LiteralSupport.isStringLiteralSeparator(character)) {
            state = State.TAG_INSIDE_STRING_LITERAL;
            buffer.append(character);
        } else if (character == END_DELIMITER) {
            flushTag();
        } else {
            buffer.append(character);
        }
    }

    private void tagStringLiteral(char character) {
        if (LiteralSupport.isStringLiteralSeparator(character)) {
            state = State.TAG_INSIDE;
        }
        buffer.append(character);
    }

    private void tagCandidate(char character) {
        if (isValidIdentifierStart(character)) {
            // Real tag start, flush text if any
            flushText();
            if (character == COMMENT_DELIMITER) {
                buffer.append(character);
                state = State.COMMENT;
            } else if (character == CDATA_START_DELIMITER) {
                state = State.CDATA;
            } else {
                buffer.append(character);
                state = State.TAG_INSIDE;
            }
        } else {
            // Ignore expressions/tags starting with an invalid identifier
            buffer.append(START_DELIMITER);
            state = State.TEXT;
            if (START_DELIMITER == character) {
                buffer.append(START_DELIMITER);
            } else {
                processCharacter(character);
            }
        }
    }

    private boolean isValidIdentifierStart(char character) {
        // A valid identifier must start with a digit, alphabet, underscore, comment delimiter, cdata start delimiter or a tag command (e.g. # for sections)
        return Tag.isCommand(character) || character == COMMENT_DELIMITER || character == CDATA_START_DELIMITER
                || character == UNDERSCORE
                || Character.isDigit(character)
                || Character.isAlphabetic(character);
    }

    static boolean isValidIdentifier(String value) {
        int offset = 0;
        int length = value.length();
        while (offset < length) {
            int c = value.codePointAt(offset);
            if (!Character.isWhitespace(c)) {
                offset += Character.charCount(c);
                continue;
            }
            return false;
        }
        return true;
    }

    private boolean isLineSeparatorStart(char character) {
        return character == LINE_SEPARATOR_CR || character == LINE_SEPARATOR_LF;
    }

    private void flushText() {
        if (buffer.length() > 0 && !ignoreContent) {
            SectionBlock.Builder block = sectionStack.peek().currentBlock();
            block.addNode(new TextNode(buffer.toString(), origin(0)));
        }
        this.buffer = new StringBuilder();
    }

    private void flushNextLine() {
        if (buffer.length() > 0 && !ignoreContent) {
            SectionBlock.Builder block = sectionStack.peek().currentBlock();
            block.addNode(new LineSeparatorNode(buffer.toString(), origin(0)));
        }
        this.buffer = new StringBuilder();
        line++;
        lineCharacter = 1;
    }

    private void flushTag() {
        state = State.TEXT;
        String content = buffer.toString().trim();
        String tag = START_DELIMITER + content + END_DELIMITER;

        if (content.charAt(0) == Tag.SECTION.command) {
            // It's a section/block start
            // {#if}, {#else}, etc.
            sectionStart(content, tag);
        } else if (content.charAt(0) == Tag.SECTION_END.command) {
            // It's a section/block end
            sectionEnd(content, tag);
        } else if (content.charAt(0) == Tag.PARAM.command) {
            // Parameter declaration
            // {@org.acme.Foo foo}
            parameterDeclaration(content, tag);
        } else {
            // Expression
            sectionStack.peek().currentBlock()
                    .addNode(new ExpressionNode(createExpression(content), engine));
        }
        this.buffer = new StringBuilder();
    }

    private void sectionStart(String content, String tag) {
        boolean isEmptySection = false;
        if (content.charAt(content.length() - 1) == Tag.SECTION_END.command) {
            content = content.substring(0, content.length() - 1);
            isEmptySection = true;
        }

        Iterator<String> iter = splitSectionParams(content, this);
        if (!iter.hasNext()) {
            throw error(ParserError.NO_SECTION_NAME, "no section name declared for {tag}").argument("tag", tag).build();
        }
        String sectionName = iter.next();
        sectionName = sectionName.substring(1, sectionName.length());

        if (sectionName.isBlank()) {
            throw error(ParserError.NO_SECTION_NAME, "no section name declared for {tag}").argument("tag", tag).build();
        }

        SectionNode.Builder lastSection = sectionStack.peek();
        // Add a section block if the section name matches a section block label
        // or does not map to any section helper and the last section treats unknown subsections as blocks
        if (lastSection != null && lastSection.factory.getBlockLabels().contains(sectionName)
                || (lastSection.factory.treatUnknownSectionsAsBlocks()
                        && !engine.getSectionHelperFactories().containsKey(sectionName))) {

            // => New section block
            SectionBlock.Builder block = SectionBlock.builder("" + sectionBlockIdx++, this, this)
                    .setOrigin(origin(tag.length() - 1)).setLabel(sectionName);
            lastSection.addBlock(block);

            processParams(tag, sectionName, iter, block);

            // Initialize the block
            Scope currentScope = scopeStack.peek();
            Scope newScope = lastSection.factory.initializeBlock(currentScope, block);
            scopeStack.addFirst(newScope);

        } else {
            // => New section
            SectionHelperFactory<?> factory = engine.getSectionHelperFactory(sectionName);
            if (factory == null) {
                throw error(ParserError.NO_SECTION_HELPER_FOUND, "no section helper found for {tag}")
                        .argument("tag", tag)
                        .build();
            }
            SectionNode.Builder sectionNode = SectionNode
                    .builder(sectionName, origin(tag.length() - 1), this, this)
                    .setEngine(engine)
                    .setHelperFactory(factory);

            paramsStack.addFirst(factory.getParameters());
            processParams(tag, SectionHelperFactory.MAIN_BLOCK_NAME, iter, sectionNode.currentBlock());

            // Init section block
            Scope currentScope = scopeStack.peek();
            Scope newScope = factory.initializeBlock(currentScope, sectionNode.currentBlock());

            if (isEmptySection) {
                // Remove params from the stack
                paramsStack.pop();
                // Add node to the parent block
                sectionStack.peek().currentBlock().addNode(sectionNode.build(this::currentTemplate));
            } else {
                scopeStack.addFirst(newScope);
                sectionStack.addFirst(sectionNode);
            }
        }
    }

    private void sectionEnd(String content, String tag) {
        SectionNode.Builder section = sectionStack.peek();
        SectionBlock.Builder block = section.currentBlock();
        String name = content.substring(1, content.length());
        if (block != null
                && !block.getLabel().equals(SectionHelperFactory.MAIN_BLOCK_NAME)
                && !section.helperName.equals(name)) {
            // Non-main block end, e.g. {/else}
            if (!name.isEmpty() && !block.getLabel().equals(name)) {
                throw error(ParserError.SECTION_BLOCK_END_DOES_NOT_MATCH_START,
                        "section block end tag [{name}] does not match the start tag [{tagName}]")
                        .argument("name", name)
                        .argument("tagName", block.getLabel()).build();
            }
            section.endBlock();
        } else {
            // Section end, e.g. {/if} or {/}
            if (section.helperName.equals(ROOT_HELPER_NAME)) {
                throw error(ParserError.SECTION_START_NOT_FOUND, "section start tag found for {tag}")
                        .argument("tag", tag)
                        .build();
            }
            if (!name.isEmpty() && !section.helperName.equals(name)) {
                // The tag name is not empty but does not match the current section
                // First handle the optional end tags and if an unterminated section is found the then throw an exception
                SectionNode.Builder unterminated = handleOptionalEngTags(section, name);
                if (unterminated != null) {
                    throw error(ParserError.SECTION_END_DOES_NOT_MATCH_START,
                            "section end tag [{name}] does not match the start tag [{tag}]")
                            .argument("name", name)
                            .argument("tag", unterminated.helperName)
                            .build();
                }
            }
            // Pop the section and its main block
            section = sectionStack.pop();
            sectionStack.peek().currentBlock().addNode(section.build(this::currentTemplate));
        }

        // Remove the last type info map from the stack
        scopeStack.pop();
    }

    /**
     *
     * @param section
     * @return an unterminated section or {@code null} if no unterminated section was detected
     */
    private SectionNode.Builder handleOptionalEngTags(SectionNode.Builder section, String name) {
        while (section != null && !section.helperName.equals(name)) {
            if (section.factory.missingEndTagStrategy() == MissingEndTagStrategy.BIND_TO_PARENT) {
                section = sectionStack.pop();
                sectionStack.peek().currentBlock().addNode(section.build(this::currentTemplate));
                scopeStack.pop();
                section = sectionStack.peek();
            } else {
                return section;
            }
        }
        return null;
    }

    private void parameterDeclaration(String content, String tag) {

        Scope currentScope = scopeStack.peek();
        // "@org.acme.Foo foo " -> "org.acme.Foo foo" and split
        List<String> parts = Expressions.splitParts(content.substring(1).trim(),
                Expressions.PARAM_DECLARATION_SPLIT_CONFIG);

        String value = null;
        String key = null;
        String defaultValue = null;

        if (parts.size() >= 2 && parts.size() <= 4) {
            // [typeInfo][name]
            value = parts.get(0);
            key = parts.get(1);

            if (parts.size() == 4 && "=".equals(parts.get(2))) {
                // [typeInfo][name][=][defaultValue]
                defaultValue = parts.get(3);
            } else if (parts.size() == 2) {
                // [typeInfo][name=defaultValue]
                int eqIdx = key.indexOf('=');
                if (eqIdx != -1) {
                    defaultValue = key.substring(eqIdx + 1);
                    key = key.substring(0, eqIdx);
                }
            } else if (parts.size() == 3) {
                // [typeInfo][name][=defaultValue]
                // [typeInfo][name=][defaultValue]
                String defValPart = parts.get(2);
                int eqIdx = defValPart.indexOf('=');
                if (eqIdx != -1) {
                    defaultValue = defValPart.substring(eqIdx + 1);
                } else {
                    eqIdx = key.indexOf('=');
                    if (eqIdx != -1) {
                        defaultValue = defValPart;
                        key = key.substring(0, eqIdx);
                    }
                }
            }
        }

        if (key == null || value == null || (parts.size() > 2 && defaultValue == null)) {
            throw error(ParserError.INVALID_PARAM_DECLARATION, "invalid parameter declaration {param}")
                    .argument("param", START_DELIMITER + buffer.toString() + END_DELIMITER)
                    .origin(origin(0))
                    .build();
        }

        String typeInfo = Expressions.typeInfoFrom(value);
        currentScope.putBinding(key, typeInfo);
        sectionStack.peek().currentBlock().addNode(
                new ParameterDeclarationNode(typeInfo, key, defaultValue != null ? createExpression(defaultValue) : null,
                        origin(0)));

        // If a default value is set we add a synthetic {#let} section to define local variables with default values
        if (defaultValue != null) {
            SectionHelperFactory<?> factory = engine.getSectionHelperFactory("let");
            if (factory == null) {
                throw error(ParserError.NO_SECTION_HELPER_FOUND,
                        "Parameter declaration with a default value requires a \\{#let} section helper to be present: {tag}")
                        .argument("tag", tag)
                        .build();
            }
            SectionNode.Builder sectionNode = SectionNode
                    .builder("let", origin(0), this, this)
                    .setEngine(engine)
                    .setHelperFactory(factory);

            paramsStack.addFirst(factory.getParameters());
            processParams(tag, SectionHelperFactory.MAIN_BLOCK_NAME,
                    List.of(key + "?=" + defaultValue).iterator(),
                    sectionNode.currentBlock());

            // Init a synthetic section block
            currentScope = scopeStack.peek();
            Scope newScope = factory.initializeBlock(currentScope, sectionNode.currentBlock());
            scopeStack.addFirst(newScope);
            sectionStack.addFirst(sectionNode);
        }
    }

    public TemplateException.Builder error(String message) {
        return error(ParserError.GENERAL_ERROR, message, null);
    }

    private TemplateException.Builder error(ErrorCode code, String message) {
        return error(code, message, origin(0));
    }

    static TemplateException.Builder error(ErrorCode code, String message, Origin origin) {
        return TemplateException.builder()
                .code(code)
                .origin(origin)
                .message("Parser error{#if origin.hasNonGeneratedTemplateId??} in{origin}{/if}: " + message);
    }

    @Override
    public Origin getOrigin() {
        return origin(0);
    }

    private void processParams(String tag, String label, Iterator<String> iter, SectionBlock.Builder block) {
        Map<String, String> params = new LinkedHashMap<>();
        ParametersInfo factoryParamsInfo = paramsStack.peek();
        List<Parameter> factoryParams = factoryParamsInfo.get(label);
        List<String> paramValues = new ArrayList<>();

        while (iter.hasNext()) {
            // Ignore whitespace strings
            String val = iter.next().trim();
            if (!val.isEmpty()) {
                paramValues.add(val);
            }
        }

        int actualSize = paramValues.size();
        if (factoryParamsInfo.isCheckNumberOfParams()
                && actualSize > factoryParams.size()
                && LOGGER.isDebugEnabled()) {
            StringBuilder builder = new StringBuilder("Too many section params for ").append(tag);
            Origin origin = origin(0);
            origin.appendTo(builder);
            builder.append(String.format("[label=%s, params=%s, factoryParams=%s]", label, paramValues, factoryParams));
            LOGGER.debugf(builder.toString());
        }

        List<String> parametersPositions = new ArrayList<>(paramValues.size());

        // Process named params first
        for (String param : paramValues) {
            int equalsPosition = getFirstDeterminingEqualsCharPosition(param);
            if (equalsPosition != -1) {
                // Named param
                String val = param.substring(equalsPosition + 1, param.length());
                params.put(param.substring(0, equalsPosition), val);
                parametersPositions.add(val);
            } else {
                parametersPositions.add(null);
            }
        }

        // Then process positional params
        // When the number of actual params is less than factory params then we need to choose the best fit for positional params
        processPositionalParams(paramValues, parametersPositions, factoryParams, params, actualSize < factoryParams.size());

        // Use the default values if needed
        for (Parameter param : factoryParams) {
            if (param.hasDefaultValue()) {
                params.putIfAbsent(param.name, param.defaultValue);
            }
        }

        // Find undeclared mandatory params
        List<String> undeclaredParams = new ArrayList<>(factoryParams.size());
        for (Parameter param : factoryParams) {
            if (param.isOptional() || params.containsKey(param.name)) {
                continue;
            }
            undeclaredParams.add(param.name);
        }
        if (!undeclaredParams.isEmpty()) {
            throw error(ParserError.MANDATORY_SECTION_PARAMS_MISSING,
                    "mandatory section parameters not declared for {tag}: {undeclaredParams}")
                    .argument("tag", tag)
                    .argument("undeclaredParams", undeclaredParams)
                    .build();
        }

        params.entrySet().forEach(block::addParameter);
        block.setParametersPositions(parametersPositions);
    }

    private void processPositionalParams(List<String> paramValues, List<String> parametersPositions,
            List<Parameter> factoryParams, Map<String, String> params, boolean noDefaultValueTakesPrecedence) {
        int generatedIdx = 0;
        int idx = 0;
        Predicate<String> included = params::containsKey;
        for (String param : paramValues) {
            if (parametersPositions.isEmpty() || parametersPositions.get(idx) == null) {
                Parameter found = findFactoryParameter(param, factoryParams, included, noDefaultValueTakesPrecedence);
                if (found != null) {
                    params.put(found.name, param);
                } else {
                    params.put("" + generatedIdx++, param);
                }
                parametersPositions.set(idx, param);
            }
            idx++;
        }
    }

    private Parameter findFactoryParameter(String paramValue, List<Parameter> factoryParams, Predicate<String> included,
            boolean noDefaultValueTakesPrecedence) {
        if (noDefaultValueTakesPrecedence) {
            for (Parameter param : factoryParams) {
                // Params with no default value take precedence
                if (param.accepts(paramValue) && !param.hasDefaultValue() && !included.test(param.name)) {
                    return param;
                }
            }
        }
        for (Parameter param : factoryParams) {
            if (param.accepts(paramValue) && !included.test(param.name)) {
                return param;
            }
        }
        return null;
    }

    /**
     *
     * @param part
     * @return the index of an equals char outside of any string literal,
     *         <code>-1</code> if no such char is found or if the part represents a composite param
     */
    static int getFirstDeterminingEqualsCharPosition(String part) {
        if (!part.isEmpty() && part.charAt(0) == START_COMPOSITE_PARAM) {
            return -1;
        }
        boolean stringLiteral = false;
        for (int i = 0; i < part.length(); i++) {
            if (LiteralSupport.isStringLiteralSeparator(part.charAt(i))) {
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

    static <B extends ErrorInitializer & WithOrigin> Iterator<String> splitSectionParams(String content, B block) {

        boolean stringLiteral = false;
        short composite = 0;
        byte brackets = 0;
        boolean space = false;
        List<String> parts = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == ' ') {
                if (!space) {
                    if (!stringLiteral && composite == 0 && brackets == 0) {
                        if (buffer.length() > 0) {
                            parts.add(buffer.toString());
                            buffer = new StringBuilder();
                        }
                        space = true;
                    } else {
                        buffer.append(c);
                    }
                }
            } else {
                if (composite == 0
                        && LiteralSupport.isStringLiteralSeparator(c)) {
                    stringLiteral = !stringLiteral;
                } else if (!stringLiteral
                        && isCompositeStart(c) && (i == 0 || space || composite > 0
                                || (buffer.length() > 0 && buffer.charAt(buffer.length() - 1) == '!'))) {
                    composite++;
                } else if (!stringLiteral
                        && isCompositeEnd(c) && composite > 0) {
                    composite--;
                } else if (!stringLiteral
                        && Parser.isLeftBracket(c)) {
                    brackets++;
                } else if (!stringLiteral
                        && Parser.isRightBracket(c) && brackets > 0) {
                    brackets--;
                }
                space = false;
                buffer.append(c);
            }
        }

        if (buffer.length() > 0) {
            if (stringLiteral || composite > 0) {
                throw block.error("unterminated string literal or composite parameter detected for [{content}]")
                        .argument("content", content)
                        .code(ParserError.UNTERMINATED_STRING_LITERAL_OR_COMPOSITE_PARAMETER)
                        .origin(block.getOrigin())
                        .build();
            }
            parts.add(buffer.toString());
        }

        // Try to find/replace "standalone" equals signs used as param names separators
        // This allows for more lenient parsing of named section parameters, e.g. item.name = 'foo' instead of item.name='foo'
        for (ListIterator<String> it = parts.listIterator(); it.hasNext();) {
            if (it.next().equals("=") && it.previousIndex() != 0 && it.hasNext()) {
                // move cursor back
                it.previous();
                String merged = parts.get(it.previousIndex()) + it.next() + it.next();
                // replace the element with the merged value
                it.set(merged);
                // move cursor back and remove previous two elements
                it.previous();
                it.previous();
                it.remove();
                it.previous();
                it.remove();
            }
        }

        return parts.iterator();
    }

    static boolean isCompositeStart(char character) {
        return character == START_COMPOSITE_PARAM;
    }

    static boolean isCompositeEnd(char character) {
        return character == END_COMPOSITE_PARAM;
    }

    enum Tag {

        EXPRESSION(null),
        SECTION('#'),
        SECTION_END('/'),
        PARAM('@'),;

        final Character command;

        Tag(Character command) {
            this.command = command;
        }

        static boolean isCommand(char command) {
            for (Tag tag : Tag.values()) {
                if (tag.command != null && tag.command == command) {
                    return true;
                }
            }
            return false;
        }

    }

    enum State {

        TEXT,
        TAG_INSIDE,
        TAG_INSIDE_STRING_LITERAL,
        TAG_CANDIDATE,
        COMMENT,
        ESCAPE,
        CDATA,
        LINE_SEPARATOR,

    }

    static ExpressionImpl parseExpression(Supplier<Integer> idGenerator, String value, Scope scope, Origin origin) {
        if (value == null || value.isEmpty()) {
            return ExpressionImpl.EMPTY;
        }
        // (foo ?: bar) -> foo ?: bar
        if (value.charAt(0) == START_COMPOSITE_PARAM && value.charAt(value.length() - 1) == END_COMPOSITE_PARAM) {
            value = value.substring(1, value.length() - 1);
        }
        String namespace = null;
        int namespaceIdx = value.indexOf(NAMESPACE_SEPARATOR);
        int spaceIdx;
        int bracketIdx;

        List<String> strParts;
        if (namespaceIdx != -1
                // No space or colon before the space
                && ((spaceIdx = value.indexOf(' ')) == -1 || namespaceIdx < spaceIdx)
                // No bracket or colon before the bracket
                && ((bracketIdx = value.indexOf('(')) == -1 || namespaceIdx < bracketIdx)
                // No string literal
                && !LiteralSupport.isStringLiteralSeparator(value.charAt(0))) {
            // Expression that starts with a namespace
            strParts = Expressions.splitParts(value.substring(namespaceIdx + 1, value.length()));
            namespace = value.substring(0, namespaceIdx);
        } else {
            Object literalValue = LiteralSupport.getLiteralValue(value);
            if (!Results.isNotFound(literalValue)) {
                return ExpressionImpl.literal(idGenerator.get(), value, literalValue, origin);
            }
            strParts = Expressions.splitParts(value);
        }

        if (strParts.isEmpty()) {
            throw error(ParserError.EMPTY_EXPRESSION, "empty expression found \\{{value}\\}", origin)
                    .argument("value", value)
                    .build();
        }

        // Safe expressions
        int lastIdx = strParts.size() - 1;
        String last = strParts.get(lastIdx);
        if (last.endsWith("??")) {
            // foo.val?? -> foo.val.or(null)
            strParts = ImmutableList.<String> builder().addAll(strParts.subList(0, lastIdx))
                    .add(last.substring(0, last.length() - 2)).add("or(null)").build();
        }

        List<Part> parts = new ArrayList<>(strParts.size());
        Part first = null;
        Iterator<String> strPartsIterator = strParts.iterator();
        while (strPartsIterator.hasNext()) {
            Part part = createPart(idGenerator, namespace, first, strPartsIterator, scope, origin, value);
            if (first == null) {
                first = part;
            }
            parts.add(part);
        }
        return new ExpressionImpl(idGenerator.get(), namespace, ImmutableList.copyOf(parts), Results.NotFound.EMPTY, origin);
    }

    private static Part createPart(Supplier<Integer> idGenerator, String namespace, Part first,
            Iterator<String> strPartsIterator, Scope scope, Origin origin, String exprValue) {
        String value = strPartsIterator.next();
        if (Expressions.isVirtualMethod(value)) {
            String name = Expressions.parseVirtualMethodName(value);
            List<String> strParams = new ArrayList<>(Expressions.parseVirtualMethodParams(value, origin, exprValue));
            List<Expression> params = new ArrayList<>(strParams.size());
            Scope paramScope = new Scope(scope);
            for (String strParam : strParams) {
                params.add(parseExpression(idGenerator, strParam.trim(), paramScope, origin));
            }
            // Note that an expression may never start with a virtual method
            String lastPartHint = strPartsIterator.hasNext() ? null : scope.getLastPartHint();
            return new ExpressionImpl.VirtualMethodPartImpl(name, params, lastPartHint);
        }
        // Try to parse the literal for bracket notation
        if (Expressions.isBracketNotation(value)) {
            value = Expressions.parseBracketContent(value, origin, exprValue);
            Object literal = LiteralSupport.getLiteralValue(value);
            if (literal != null && !Results.isNotFound(literal)) {
                value = literal.toString();
            } else {
                throw TemplateException.builder()
                        .message((literal == null ? "Null" : "Non-literal")
                                + " value used in bracket notation [{value}] {origin}")
                        .argument("value", value)
                        .build();
            }
        } else {
            if (!isValidIdentifier(value)) {
                throw error(ParserError.INVALID_IDENTIFIER, "invalid identifier found [{value}]", origin)
                        .argument("value", value)
                        .build();
            }
        }

        String typeInfo = null;
        if (namespace != null) {
            // If a namespace is used and it's the first part then prepend the value with the namespace
            // For example foo -> inject:foo
            typeInfo = first != null ? value : namespace + NAMESPACE_SEPARATOR + value;
        } else if (first == null) {
            // No namespace used and it's the first part
            // Try to find the binding type for the first part of the expression
            typeInfo = scope.getBinding(value);
        } else if (first.getTypeInfo() != null) {
            // No namespace and not the first part
            typeInfo = value;
        }
        if (typeInfo != null && !strPartsIterator.hasNext() && scope.getLastPartHint() != null) {
            // If the type info present then append hint to the last part
            typeInfo += scope.getLastPartHint();
        }
        return new ExpressionImpl.PartImpl(value, typeInfo);
    }

    static boolean isLeftBracket(char character) {
        return character == '(';
    }

    static boolean isRightBracket(char character) {
        return character == ')';
    }

    ExpressionImpl createSectionBlockExpression(BlockInfo block, String value) {
        return parseExpression(expressionIdGenerator::incrementAndGet, value, scopeStack.peek(), block.getOrigin());
    }

    ExpressionImpl createExpression(String value) {
        return parseExpression(expressionIdGenerator::incrementAndGet, value, scopeStack.peek(), origin(value.length() + 1));
    }

    Origin origin(int lineCharacterOffset) {
        return new OriginImpl(line, lineCharacter - lineCharacterOffset, lineCharacter, templateId, generatedId, variant);
    }

    Origin syntheticOrigin() {
        return new OriginImpl(-1, -1, -1, templateId, generatedId, variant);
    }

    private List<List<TemplateNode>> readLines(SectionNode rootNode) {
        List<List<TemplateNode>> lines = new ArrayList<>();
        // Add the last line manually - there is no line separator to trigger flush
        lines.add(readLines(lines, null, rootNode));
        return lines;
    }

    private List<TemplateNode> readLines(List<List<TemplateNode>> lines, List<TemplateNode> currentLine,
            SectionNode sectionNode) {
        if (currentLine == null) {
            currentLine = new ArrayList<>();
        }
        boolean isRoot = ROOT_HELPER_NAME.equals(sectionNode.name);
        if (!isRoot) {
            // Simulate the start tag
            currentLine.add(sectionNode);
        }
        for (SectionBlock block : sectionNode.blocks) {
            if (!isRoot) {
                currentLine.add(BLOCK_NODE);
            }
            for (TemplateNode node : block.nodes) {
                if (node instanceof SectionNode) {
                    currentLine = readLines(lines, currentLine, (SectionNode) node);
                } else if (node instanceof LineSeparatorNode) {
                    // New line separator - flush the line
                    currentLine.add(node);
                    lines.add(currentLine);
                    currentLine = new ArrayList<>();
                } else {
                    currentLine.add(node);
                }
            }
            if (!isRoot) {
                currentLine.add(BLOCK_NODE);
            }
        }
        if (!ROOT_HELPER_NAME.equals(sectionNode.name)) {
            // Simulate the end tag
            currentLine.add(sectionNode);
        }
        return currentLine;
    }

    private boolean isStandalone(List<TemplateNode> line) {
        boolean maybeStandalone = false;
        for (TemplateNode node : line) {
            if (node instanceof ExpressionNode) {
                // Line contains an expression
                return false;
            } else if (node instanceof SectionNode || node instanceof ParameterDeclarationNode || node == BLOCK_NODE
                    || node == COMMENT_NODE) {
                maybeStandalone = true;
            } else if (node instanceof TextNode) {
                if (!isBlank(((TextNode) node).getValue())) {
                    // Line contains a non-whitespace char
                    return false;
                }
            }
        }
        return maybeStandalone;
    }

    private boolean isBlank(CharSequence val) {
        if (val == null) {
            return true;
        }
        int length = val.length();
        if (length == 0) {
            return true;
        }
        for (int i = 0; i < length; i++) {
            if (!Character.isWhitespace(val.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static String toString(Reader in)
            throws IOException {
        if (in instanceof StringReader) {
            return ((StringReader) in).str;
        }
        StringBuilder out = new StringBuilder();
        CharBuffer buffer = CharBuffer.allocate(8192);
        while (in.read(buffer) != -1) {
            buffer.flip();
            out.append(buffer);
            buffer.clear();
        }
        return out.toString();
    }

    static class StringReader extends java.io.StringReader {

        // make the underlying string accessible
        final String str;

        public StringReader(String s) {
            super(s);
            this.str = s;
        }

    }

    static class OriginImpl implements Origin {

        private final int line;
        private final int lineCharacterStart;
        private final int lineCharacterEnd;
        private final String templateId;
        private final String templateGeneratedId;
        private final Optional<Variant> variant;

        OriginImpl(int line, int lineCharacterStart, int lineCharacterEnd, String templateId, String templateGeneratedId,
                Optional<Variant> variant) {
            this.line = line;
            this.lineCharacterStart = lineCharacterStart;
            this.lineCharacterEnd = lineCharacterEnd;
            this.templateId = templateId;
            this.templateGeneratedId = templateGeneratedId;
            this.variant = variant;
        }

        @Override
        public int getLine() {
            return line;
        }

        @Override
        public int getLineCharacterStart() {
            return lineCharacterStart;
        }

        @Override
        public int getLineCharacterEnd() {
            return lineCharacterEnd;
        }

        @Override
        public String getTemplateId() {
            return templateId;
        }

        @Override
        public String getTemplateGeneratedId() {
            return templateGeneratedId;
        }

        public Optional<Variant> getVariant() {
            return variant;
        }

        @Override
        public int hashCode() {
            return Objects.hash(line, templateGeneratedId, templateId, variant);
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
            return line == other.line
                    && Objects.equals(templateGeneratedId, other.templateGeneratedId)
                    && Objects.equals(templateId, other.templateId) && Objects.equals(variant, other.variant);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            appendTo(builder);
            return builder.toString();
        }

    }

    @Override
    public String getTemplateId() {
        return templateId;
    }

    @Override
    public void addParameter(String name, String type) {
        // {@org.acme.Foo foo}
        Scope currentScope = scopeStack.peek();
        currentScope.putBinding(name, Expressions.typeInfoFrom(type));
    }

    @Override
    public void addContentFilter(Function<String, String> filter) {
        contentFilters.add(filter);
    }

    private static final SectionHelper ROOT_SECTION_HELPER = new SectionHelper() {
        @Override
        public CompletionStage<ResultNode> resolve(SectionResolutionContext context) {
            return context.execute();
        }
    };
    private static final SectionHelperFactory<SectionHelper> ROOT_SECTION_HELPER_FACTORY = new SectionHelperFactory<SectionHelper>() {
        @Override
        public SectionHelper initialize(SectionInitContext context) {
            return ROOT_SECTION_HELPER;
        }
    };

    private static final BlockNode BLOCK_NODE = new BlockNode();
    static final CommentNode COMMENT_NODE = new CommentNode();

    // A dummy node for section blocks, it's only used when removing standalone lines
    private static class BlockNode implements TemplateNode {

        @Override
        public CompletionStage<ResultNode> resolve(ResolutionContext context) {
            throw new IllegalStateException();
        }

        @Override
        public Origin getOrigin() {
            throw new IllegalStateException();
        }

    }

    // A dummy node for comments, it's only used when removing standalone lines
    static class CommentNode implements TemplateNode {

        @Override
        public CompletionStage<ResultNode> resolve(ResolutionContext context) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Origin getOrigin() {
            throw new UnsupportedOperationException();
        }

    }

}
