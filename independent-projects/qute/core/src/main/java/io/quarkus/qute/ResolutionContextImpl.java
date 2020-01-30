package io.quarkus.qute;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

class ResolutionContextImpl implements ResolutionContext {

    private final ResolutionContextImpl parent;
    private final Object data;
    private final List<NamespaceResolver> namespaceResolvers;
    private final Evaluator evaluator;
    private final Map<String, SectionBlock> extendingBlocks;

    ResolutionContextImpl(ResolutionContextImpl parent, Object data, List<NamespaceResolver> namespaceResolvers,
            Evaluator evaluator, Map<String, SectionBlock> extendingBlocks) {
        this.parent = parent;
        this.data = data;
        this.namespaceResolvers = namespaceResolvers;
        this.evaluator = evaluator;
        this.extendingBlocks = extendingBlocks;
    }

    @Override
    public CompletionStage<Object> evaluate(String value) {
        return evaluate(Expression.from(value));
    }

    @Override
    public CompletionStage<Object> evaluate(Expression expression) {
        return evaluator.evaluate(expression, this);
    }

    @Override
    public ResolutionContext createChild(Object data, List<NamespaceResolver> namespaceResolvers) {
        return new ResolutionContextImpl(this, data, namespaceResolvers, evaluator, null);
    }

    @Override
    public ResolutionContext createChild(Map<String, SectionBlock> extendingBlocks) {
        return new ResolutionContextImpl(this, data, namespaceResolvers, evaluator, extendingBlocks);
    }

    @Override
    public Object getData() {
        return data;
    }

    @Override
    public List<NamespaceResolver> getNamespaceResolvers() {
        return namespaceResolvers;
    }

    @Override
    public ResolutionContextImpl getParent() {
        return parent;
    }

    @Override
    public SectionBlock getExtendingBlock(String name) {
        if (extendingBlocks != null) {
            SectionBlock block = extendingBlocks.get(name);
            if (block != null) {
                return block;
            }
            if (parent != null) {
                return parent.getExtendingBlock(name);
            }
        }
        return null;
    }

}
