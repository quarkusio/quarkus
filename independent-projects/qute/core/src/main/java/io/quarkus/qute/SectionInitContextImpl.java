package io.quarkus.qute;

import io.quarkus.qute.SectionHelperFactory.SectionInitContext;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class SectionInitContextImpl implements SectionInitContext {

    private final EngineImpl engine;
    private final List<SectionBlock> blocks;
    private final ErrorInitializer errorInitializer;

    public SectionInitContextImpl(EngineImpl engine, List<SectionBlock> blocks, ErrorInitializer errorInitializer) {
        this.engine = engine;
        this.blocks = blocks;
        this.errorInitializer = errorInitializer;
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
    public TemplateException.Builder error(String message) {
        return errorInitializer.error(message);
    }

}
