package io.quarkus.qute;

import static io.quarkus.qute.Namespaces.DATA_NAMESPACE;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

class TemplateImpl implements Template {

    private static final Logger LOG = Logger.getLogger(TemplateImpl.class);

    private final String templateId;
    private final String generatedId;
    private final EngineImpl engine;
    private final Optional<Variant> variant;
    final SectionNode root;
    private final List<ParameterDeclaration> parameterDeclarations;
    private final LazyValue<Map<String, Fragment>> fragments;

    TemplateImpl(EngineImpl engine, SectionNode root, String templateId, String generatedId, Optional<Variant> variant) {
        this.engine = engine;
        this.root = root;
        this.templateId = templateId;
        this.generatedId = generatedId;
        this.variant = variant;
        // Note that param declarations can be removed if placed on a standalone line
        this.parameterDeclarations = ImmutableList.copyOf(root.getParameterDeclarations());
        // Use a lazily initialized map to avoid unnecessary performance costs during parsing
        this.fragments = initFragments(root);
    }

    @Override
    public TemplateInstance instance() {
        TemplateInstance instance = new TemplateInstanceImpl();
        if (!engine.initializers.isEmpty()) {
            for (TemplateInstance.Initializer initializer : engine.initializers) {
                initializer.accept(instance);
            }
        }
        return instance;
    }

    @Override
    public List<Expression> getExpressions() {
        return root.getExpressions();
    }

    @Override
    public Expression findExpression(Predicate<Expression> predicate) {
        return root.findExpression(predicate);
    }

    @Override
    public List<ParameterDeclaration> getParameterDeclarations() {
        return parameterDeclarations;
    }

    @Override
    public String getGeneratedId() {
        return generatedId;
    }

    @Override
    public String getId() {
        return templateId;
    }

    @Override
    public Optional<Variant> getVariant() {
        return variant;
    }

    @Override
    public String toString() {
        return "Template " + templateId + " [generatedId=" + generatedId + "]";
    }

    @Override
    public Fragment getFragment(String identifier) {
        return fragments != null ? fragments.get().get(Objects.requireNonNull(identifier)) : null;
    }

    @Override
    public Set<String> getFragmentIds() {
        return fragments != null ? Set.copyOf(fragments.get().keySet()) : Set.of();
    }

    private LazyValue<Map<String, Fragment>> initFragments(SectionNode section) {
        if (section.name.equals(Parser.ROOT_HELPER_NAME)) {
            // Initialize the lazy map for root sections only
            return new LazyValue<>(new Supplier<Map<String, Fragment>>() {

                @Override
                public Map<String, Fragment> get() {
                    Predicate<TemplateNode> isFragmentNode = new Predicate<TemplateNode>() {
                        @Override
                        public boolean test(TemplateNode node) {
                            if (!node.isSection()) {
                                return false;
                            }
                            SectionNode sectionNode = (SectionNode) node;
                            return sectionNode.helper instanceof FragmentSectionHelper;
                        }
                    };
                    List<TemplateNode> fragmentNodes = section.findNodes(isFragmentNode);
                    if (fragmentNodes.isEmpty()) {
                        return Collections.emptyMap();
                    } else {
                        Map<String, Fragment> fragments = new HashMap<>();
                        for (TemplateNode fragmentNode : fragmentNodes) {
                            FragmentSectionHelper helper = (FragmentSectionHelper) ((SectionNode) fragmentNode).helper;
                            Fragment fragment = new FragmentImpl(engine, (SectionNode) fragmentNode, helper.getIdentifier(),
                                    engine.generateId(), variant);
                            fragments.put(helper.getIdentifier(), fragment);
                        }
                        return fragments;
                    }
                }
            });
        }
        return null;
    }

    private class TemplateInstanceImpl extends TemplateInstanceBase {

        @Override
        public String render() {
            long timeout = getTimeout();
            try {
                return renderAsyncNoTimeout().toCompletableFuture().get(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            } catch (TimeoutException e) {
                throw newTimeoutException(timeout);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new IllegalStateException(e.getCause());
                }
            }
        }

        @Override
        public Multi<String> createMulti() {
            Multi<String> multi = Multi.createFrom().emitter(emitter -> renderData(data(), emitter::emit)
                    .whenComplete((r, f) -> {
                        if (f == null) {
                            emitter.complete();
                        } else {
                            emitter.fail(f);
                        }
                    }));
            if (engine.useAsyncTimeout()) {
                long timeout = getTimeout();
                multi = multi.ifNoItem()
                        .after(Duration.ofMillis(timeout))
                        .failWith(() -> newTimeoutException(timeout));
            }
            return multi;
        }

        @Override
        public Uni<String> createUni() {
            Uni<String> uni = Uni.createFrom().completionStage(this::renderAsyncNoTimeout);
            if (engine.useAsyncTimeout()) {
                long timeout = getTimeout();
                uni = uni.ifNoItem()
                        .after(Duration.ofMillis(timeout))
                        .failWith(() -> newTimeoutException(timeout));
            }
            return uni;
        }

        @Override
        public CompletionStage<String> renderAsync() {
            CompletionStage<String> cs = renderAsyncNoTimeout();
            if (engine.useAsyncTimeout()) {
                cs = cs.toCompletableFuture().orTimeout(getTimeout(), TimeUnit.MILLISECONDS);
            }
            return cs;
        }

        @Override
        public CompletionStage<Void> consume(Consumer<String> resultConsumer) {
            CompletionStage<Void> cs = renderData(data(), resultConsumer);
            if (engine.useAsyncTimeout()) {
                cs = cs.toCompletableFuture().orTimeout(getTimeout(), TimeUnit.MILLISECONDS);
            }
            return cs;
        }

        private TemplateException newTimeoutException(long timeout) {
            return new TemplateException(TemplateImpl.this.toString() + " rendering timeout [" + timeout + "ms] occured");
        }

        @Override
        protected Engine engine() {
            return engine;
        }

        private CompletionStage<String> renderAsyncNoTimeout() {
            StringBuilder builder = new StringBuilder(1028);
            return renderData(data(), builder::append).thenApply(v -> builder.toString());
        }

        private CompletionStage<Void> renderData(Object data, Consumer<String> consumer) {
            CompletableFuture<Void> result = new CompletableFuture<>();
            ResolutionContext rootContext = new ResolutionContextImpl(data,
                    engine.getEvaluator(), null, this::getAttribute);
            setAttribute(DataNamespaceResolver.ROOT_CONTEXT, rootContext);
            // Async resolution
            root.resolve(rootContext).whenComplete((r, t) -> {
                if (t != null) {
                    result.completeExceptionally(t);
                } else {
                    // Sync processing of the result tree - build the output
                    try {
                        r.process(consumer);
                        result.complete(null);
                    } catch (Throwable e) {
                        result.completeExceptionally(e);
                    } finally {
                        if (renderedActions != null) {
                            for (Runnable action : renderedActions) {
                                try {
                                    action.run();
                                } catch (Throwable e) {
                                    LOG.error("Unable to perform an action when rendering finished", e);
                                }
                            }
                        }

                    }
                }
            });
            return result;
        }

        @Override
        public Template getTemplate() {
            return TemplateImpl.this;
        }

        @Override
        public String toString() {
            return "Instance of " + TemplateImpl.this.toString();
        }

    }

    class FragmentImpl extends TemplateImpl implements Fragment {

        public FragmentImpl(EngineImpl engine, SectionNode root, String fragmentId, String generatedId,
                Optional<Variant> variant) {
            super(engine, root, fragmentId, generatedId, variant);
        }

        @Override
        public Fragment getFragment(String id) {
            return TemplateImpl.this.getFragment(id);
        }

        @Override
        public Set<String> getFragmentIds() {
            return TemplateImpl.this.getFragmentIds();
        }

        @Override
        public Template getOriginalTemplate() {
            return TemplateImpl.this;
        }

        @Override
        public TemplateInstance instance() {
            TemplateInstance instance = super.instance();
            instance.setAttribute(Fragment.ATTRIBUTE, true);
            return instance;
        }

    }

    static class DataNamespaceResolver implements NamespaceResolver {

        static final String ROOT_CONTEXT = "qute$rootContext";

        @Override
        public CompletionStage<Object> resolve(EvalContext context) {
            Object rootContext = context.getAttribute(ROOT_CONTEXT);
            if (rootContext != null && rootContext instanceof ResolutionContext) {
                return ((ResolutionContext) rootContext).evaluate(context.getName());
            }
            return Results.notFound(context);
        }

        @Override
        public String getNamespace() {
            return DATA_NAMESPACE;
        }

    }

}
