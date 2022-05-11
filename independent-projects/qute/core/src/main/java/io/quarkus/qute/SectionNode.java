package io.quarkus.qute;

import io.quarkus.qute.SectionHelper.SectionResolutionContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.jboss.logging.Logger;

/**
 * Section node.
 */
class SectionNode implements TemplateNode {

    private static final Logger LOG = Logger.getLogger("io.quarkus.qute.nodeResolve");

    static Builder builder(String helperName, Origin origin, Function<String, Expression> expressionFun,
            Function<String, TemplateException.Builder> errorFun) {
        return new Builder(helperName, origin, expressionFun, errorFun);
    }

    final String name;
    final List<SectionBlock> blocks;
    private final SectionHelper helper;
    private final Origin origin;
    private final boolean traceLevel;

    SectionNode(String name, List<SectionBlock> blocks, SectionHelper helper, Origin origin) {
        this.name = name;
        this.blocks = blocks;
        this.helper = helper;
        this.origin = origin;
        this.traceLevel = LOG.isTraceEnabled();
    }

    @Override
    public CompletionStage<ResultNode> resolve(ResolutionContext context) {
        if (traceLevel && !Parser.ROOT_HELPER_NAME.equals(name)) {
            LOG.tracef("Resolve {#%s} started:%s", name, origin);
            return helper.resolve(new SectionResolutionContextImpl(context)).thenApply(r -> {
                LOG.tracef("Resolve {#%s} completed:%s", name, origin);
                return r;
            });
        }
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

    @Override
    public List<ParameterDeclaration> getParameterDeclarations() {
        List<ParameterDeclaration> declarations = null;
        for (SectionBlock block : blocks) {
            List<ParameterDeclaration> blockDeclarations = block.getParamDeclarations();
            if (!blockDeclarations.isEmpty()) {
                if (declarations == null) {
                    declarations = new ArrayList<>();
                }
                declarations.addAll(blockDeclarations);
            }
        }
        return declarations != null ? declarations : Collections.emptyList();
    }

    static class Builder {

        final String helperName;
        final Origin origin;
        private final List<SectionBlock.Builder> blocks;
        private SectionBlock.Builder currentBlock;
        SectionHelperFactory<?> factory;
        private EngineImpl engine;
        private final Function<String, TemplateException.Builder> errorFun;

        Builder(String helperName, Origin origin, Function<String, Expression> expressionFun,
                Function<String, TemplateException.Builder> errorFun) {
            this.helperName = helperName;
            this.origin = origin;
            this.blocks = new ArrayList<>();
            this.errorFun = errorFun;
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
                    factory.initialize(new SectionInitContextImpl(engine, blocks, errorFun)), origin);
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

        @Override
        public ResolutionContext newResolutionContext(Object data, Map<String, SectionBlock> extendingBlocks) {
            return new ResolutionContextImpl(data, resolutionContext.getEvaluator(), extendingBlocks,
                    resolutionContext::getAttribute);
        }

    }

}
