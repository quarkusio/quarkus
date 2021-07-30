package io.quarkus.qute;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

class TemplateImpl implements Template {

    private final String generatedId;
    private final EngineImpl engine;
    private final Optional<Variant> variant;
    final SectionNode root;

    TemplateImpl(EngineImpl engine, SectionNode root, String generatedId, Optional<Variant> variant) {
        this.engine = engine;
        this.root = root;
        this.generatedId = generatedId;
        this.variant = variant;
    }

    @Override
    public TemplateInstance instance() {
        return new TemplateInstanceImpl();
    }

    @Override
    public List<Expression> getExpressions() {
        return root.getExpressions();
    }

    @Override
    public String getGeneratedId() {
        return generatedId;
    }

    @Override
    public Optional<Variant> getVariant() {
        return variant;
    }

    private class TemplateInstanceImpl extends TemplateInstanceBase {

        @Override
        public String render() {
            try {
                Object timeoutAttr = getAttribute(TIMEOUT);
                long timeout = timeoutAttr != null ? Long.parseLong(timeoutAttr.toString()) : 10000;
                return renderAsync().toCompletableFuture().get(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            } catch (TimeoutException e) {
                throw new IllegalStateException(e);
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
            return Multi.createFrom().emitter(emitter -> consume(emitter::emit)
                    .whenComplete((r, f) -> {
                        if (f == null) {
                            emitter.complete();
                        } else {
                            emitter.fail(f);
                        }
                    }));
        }

        @Override
        public Uni<String> createUni() {
            return Uni.createFrom().completionStage(this::renderAsync);
        }

        @Override
        public CompletionStage<String> renderAsync() {
            StringBuilder builder = new StringBuilder(1028);
            return renderData(data(), builder::append).thenApply(v -> builder.toString());
        }

        @Override
        public CompletionStage<Void> consume(Consumer<String> resultConsumer) {
            return renderData(data(), resultConsumer);
        }

        private CompletionStage<Void> renderData(Object data, Consumer<String> consumer) {
            CompletableFuture<Void> result = new CompletableFuture<>();
            ResolutionContext rootContext = new ResolutionContextImpl(data,
                    engine.getEvaluator(), null, this);
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
                    }
                }
            });
            return result;
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
