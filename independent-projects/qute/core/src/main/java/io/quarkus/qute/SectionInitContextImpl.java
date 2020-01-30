package io.quarkus.qute;

import io.quarkus.qute.SectionHelperFactory.SectionInitContext;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 
 */
final class SectionInitContextImpl implements SectionInitContext {

    private final EngineImpl engine;
    private final List<SectionBlock> blocks;
    private final Function<String, TemplateException> errorFun;

    public SectionInitContextImpl(EngineImpl engine, List<SectionBlock> blocks, Function<String, TemplateException> errorFun) {
        this.engine = engine;
        this.blocks = blocks;
        this.errorFun = errorFun;
    }

    /**
     * 
     * @return the params of the main block
     */
    public Map<String, String> getParameters() {
        return blocks.get(0).parameters;
    }

    public boolean hasParameter(String name) {
        return getParameters().containsKey(name);
    }

    public String getParameter(String name) {
        return getParameters().get(name);
    }

    @Override
    public Expression getExpression(String parameterName) {
        return blocks.get(0).expressions.get(parameterName);
    }

    @Override
    public Expression parseValue(String value) {
        return Expression.from(value);
    }

    public List<SectionBlock> getBlocks() {
        return blocks;
    }

    @Override
    public EngineImpl getEngine() {
        return engine;
    }

    @Override
    public TemplateException createParserError(String message) {
        return errorFun.apply(message);
    }

}
