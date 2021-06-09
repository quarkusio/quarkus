package io.quarkus.qute;

import io.quarkus.qute.SectionHelper.SectionResolutionContext;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
            CompletableFuture<ResultNode> result = new CompletableFuture<ResultNode>();

            // Collect async results first 
            @SuppressWarnings("unchecked")
            CompletableFuture<ResultNode>[] allResults = new CompletableFuture[size];
            List<CompletableFuture<ResultNode>> asyncResults = null;
            int idx = 0;
            for (TemplateNode node : block.nodes) {
                CompletableFuture<ResultNode> nodeResult = node.resolve(context).toCompletableFuture();
                allResults[idx++] = nodeResult;
                if (node.isConstant()) {
                    // Constant blocks do not need to be resolved 
                    continue;
                }
                if (asyncResults == null) {
                    asyncResults = new LinkedList<>();
                }
                asyncResults.add(nodeResult);
            }

            if (asyncResults == null) {
                // No async results present
                result.complete(new MultiResultNode(allResults));
            } else {
                CompletionStage<?> cs;
                if (asyncResults.size() == 1) {
                    cs = asyncResults.get(0);
                } else {
                    cs = CompletableFuture
                            .allOf(asyncResults.toArray(new CompletableFuture[0]));
                }
                cs.whenComplete((v, t) -> {
                    if (t != null) {
                        result.completeExceptionally(t);
                    } else {
                        result.complete(new MultiResultNode(allResults));
                    }
                });
            }
            return result;
        }

        @Override
        public ResolutionContext resolutionContext() {
            return resolutionContext;
        }

    }

}
