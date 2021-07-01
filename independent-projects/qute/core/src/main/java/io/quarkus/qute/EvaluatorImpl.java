package io.quarkus.qute;

import io.quarkus.qute.Expression.Part;
import io.quarkus.qute.ExpressionImpl.PartImpl;
import io.smallrye.mutiny.Uni;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletionStage;
import org.jboss.logging.Logger;

/**
 * 
 */
class EvaluatorImpl implements Evaluator {

    private static final Logger LOGGER = Logger.getLogger(EvaluatorImpl.class);

    private final List<ValueResolver> resolvers;
    private final Map<String, List<NamespaceResolver>> namespaceResolvers;

    EvaluatorImpl(List<ValueResolver> valueResolvers, List<NamespaceResolver> namespaceResolvers) {
        this.resolvers = valueResolvers;
        Map<String, List<NamespaceResolver>> namespaceResolversMap = new HashMap<>();
        for (NamespaceResolver namespaceResolver : namespaceResolvers) {
            List<NamespaceResolver> matching = namespaceResolversMap.get(namespaceResolver.getNamespace());
            if (matching == null) {
                matching = new ArrayList<>();
                namespaceResolversMap.put(namespaceResolver.getNamespace(), matching);
            }
            matching.add(namespaceResolver);
        }
        for (Entry<String, List<NamespaceResolver>> entry : namespaceResolversMap.entrySet()) {
            List<NamespaceResolver> list = entry.getValue();
            if (list.size() == 1) {
                entry.setValue(Collections.singletonList(list.get(0)));
            } else {
                // Sort by priority - higher priority wins
                list.sort(Comparator.comparingInt(WithPriority::getPriority).reversed());
            }
        }
        this.namespaceResolvers = namespaceResolversMap;
    }

    @Override
    public CompletionStage<Object> evaluate(Expression expression, ResolutionContext resolutionContext) {
        Iterator<Part> parts;
        if (expression.hasNamespace()) {
            parts = expression.getParts().iterator();
            List<NamespaceResolver> matching = namespaceResolvers.get(expression.getNamespace());
            if (matching == null) {
                String msg = "No namespace resolver found for: " + expression.getNamespace();
                LOGGER.errorf(msg);
                return CompletedStage.failure(new TemplateException(msg));
            }
            EvalContext context = new EvalContextImpl(false, null, parts.next(), resolutionContext);
            if (matching.size() == 1) {
                // Very often a single matching resolver will be found
                return matching.get(0).resolve(context).thenCompose(r -> {
                    if (parts.hasNext()) {
                        return resolveReference(false, r, parts, resolutionContext);
                    } else {
                        return toCompletionStage(r);
                    }
                });
            } else {
                // Multiple namespace resolvers match
                return resolveNamespace(context, resolutionContext, parts, matching.iterator());
            }
        } else {
            if (expression.isLiteral()) {
                return expression.asLiteral();
            } else {
                parts = expression.getParts().iterator();
                return resolveReference(true, resolutionContext.getData(), parts, resolutionContext);
            }
        }
    }

    private CompletionStage<Object> resolveNamespace(EvalContext context, ResolutionContext resolutionContext,
            Iterator<Part> parts, Iterator<NamespaceResolver> resolvers) {
        NamespaceResolver resolver = resolvers.next();
        return resolver.resolve(context).thenCompose(r -> {
            if (Results.isNotFound(r)) {
                if (resolvers.hasNext()) {
                    return resolveNamespace(context, resolutionContext, parts, resolvers);
                } else {
                    return Results.notFound(context);
                }
            } else if (parts.hasNext()) {
                return resolveReference(false, r, parts, resolutionContext);
            } else {
                return toCompletionStage(r);
            }
        });
    }

    private CompletionStage<Object> resolveReference(boolean tryParent, Object ref, Iterator<Part> parts,
            ResolutionContext resolutionContext) {
        Part part = parts.next();
        EvalContextImpl evalContext = new EvalContextImpl(tryParent, ref, part, resolutionContext);
        if (!parts.hasNext()) {
            // The last part - no need to compose
            return resolve(evalContext, null, true);
        } else {
            // Next part - no need to try the parent context/outer scope
            return resolve(evalContext, null, true)
                    .thenCompose(r -> resolveReference(false, r, parts, resolutionContext));
        }
    }

    private CompletionStage<Object> resolve(EvalContextImpl evalContext, Iterator<ValueResolver> resolvers,
            boolean tryCachedResolver) {

        if (tryCachedResolver) {
            // Try the cached resolver first
            ValueResolver cachedResolver = evalContext.getCachedResolver();
            if (cachedResolver != null && cachedResolver.appliesTo(evalContext)) {
                return cachedResolver.resolve(evalContext).thenCompose(r -> {
                    if (Results.isNotFound(r)) {
                        return resolve(evalContext, null, false);
                    } else {
                        return toCompletionStage(r);
                    }
                });
            }
        }

        if (resolvers == null) {
            // Iterate the resolvers lazily 
            resolvers = this.resolvers.iterator();
        }

        ValueResolver applicableResolver = null;
        while (applicableResolver == null && resolvers.hasNext()) {
            ValueResolver next = resolvers.next();
            if (next.appliesTo(evalContext)) {
                applicableResolver = next;
            }
        }
        if (applicableResolver == null) {
            ResolutionContext parent = evalContext.resolutionContext.getParent();
            if (evalContext.tryParent && parent != null) {
                // Continue with parent context
                return resolve(
                        new EvalContextImpl(true, parent.getData(), parent,
                                evalContext.part),
                        null, false);
            }
            LOGGER.tracef("Unable to resolve %s", evalContext);
            if (Results.isNotFound(evalContext.getBase())) {
                // If the base is "not found" then just return it
                return CompletedStage.of(evalContext.getBase());
            }
            return Results.notFound(evalContext);
        }

        final Iterator<ValueResolver> remainingResolvers = resolvers;
        final ValueResolver foundResolver = applicableResolver;
        return applicableResolver.resolve(evalContext).thenCompose(r -> {
            if (Results.isNotFound(r)) {
                // Result not found - try the next resolver
                return resolve(evalContext, remainingResolvers, false);
            } else {
                // Cache the first resolver where a result is found
                evalContext.setCachedResolver(foundResolver);
                return toCompletionStage(r);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static CompletionStage<Object> toCompletionStage(Object result) {
        if (result instanceof CompletionStage) {
            // If the result is a completion stage return it as is
            return (CompletionStage<Object>) result;
        } else if (result instanceof Uni) {
            // Subscribe to the Uni
            return ((Uni<Object>) result).subscribeAsCompletionStage();
        }
        return CompletedStage.of(result);
    }

    static class EvalContextImpl implements EvalContext {

        final boolean tryParent;
        final Object base;
        final ResolutionContext resolutionContext;
        final PartImpl part;
        final List<Expression> params;
        final String name;

        EvalContextImpl(boolean tryParent, Object base, Part part, ResolutionContext resolutionContext) {
            this(tryParent, base, resolutionContext, part);
        }

        EvalContextImpl(boolean tryParent, Object base, ResolutionContext resolutionContext, Part part) {
            this.tryParent = tryParent;
            this.base = base;
            this.resolutionContext = resolutionContext;
            this.part = (PartImpl) part;
            this.name = part.getName();
            this.params = part.isVirtualMethod() ? part.asVirtualMethod().getParameters() : Collections.emptyList();
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

        ValueResolver getCachedResolver() {
            return part.cachedResolver;
        }

        void setCachedResolver(ValueResolver valueResolver) {
            part.cachedResolver = valueResolver;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("EvalContextImpl [tryParent=").append(tryParent).append(", base=").append(base).append(", name=")
                    .append(getName()).append(", params=").append(getParams()).append("]");
            return builder.toString();
        }

    }

}
