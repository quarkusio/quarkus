package io.quarkus.qute;

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
        Iterator<String> parts;
        if (expression.namespace != null) {
            parts = expression.parts.iterator();
            NamespaceResolver resolver = findNamespaceResolver(expression.namespace, resolutionContext);
            if (resolver == null) {
                LOGGER.errorf("No namespace resolver found for: %s", expression.namespace);
                return Futures.failure(new IllegalStateException("No resolver for namespace: " + expression.namespace));
            }
            EvalContext context = new EvalContextImpl(false, null, parts.next(), resolutionContext);
            LOGGER.debugf("Found '%s' namespace resolver: %s", expression.namespace, resolver.getClass());
            return resolver.resolve(context).thenCompose(r -> {
                if (parts.hasNext()) {
                    return resolveReference(false, r, parts, resolutionContext);
                } else {
                    return CompletableFuture.completedFuture(r);
                }
            });
        } else {
            if (expression.literal != null) {
                return expression.literal;
            } else {
                parts = expression.parts.iterator();
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

    private CompletionStage<Object> resolveReference(boolean tryParent, Object ref, Iterator<String> parts,
            ResolutionContext resolutionContext) {
        return resolve(new EvalContextImpl(tryParent, ref, parts.next(), resolutionContext), resolvers.iterator())
                .thenCompose(r -> {
                    if (parts.hasNext()) {
                        return resolveReference(false, r, parts, resolutionContext);
                    } else {
                        return CompletableFuture.completedFuture(r);
                    }
                });
    }

    private CompletionStage<Object> resolve(EvalContextImpl evalContext, Iterator<ValueResolver> resolvers) {
        if (!resolvers.hasNext()) {
            ResolutionContext parent = evalContext.resolutionContext.getParent();
            if (evalContext.tryParent && parent != null) {
                // Continue with parent context
                return resolve(
                        new EvalContextImpl(false, parent.getData(), evalContext.name, parent),
                        this.resolvers.iterator());
            }
            LOGGER.tracef("Unable to resolve %s", evalContext);
            return Results.NOT_FOUND;
        }
        ValueResolver resolver = resolvers.next();
        if (resolver.appliesTo(evalContext)) {
            return resolver.resolve(evalContext).thenCompose(r -> {
                if (Result.NOT_FOUND.equals(r)) {
                    return resolve(evalContext, resolvers);
                } else {
                    return CompletableFuture.completedFuture(r);
                }
            });
        } else {
            // Try next resolver
            return resolve(evalContext, resolvers);
        }
    }

    static class EvalContextImpl implements EvalContext {

        final boolean tryParent;
        final Object base;
        final String name;
        final List<String> params;
        final ResolutionContext resolutionContext;

        public EvalContextImpl(boolean tryParent, Object base, String name, ResolutionContext resolutionContext) {
            this.tryParent = tryParent;
            this.base = base;
            this.resolutionContext = resolutionContext;
            if (Expressions.isVirtualMethod(name)) {
                this.params = Expressions.parseVirtualMethodParams(name);
                this.name = Expressions.parseVirtualMethodName(name);
            } else {
                this.params = Collections.emptyList();
                this.name = name;
            }
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
        public List<String> getParams() {
            return params;
        }

        @Override
        public CompletionStage<Object> evaluate(String value) {
            return evaluate(Expression.from(value));
        }

        @Override
        public CompletionStage<Object> evaluate(Expression expression) {
            return resolutionContext.evaluate(expression);
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
