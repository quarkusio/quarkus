package io.quarkus.qute;

import io.quarkus.qute.SectionHelper.SectionResolutionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Section node.
 */
class SectionNode implements TemplateNode {

    static Builder builder(String helperName, Origin origin, Function<String, Expression> expressionFun,
            Function<String, TemplateException> errorFun) {
        return new Builder(helperName, origin, expressionFun, errorFun);
    }

    final String name;
    final List<SectionBlock> blocks;
    private final SectionHelper helper;
    private final Origin origin;

    SectionNode(String name, List<SectionBlock> blocks, SectionHelper helper, Origin origin) {
        this.name = name;
        this.blocks = blocks;
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

    void optimizeNodes(Set<TemplateNode> nodes) {
        for (SectionBlock block : blocks) {
            block.optimizeNodes(nodes);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SectionNode [helper=").append(helper.getClass().getSimpleName()).append(", origin= ").append(origin)
                .append("]");
        return builder.toString();
    }

    public List<Expression> getExpressions() {
        List<Expression> expressions = new ArrayList<>();
        for (SectionBlock block : blocks) {
            expressions.addAll(block.getExpressions());
        }
        return expressions;
    }

    static class Builder {

        final String helperName;
        final Origin origin;
        private final List<SectionBlock.Builder> blocks;
        private SectionBlock.Builder currentBlock;
        SectionHelperFactory<?> factory;
        private EngineImpl engine;

        public Builder(String helperName, Origin origin, Function<String, Expression> expressionFun,
                Function<String, TemplateException> errorFun) {
            this.helperName = helperName;
            this.origin = origin;
            this.blocks = new ArrayList<>();
            // The main block is always present 
            addBlock(SectionBlock
                    .builder(SectionHelperFactory.MAIN_BLOCK_NAME, expressionFun, errorFun)
                    .setOrigin(origin));
        }

        Builder addBlock(SectionBlock.Builder block) {
            this.blocks.add(block);
            this.currentBlock = block;
            return this;
        }

        Builder endBlock() {
            // Set main as the current
            this.currentBlock = blocks.get(0);
            return this;
        }

        SectionBlock.Builder currentBlock() {
            return currentBlock;
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
            ImmutableList.Builder<SectionBlock> builder = ImmutableList.builder();
            for (SectionBlock.Builder block : blocks) {
                builder.add(block.build());
            }
            List<SectionBlock> blocks = builder.build();
            return new SectionNode(helperName, blocks,
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
            int size = block.nodes.size();
            if (size == 1) {
                // Single node in the block
                return block.nodes.get(0).resolve(context);
            }
            List<CompletionStage<ResultNode>> results = new ArrayList<>(size);
            for (TemplateNode node : block.nodes) {
                results.add(node.resolve(context));
            }
            return Results.process(results);
        }

        @Override
        public ResolutionContext resolutionContext() {
            return resolutionContext;
        }

    }

}
