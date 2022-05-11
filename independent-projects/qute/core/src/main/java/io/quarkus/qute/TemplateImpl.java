package io.quarkus.qute;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.jboss.logging.Logger;

class TemplateImpl implements Template {

    private static final Logger LOG = Logger.getLogger(TemplateImpl.class);

    private final String templateId;
    private final String generatedId;
    private final EngineImpl engine;
    private final Optional<Variant> variant;
    final SectionNode root;

    TemplateImpl(EngineImpl engine, SectionNode root, String templateId, String generatedId, Optional<Variant> variant) {
        this.engine = engine;
        this.root = root;
        this.templateId = templateId;
        this.generatedId = generatedId;
        this.variant = variant;
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
    public List<ParameterDeclaration> getParameterDeclarations() {
        return root.getParameterDeclarations();
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
                        if (!renderedActions.isEmpty()) {
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
            return "data";
        }

    }

}
