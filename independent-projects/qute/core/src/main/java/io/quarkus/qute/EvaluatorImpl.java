package io.quarkus.qute;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletionStage;

import org.jboss.logging.Logger;

import io.quarkus.qute.Expression.Part;
import io.quarkus.qute.ExpressionImpl.PartImpl;
import io.quarkus.qute.Results.NotFound;

class EvaluatorImpl implements Evaluator {

    private static final Logger LOG = Logger.getLogger(EvaluatorImpl.class);

    private final List<ValueResolver> resolvers;
    private final Map<String, NamespaceResolver[]> namespaceResolvers;
    private final boolean strictRendering;
    private final ErrorInitializer initializer;

    EvaluatorImpl(List<ValueResolver> valueResolvers, List<NamespaceResolver> namespaceResolvers, boolean strictRendering,
            ErrorInitializer errorInitializer) {
        this.resolvers = valueResolvers;
        Map<String, NamespaceResolver[]> namespaceResolversMap = new HashMap<>();
        for (NamespaceResolver namespaceResolver : namespaceResolvers) {
            NamespaceResolver[] matching = namespaceResolversMap.get(namespaceResolver.getNamespace());
            if (matching == null) {
                matching = new NamespaceResolver[] { namespaceResolver };
            } else {
                int newLength = matching.length + 1;
                matching = Arrays.copyOf(matching, newLength);
                matching[newLength - 1] = namespaceResolver;
            }
            namespaceResolversMap.put(namespaceResolver.getNamespace(), matching);
        }
        for (Entry<String, NamespaceResolver[]> entry : namespaceResolversMap.entrySet()) {
            NamespaceResolver[] matching = entry.getValue();
            if (matching.length > 1) {
                // Sort by priority - higher priority wins
                Arrays.sort(matching, Comparator.comparingInt(WithPriority::getPriority).reversed());
            }
        }
        this.namespaceResolvers = namespaceResolversMap;
        this.strictRendering = strictRendering;
        this.initializer = errorInitializer;
    }

    @Override
    public CompletionStage<Object> evaluate(Expression expression, ResolutionContext resolutionContext) {
        if (expression.isLiteral()) {
            return expression.asLiteral();
        } else if (expression.hasNamespace()) {
            NamespaceResolver[] matching = namespaceResolvers.get(expression.getNamespace());
            if (matching == null) {
                return CompletedStage.failure(
                        initializer.error("No namespace resolver found for [{namespace}] in expression \\{{expression}\\}")
                                .code(Code.NAMESPACE_RESOLVER_NOT_FOUND)
                                .argument("namespace", expression.getNamespace())
                                .argument("expression", expression.toOriginalString())
                                .origin(expression.getOrigin())
                                .build());
            }
            List<Part> parts = expression.getParts();
            Part part = parts.get(0);
            EvalContext context = part.isVirtualMethod()
                    ? new NamespaceMethodEvalContextImpl(resolutionContext, part, part.asVirtualMethod().getParameters())
                    : new NamespaceEvalContextImpl(resolutionContext, part);
            if (matching.length == 1) {
                // Very often a single matching resolver will be found
                return matching[0].resolve(context).thenCompose(r -> (parts.size() > 1)
                        ? resolveReference(false, r, parts, resolutionContext, expression, 1)
                        : CompletionStageSupport.toCompletionStage(r));
            } else {
                // Multiple namespace resolvers match
                return resolveNamespace(context, resolutionContext, parts, matching, 0, expression);
            }
        } else {
            return resolveReference(true, resolutionContext.getData(), expression.getParts(), resolutionContext, expression,
                    0);
        }
    }

    @Override
    public boolean strictRendering() {
        return strictRendering;
    }

    private CompletionStage<Object> resolveNamespace(EvalContext context, ResolutionContext resolutionContext,
            List<Part> parts, NamespaceResolver[] resolvers, int resolverIndex, Expression expression) {
        // Use the next matching namespace resolver
        NamespaceResolver resolver = resolvers[resolverIndex];
        return resolver.resolve(context).thenCompose(r -> {
            if (Results.isNotFound(r)) {
                // Result not found
                int nextIdx = resolverIndex + 1;
                if (nextIdx < resolvers.length) {
                    // Try the next matching resolver
                    return resolveNamespace(context, resolutionContext, parts, resolvers, nextIdx, expression);
                } else {
                    // No other matching namespace resolver exist
                    if (parts.size() > 1) {
                        // Continue to the next part of the expression
                        return resolveReference(false, r, parts, resolutionContext, expression, 1);
                    } else if (strictRendering) {
                        throw propertyNotFound(r, expression);
                    }
                    return Results.notFound(context);
                }
            } else if (parts.size() > 1) {
                return resolveReference(false, r, parts, resolutionContext, expression, 1);
            } else {
                return CompletionStageSupport.toCompletionStage(r);
            }
        });
    }

    private CompletionStage<Object> resolveReference(boolean tryParent, Object ref, List<Part> parts,
            ResolutionContext resolutionContext, final Expression expression, int partIndex) {
        Part part = parts.get(partIndex);
        EvalContextImpl evalContext = tryParent ? new EvalContextImpl(ref, resolutionContext, part)
                : new TerminalEvalContextImpl(ref, resolutionContext, part);
        if (partIndex + 1 >= parts.size()) {
            // The last part - no need to compose
            return resolve(evalContext, null, true, expression, true, partIndex);
        } else {
            // Next part - no need to try the parent context/outer scope
            return resolve(evalContext, null, true, expression, false, partIndex)
                    .thenCompose(r -> resolveReference(false, r, parts, resolutionContext, expression, partIndex + 1));
        }
    }

    private CompletionStage<Object> resolve(EvalContextImpl evalContext, Iterator<ValueResolver> resolvers,
            boolean tryCachedResolver, final Expression expression, boolean isLastPart, int partIndex) {

        if (tryCachedResolver) {
            // Try the cached resolver first
            ValueResolver cached = evalContext.getCachedResolver();
            if (cached != null && cached.appliesTo(evalContext)) {
                return cached.resolve(evalContext).thenCompose(r -> {
                    if (Results.isNotFound(r)) {
                        return resolve(evalContext, null, false, expression, isLastPart, partIndex);
                    } else {
                        return CompletionStageSupport.toCompletionStage(r);
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
            if (parent != null && evalContext.tryParent()) {
                // Continue with parent context
                return resolve(
                        new EvalContextImpl(parent.getData(), parent,
                                evalContext.part),
                        null, false, expression, isLastPart, partIndex);
            }
            LOG.tracef("Unable to resolve %s", evalContext);
            Object notFound;
            if (Results.isNotFound(evalContext.getBase())) {
                // If the base is "not found" then just return it
                notFound = evalContext.getBase();
            } else {
                // If the next part matches the ValueResolvers.orResolver() we can just use the empty NotFound constant
                // and avoid unnecessary allocations
                // This optimization should be ok in 99% of cases, for the rest an incomplete NotFound is an acceptable loss
                Part nextPart = isLastPart ? null : expression.getParts().get(partIndex + 1);
                if (nextPart != null
                        // is virtual method with a single param
                        && nextPart.isVirtualMethod()
                        && nextPart.asVirtualMethod().getParameters().size() == 1
                        // name has less than 3 chars
                        && nextPart.getName().length() < 3
                        // name is "?:", "or" or ":"
                        && (nextPart.getName().equals(ValueResolvers.ELVIS)
                                || nextPart.getName().equals(ValueResolvers.OR)
                                || nextPart.getName().equals(ValueResolvers.COLON))) {
                    notFound = Results.NotFound.EMPTY;
                } else {
                    notFound = Results.NotFound.from(evalContext);
                }
            }
            // If in strict mode then just throw an exception
            if (strictRendering && isLastPart) {
                throw propertyNotFound(notFound, expression);
            }
            return CompletedStage.of(notFound);
        }

        final Iterator<ValueResolver> remainingResolvers = resolvers;
        final ValueResolver foundResolver = applicableResolver;
        return applicableResolver.resolve(evalContext).thenCompose(r -> {
            if (Results.isNotFound(r)) {
                // Result not found - try the next resolver
                return resolve(evalContext, remainingResolvers, false, expression, isLastPart, partIndex);
            } else {
                // Cache the first resolver where a result is found
                evalContext.setCachedResolver(foundResolver.getCachedResolver(evalContext));
                return CompletionStageSupport.toCompletionStage(r);
            }
        });
    }

    private TemplateException propertyNotFound(Object result, Expression expression) {
        String propertyMessage;
        if (result instanceof NotFound) {
            propertyMessage = ((NotFound) result).asMessage();
        } else {
            propertyMessage = "Property not found";
        }
        return initializer.error("{prop} in expression \\{{expression}\\}")
                .code(Code.PROPERTY_NOT_FOUND)
                .origin(expression.getOrigin())
                .arguments(Map.of("prop", propertyMessage, "expression", expression.toOriginalString()))
                .build();
    }

    enum Code implements ErrorCode {

        PROPERTY_NOT_FOUND,

        NAMESPACE_RESOLVER_NOT_FOUND,

        ;

        @Override
        public String getName() {
            return "EVALUATOR_" + name();
        }

    }

    static class NamespaceEvalContextImpl implements EvalContext {

        final ResolutionContext resolutionContext;
        final PartImpl part;
        final String name;

        NamespaceEvalContextImpl(ResolutionContext resolutionContext, Part part) {
            this.resolutionContext = resolutionContext;
            this.part = (PartImpl) part;
            this.name = part.getName();
        }

        @Override
        public Object getBase() {
            return null;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public List<Expression> getParams() {
            return Collections.emptyList();
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
            builder.append("NamespaceEvalContextImpl [name=")
                    .append(name).append("]");
            return builder.toString();
        }

    }

    static class NamespaceMethodEvalContextImpl extends NamespaceEvalContextImpl {

        final List<Expression> params;

        NamespaceMethodEvalContextImpl(ResolutionContext resolutionContext, Part part, List<Expression> params) {
            super(resolutionContext, part);
            this.params = params;
        }

        @Override
        public List<Expression> getParams() {
            return params;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("NamespaceMethodEvalContextImpl [name=")
                    .append(name).append(", params=").append(params).append("]");
            return builder.toString();
        }

    }

    static class TerminalEvalContextImpl extends EvalContextImpl {

        TerminalEvalContextImpl(Object base, ResolutionContext resolutionContext, Part part) {
            super(base, resolutionContext, part);
        }

        @Override
        boolean tryParent() {
            return false;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("TerminalEvalContextImpl [base=").append(base).append(", name=")
                    .append(name).append(", params=").append(params).append("]");
            return builder.toString();
        }

    }

    static class EvalContextImpl implements EvalContext {

        final Object base;
        final ResolutionContext resolutionContext;
        final PartImpl part;
        final List<Expression> params;
        final String name;

        EvalContextImpl(Object base, ResolutionContext resolutionContext, Part part) {
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
            // Non-atomic write is ok here
            part.cachedResolver = valueResolver;
        }

        boolean tryParent() {
            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("EvalContextImpl [base=").append(base).append(", name=")
                    .append(name).append(", params=").append(params).append("]");
            return builder.toString();
        }

    }

}
