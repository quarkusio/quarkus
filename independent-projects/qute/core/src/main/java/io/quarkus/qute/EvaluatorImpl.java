package io.quarkus.qute;

import io.quarkus.qute.Expression.Part;
import io.quarkus.qute.ExpressionImpl.PartImpl;
import io.quarkus.qute.Results.Result;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.jboss.logging.Logger;

/**
 * 
 */
class EvaluatorImpl implements Evaluator {

    private static final Logger LOGGER = Logger.getLogger(EvaluatorImpl.class);

    private final List<ValueResolver> resolvers;

    EvaluatorImpl(List<ValueResolver> valueResolvers) {
        this.resolvers = valueResolvers;
    }

    @Override
    public CompletionStage<Object> evaluate(Expression expression, ResolutionContext resolutionContext) {
        Iterator<Part> parts;
        if (expression.hasNamespace()) {
            parts = expression.getParts().iterator();
            NamespaceResolver resolver = findNamespaceResolver(expression.getNamespace(), resolutionContext);
            if (resolver == null) {
                LOGGER.errorf("No namespace resolver found for: %s", expression.getNamespace());
                return Futures.failure(new TemplateException("No resolver for namespace: " + expression.getNamespace()));
            }
            EvalContext context = new EvalContextImpl(false, null, parts.next(), resolutionContext);
            LOGGER.debugf("Found '%s' namespace resolver: %s", expression.getNamespace(), resolver.getClass());
            return resolver.resolve(context).thenCompose(r -> {
                if (parts.hasNext()) {
                    return resolveReference(false, r, parts, resolutionContext);
                } else {
                    return CompletableFuture.completedFuture(r);
                }
            });
        } else {
            if (expression.isLiteral()) {
                return expression.getLiteralValue();
            } else {
                parts = expression.getParts().iterator();
                return resolveReference(true, resolutionContext.getData(), parts, resolutionContext);
            }
        }
    }

    private NamespaceResolver findNamespaceResolver(String namespace, ResolutionContext resolutionContext) {
        if (resolutionContext == null) {
            return null;
        }
        if (resolutionContext.getNamespaceResolvers() != null) {
            for (NamespaceResolver resolver : resolutionContext.getNamespaceResolvers()) {
                if (resolver.getNamespace().equals(namespace)) {
                    return resolver;
                }
            }
        }
        return findNamespaceResolver(namespace, resolutionContext.getParent());
    }

    private CompletionStage<Object> resolveReference(boolean tryParent, Object ref, Iterator<Part> parts,
            ResolutionContext resolutionContext) {
        Part part = parts.next();
        EvalContextImpl evalContext = new EvalContextImpl(tryParent, ref, part, resolutionContext);
        if (!parts.hasNext()) {
            // The last part - no need to compose
            return resolve(evalContext, resolvers.iterator(), true);
        } else {
            // Next part - no need to try the parent context/outer scope
            return resolve(evalContext, resolvers.iterator(), true)
                    .thenCompose(r -> resolveReference(false, r, parts, resolutionContext));
        }
    }

    @SuppressWarnings("unchecked")
    private CompletionStage<Object> resolve(EvalContextImpl evalContext, Iterator<ValueResolver> resolvers,
            boolean tryCachedResolver) {

        if (tryCachedResolver) {
            // Try the cached resolver first
            ValueResolver cachedResolver = ((PartImpl) evalContext.part).cachedResolver;
            if (cachedResolver != null && cachedResolver.appliesTo(evalContext)) {
                return cachedResolver.resolve(evalContext).thenCompose(r -> {
                    if (Result.NOT_FOUND.equals(r)) {
                        return resolve(evalContext, resolvers, false);
                    } else {
                        if (r instanceof CompletionStage) {
                            // If the result is a completion stage return it as is
                            return (CompletionStage<Object>) r;
                        }
                        return CompletableFuture.completedFuture(r);
                    }
                });
            }
        }

        if (!resolvers.hasNext()) {
            ResolutionContext parent = evalContext.resolutionContext.getParent();
            if (evalContext.tryParent && parent != null) {
                // Continue with parent context
                return resolve(
                        new EvalContextImpl(true, parent.getData(), evalContext.name, evalContext.params, parent,
                                evalContext.part),
                        this.resolvers.iterator(), false);
            }
            LOGGER.tracef("Unable to resolve %s", evalContext);
            return Results.NOT_FOUND;
        }
        ValueResolver resolver = resolvers.next();
        if (resolver.appliesTo(evalContext)) {
            return resolver.resolve(evalContext).thenCompose(r -> {
                if (Result.NOT_FOUND.equals(r)) {
                    // Result not found - try the next resolver
                    return resolve(evalContext, resolvers, false);
                } else {
                    // Cache the first resolver where a result is found
                    ((PartImpl) evalContext.part).setCachedResolver(resolver);
                    if (r instanceof CompletionStage) {
                        // If the result is a completion stage return it as is
                        return (CompletionStage<Object>) r;
                    }
                    return CompletableFuture.completedFuture(r);
                }
            });
        } else {
            // Try the next resolver
            return resolve(evalContext, resolvers, false);
        }
    }

    static class EvalContextImpl implements EvalContext {

        final boolean tryParent;
        final Object base;
        final String name;
        final List<Expression> params;
        final ResolutionContext resolutionContext;
        final Part part;

        EvalContextImpl(boolean tryParent, Object base, Part part, ResolutionContext resolutionContext) {
            this(tryParent, base, part.getName(),
                    part.isVirtualMethod() ? part.asVirtualMethod().getParameters() : Collections.emptyList(),
                    resolutionContext, part);
        }

        EvalContextImpl(boolean tryParent, Object base, String name, List<Expression> params,
                ResolutionContext resolutionContext, Part part) {
            this.tryParent = tryParent;
            this.base = base;
            this.resolutionContext = resolutionContext;
            this.params = params;
            this.name = name;
            this.part = part;
        }

        @Override
        public Object getBase() {
            return base;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public List<Expression> getParams() {
            return params;
        }

        @Override
        public CompletionStage<Object> evaluate(String value) {
            return evaluate(ExpressionImpl.from(value));
        }

        @Override
        public CompletionStage<Object> evaluate(Expression expression) {
            return resolutionContext.evaluate(expression);
        }

        @Override
        public Object getAttribute(String key) {
            return resolutionContext.getAttribute(key);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("EvalContextImpl [tryParent=").append(tryParent).append(", base=").append(base).append(", name=")
                    .append(name).append(", params=").append(params).append("]");
            return builder.toString();
        }

    }

}
