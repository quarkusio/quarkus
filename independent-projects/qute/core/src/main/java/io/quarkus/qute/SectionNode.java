package io.quarkus.qute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.quarkus.qute.ResolutionContextImpl.ChildResolutionContext;
import io.quarkus.qute.SectionHelper.SectionResolutionContext;

/**
 * Section node.
 */
public class SectionNode implements TemplateNode {

    private static final Logger LOG = Logger.getLogger("io.quarkus.qute.nodeResolve");

    static Builder builder(String helperName, Origin origin, Parser parser,
            ErrorInitializer errorFun) {
        return new Builder(helperName, origin, parser, errorFun);
    }

    final String name;
    final List<SectionBlock> blocks;
    final SectionHelper helper;
    private final Origin origin;
    private final boolean traceLevel;

    SectionNode(String name, List<SectionBlock> blocks, SectionHelper helper, Origin origin) {
        this.name = name;
        this.blocks = blocks;
        this.helper = helper;
        this.origin = origin;
        this.traceLevel = LOG.isTraceEnabled();
    }

    public CompletionStage<ResultNode> resolve(ResolutionContext context, Map<String, Object> params) {
        if (params == null) {
            params = Collections.emptyMap();
        }
        if (traceLevel && !Parser.ROOT_HELPER_NAME.equals(name)) {
            LOG.tracef("Resolve {#%s} started:%s", name, origin);
            return helper.resolve(new SectionResolutionContextImpl(context, params)).thenApply(r -> {
                LOG.tracef("Resolve {#%s} completed:%s", name, origin);
                return r;
            });
        }
        return helper.resolve(new SectionResolutionContextImpl(context, params));
    }

    @Override
    public CompletionStage<ResultNode> resolve(ResolutionContext context) {
        return resolve(context, null);
    }

    public Origin getOrigin() {
        return origin;
    }

    @Override
    public Kind kind() {
        return Kind.SECTION;
    }

    @Override
    public SectionNode asSection() {
        return this;
    }

    public String getName() {
        return name;
    }

    public List<SectionBlock> getBlocks() {
        return blocks;
    }

    public SectionHelper getHelper() {
        return helper;
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

    public Expression findExpression(Predicate<Expression> predicate) {
        for (SectionBlock block : blocks) {
            Expression found = block.findExpression(predicate);
            if (found != null) {
                return found;
            }
        }
        return null;
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

    TemplateNode findNode(Predicate<TemplateNode> predicate) {
        for (SectionBlock block : blocks) {
            TemplateNode found = block.findNode(predicate);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    List<TemplateNode> findNodes(Predicate<TemplateNode> predicate) {
        List<TemplateNode> ret = null;
        for (SectionBlock block : blocks) {
            List<TemplateNode> found = block.findNodes(predicate);
            if (!found.isEmpty()) {
                if (ret == null) {
                    ret = new ArrayList<>();
                }
                ret.addAll(found);
            }
        }
        return ret == null ? Collections.emptyList() : ret;
    }

    static class Builder {

        final String helperName;
        final Origin origin;
        private final List<SectionBlock.Builder> blocks;
        private SectionBlock.Builder currentBlock;
        SectionHelperFactory<?> factory;
        private EngineImpl engine;
        private final ErrorInitializer errorInitializer;

        Builder(String helperName, Origin origin, Parser parser, ErrorInitializer errorInitializer) {
            this.helperName = helperName;
            this.origin = origin;
            this.blocks = new ArrayList<>();
            this.errorInitializer = errorInitializer;
            // The main block is always present
            addBlock(SectionBlock
                    .builder(SectionHelperFactory.MAIN_BLOCK_NAME, parser, errorInitializer)
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

        SectionNode build(Supplier<Template> currentTemlate) {
            ImmutableList.Builder<SectionBlock> builder = ImmutableList.builder();
            for (SectionBlock.Builder block : blocks) {
                builder.add(block.build());
            }
            List<SectionBlock> blocks = builder.build();
            return new SectionNode(helperName, blocks,
                    factory.initialize(
                            new SectionInitContextImpl(engine, blocks, errorInitializer, currentTemlate, helperName)),
                    origin);
        }

    }

    class SectionResolutionContextImpl implements SectionResolutionContext {

        private final Map<String, Object> params;
        private final ResolutionContext resolutionContext;

        public SectionResolutionContextImpl(ResolutionContext resolutionContext, Map<String, Object> params) {
            this.resolutionContext = resolutionContext;
            this.params = params;
        }

        @Override
        public CompletionStage<Map<String, Object>> evaluate(Map<String, Expression> parameters) {
            return Futures.evaluateParams(parameters, resolutionContext);
        }

        @Override
        public CompletionStage<ResultNode> execute(SectionBlock block, ResolutionContext context) {
            if (block == null) {
                // Use the main block
                block = blocks.get(0);
            }
            return Results.resolveAndProcess(block.nodes, context);
        }

        @Override
        public ResolutionContext resolutionContext() {
            return resolutionContext;
        }

        @Override
        public ResolutionContext newResolutionContext(Object data, Map<String, SectionBlock> extendingBlocks) {
            if (resolutionContext instanceof ResolutionContextImpl rc) {
                return new ResolutionContextImpl(data, resolutionContext.getEvaluator(), extendingBlocks,
                        rc.getTemplateInstance());
            } else if (resolutionContext instanceof ChildResolutionContext child) {
                return new ResolutionContextImpl(data, resolutionContext.getEvaluator(), extendingBlocks,
                        child.getTemplateInstance());
            }
            throw new IllegalStateException();
        }

        @Override
        public Map<String, Object> getParameters() {
            return params;
        }

    }

}
