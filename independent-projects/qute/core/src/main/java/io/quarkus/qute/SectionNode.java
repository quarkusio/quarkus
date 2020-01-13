package io.quarkus.qute;

import io.quarkus.qute.SectionHelper.SectionResolutionContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Section node.
 */
class SectionNode implements TemplateNode {

    static Builder builder(String helperName, Origin origin) {
        return new Builder(helperName, origin);
    }

    final List<SectionBlock> blocks;

    private final SectionHelper helper;

    private final Origin origin;

    SectionNode(List<SectionBlock> blocks, SectionHelper helper, Origin origin) {
        this.blocks = ImmutableList.copyOf(blocks);
        this.helper = helper;
        this.origin = origin;
    }

    @Override
    public CompletionStage<ResultNode> resolve(ResolutionContext context) {
        return helper.resolve(new SectionResolutionContextImpl(context));
    }

    public Origin getOrigin() {
        return origin;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SectionNode [helper=").append(helper.getClass().getSimpleName()).append(", origin= ").append(origin)
                .append("]");
        return builder.toString();
    }

    public Set<Expression> getExpressions() {
        Set<Expression> expressions = new HashSet<>();
        for (SectionBlock block : blocks) {
            expressions.addAll(block.getExpressions());
        }
        return expressions;
    }

    static class Builder {

        final String helperName;
        final Origin origin;
        private final List<SectionBlock> blocks;
        SectionHelperFactory<?> factory;
        private EngineImpl engine;

        public Builder(String helperName, Origin origin) {
            this.helperName = helperName;
            this.origin = origin;
            this.blocks = new ArrayList<>();
        }

        Builder addBlock(SectionBlock block) {
            this.blocks.add(block);
            return this;
        }

        Builder setHelperFactory(SectionHelperFactory<?> factory) {
            this.factory = factory;
            return this;
        }

        Builder setEngine(EngineImpl engine) {
            this.engine = engine;
            return this;
        }

        SectionNode build() {
            return new SectionNode(blocks,
                    factory.initialize(new SectionInitContextImpl(engine, blocks, this::createParserError)), origin);
        }

        TemplateException createParserError(String message) {
            StringBuilder builder = new StringBuilder("Parser error");
            if (!origin.getTemplateId().equals(origin.getTemplateGeneratedId())) {
                builder.append(" in template [").append(origin.getTemplateId()).append("]");
            }
            builder.append(" on line ").append(origin.getLine()).append(": ")
                    .append(message);
            return new TemplateException(origin,
                    builder.toString());
        }

    }

    class SectionResolutionContextImpl implements SectionResolutionContext {

        private final ResolutionContext resolutionContext;

        public SectionResolutionContextImpl(ResolutionContext resolutionContext) {
            this.resolutionContext = resolutionContext;
        }

        @Override
        public CompletionStage<ResultNode> execute(SectionBlock block, ResolutionContext context) {
            if (block == null) {
                // Use the main block
                block = blocks.get(0);
            }
            if (block.nodes.size() == 1) {
                return block.nodes.get(0).resolve(context);
            }
            CompletableFuture<ResultNode> result = new CompletableFuture<ResultNode>();
            @SuppressWarnings("unchecked")
            CompletableFuture<ResultNode>[] results = new CompletableFuture[block.nodes.size()];
            int idx = 0;
            for (TemplateNode node : block.nodes) {
                results[idx++] = node.resolve(context).toCompletableFuture();
            }
            CompletableFuture
                    .allOf(results)
                    .whenComplete((v, t) -> {
                        if (t != null) {
                            result.completeExceptionally(t);
                        } else {
                            result.complete(new MultiResultNode(results));
                        }
                    });
            return result;
        }

        @Override
        public ResolutionContext resolutionContext() {
            return resolutionContext;
        }

    }

}
