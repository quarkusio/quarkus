package io.quarkus.qute;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.quarkus.qute.SectionHelperFactory.SectionInitContext;

final class SectionInitContextImpl implements SectionInitContext {

    private final EngineImpl engine;
    private final List<SectionBlock> blocks;
    private final ErrorInitializer errorInitializer;
    private final Supplier<Template> currentTemplate;
    private final String name;

    SectionInitContextImpl(EngineImpl engine, List<SectionBlock> blocks, ErrorInitializer errorInitializer,
            Supplier<Template> currentTemplate, String name) {
        this.engine = engine;
        this.blocks = blocks;
        this.errorInitializer = errorInitializer;
        this.currentTemplate = currentTemplate;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     *
     * @return the params of the main block
     */
    public Map<String, String> getParameters() {
        return blocks.isEmpty() ? Collections.emptyMap() : blocks.get(0).parameters;
    }

    public boolean hasParameter(String name) {
        return getParameters().containsKey(name);
    }

    public String getParameter(String name) {
        return getParameters().get(name);
    }

    @Override
    public Expression getExpression(String parameterName) {
        return blocks.isEmpty() ? null : blocks.get(0).expressions.get(parameterName);
    }

    @Override
    public ExpressionImpl parseValue(String value) {
        return ExpressionImpl.from(value);
    }

    public List<SectionBlock> getBlocks() {
        return blocks;
    }

    @Override
    public EngineImpl getEngine() {
        return engine;
    }

    @Override
    public Supplier<Template> getCurrentTemplate() {
        return currentTemplate;
    }

    @Override
    public TemplateException.Builder error(String message) {
        return errorInitializer.error(message);
    }

}
