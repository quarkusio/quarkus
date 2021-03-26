package io.quarkus.qute;

import java.util.Map;
import java.util.concurrent.CompletionStage;

class ResolutionContextImpl implements ResolutionContext {

    private final Object data;
    private final Evaluator evaluator;
    private final Map<String, SectionBlock> extendingBlocks;
    private final TemplateInstance templateInstance;

    ResolutionContextImpl(Object data,
            Evaluator evaluator, Map<String, SectionBlock> extendingBlocks, TemplateInstance templateInstance) {
        this.data = data;
        this.evaluator = evaluator;
        this.extendingBlocks = extendingBlocks;
        this.templateInstance = templateInstance;
    }

    @Override
    public CompletionStage<Object> evaluate(String value) {
        return evaluate(ExpressionImpl.from(value));
    }

    @Override
    public CompletionStage<Object> evaluate(Expression expression) {
        return evaluator.evaluate(expression, this);
    }

    @Override
    public ResolutionContext createChild(Object data, Map<String, SectionBlock> extendingBlocks) {
        return new ChildResolutionContext(this, data, extendingBlocks);
    }

    @Override
    public Object getData() {
        return data;
    }

    @Override
    public ResolutionContextImpl getParent() {
        return null;
    }

    @Override
    public SectionBlock getExtendingBlock(String name) {
        if (extendingBlocks != null) {
            return extendingBlocks.get(name);
        }
        return null;
    }

    @Override
    public Object getAttribute(String key) {
        return templateInstance.getAttribute(key);
    }

    @Override
    public Evaluator getEvaluator() {
        return evaluator;
    }

    static class ChildResolutionContext implements ResolutionContext {

        private final ResolutionContext parent;
        private final Object data;
        private final Map<String, SectionBlock> extendingBlocks;
        private final Evaluator evaluator;

        public ChildResolutionContext(ResolutionContext parent, Object data, Map<String, SectionBlock> extendingBlocks) {
            this.parent = parent;
            this.data = data;
            this.extendingBlocks = extendingBlocks;
            this.evaluator = parent.getEvaluator();
        }

        @Override
        public CompletionStage<Object> evaluate(String expression) {
            return evaluate(ExpressionImpl.from(expression));
        }

        @Override
        public CompletionStage<Object> evaluate(Expression expression) {
            // Make sure we use the correct resolution context
            return evaluator.evaluate(expression, this);
        }

        @Override
        public ResolutionContext createChild(Object data, Map<String, SectionBlock> extendingBlocks) {
            return new ChildResolutionContext(this, data, extendingBlocks);
        }

        @Override
        public Object getData() {
            return data;
        }

        @Override
        public ResolutionContext getParent() {
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

        @Override
        public Object getAttribute(String key) {
            return parent.getAttribute(key);
        }

        @Override
        public Evaluator getEvaluator() {
            return evaluator;
        }

    }

}
