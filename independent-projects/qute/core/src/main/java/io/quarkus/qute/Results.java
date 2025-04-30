package io.quarkus.qute;

import static java.util.function.Predicate.not;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class Results {

    public static final CompletedStage<Object> FALSE = CompletedStage.of(false);
    public static final CompletedStage<Object> TRUE = CompletedStage.of(true);
    public static final CompletedStage<Object> NULL = CompletedStage.NULL;

    private Results() {
    }

    /**
     *
     * @param result
     * @return {@code true} if the value represents a "not found" result
     */
    public static boolean isNotFound(Object result) {
        return result instanceof NotFound;
    }

    public static CompletionStage<Object> notFound(EvalContext evalContext) {
        return CompletedStage.of(NotFound.from(evalContext));
    }

    public static CompletionStage<Object> notFound(String name) {
        return CompletedStage.of(NotFound.from(name));
    }

    public static CompletionStage<Object> notFound() {
        return CompletedStage.of(NotFound.EMPTY);
    }

    static CompletionStage<ResultNode> resolveAndProcess(List<TemplateNode> nodes, ResolutionContext context) {
        int nodesCount = nodes.size();
        if (nodesCount == 1) {
            // Single node in the block
            return resolveWith(nodes.get(0), context);
        }
        @SuppressWarnings("unchecked")
        Supplier<ResultNode>[] allResults = new Supplier[nodesCount];
        List<CompletableFuture<ResultNode>> asyncResults = null;
        int idx = 0;
        for (TemplateNode templateNode : nodes) {
            final CompletionStage<ResultNode> result = resolveWith(templateNode, context);
            if (result instanceof CompletedStage) {
                // No async computation needed
                allResults[idx++] = (CompletedStage<ResultNode>) result;
            } else {
                CompletableFuture<ResultNode> fu = result.toCompletableFuture();
                if (asyncResults == null) {
                    asyncResults = new ArrayList<>();
                }
                asyncResults.add(fu);
                allResults[idx++] = Futures.toSupplier(fu);
            }
        }
        return toCompletionStage(allResults, asyncResults);
    }

    private static CompletionStage<ResultNode> toCompletionStage(Supplier<ResultNode>[] allResults,
            List<CompletableFuture<ResultNode>> asyncResults) {
        if (asyncResults == null) {
            // No async results present
            return CompletedStage.of(new MultiResultNode(allResults));
        } else {
            CompletableFuture<ResultNode> ret = new CompletableFuture<ResultNode>();
            CompletionStage<?> cs;
            if (asyncResults.size() == 1) {
                cs = asyncResults.get(0);
            } else {
                cs = CompletableFuture
                        .allOf(asyncResults.toArray(new CompletableFuture[0]));
            }
            cs.whenComplete((v, t) -> {
                if (t != null) {
                    ret.completeExceptionally(t);
                } else {
                    ret.complete(new MultiResultNode(allResults));
                }
            });
            return ret;
        }
    }

    /**
     * This method is trying to speed-up the resolve method which could become a virtual dispatch, harming
     * the performance of trivial implementations like TextNode::resolve, which is as simple as a field access.
     */
    private static CompletionStage<ResultNode> resolveWith(TemplateNode templateNode, ResolutionContext context) {
        if (templateNode instanceof TextNode textNode) {
            return textNode.resolve(context);
        }
        if (templateNode instanceof ExpressionNode expressionNode) {
            return expressionNode.resolve(context);
        }
        if (templateNode instanceof SectionNode sectionNode) {
            return sectionNode.resolve(context);
        }
        if (templateNode instanceof ParameterDeclarationNode paramNode) {
            return paramNode.resolve(context);
        }
        return templateNode.resolve(context);
    }

    static CompletionStage<ResultNode> process(List<CompletionStage<ResultNode>> results) {
        // Collect async results first
        @SuppressWarnings("unchecked")
        Supplier<ResultNode>[] allResults = new Supplier[results.size()];
        List<CompletableFuture<ResultNode>> asyncResults = null;
        int idx = 0;
        for (CompletionStage<ResultNode> result : results) {
            if (result instanceof CompletedStage) {
                // No async computation needed
                allResults[idx++] = (CompletedStage<ResultNode>) result;
            } else {
                CompletableFuture<ResultNode> fu = result.toCompletableFuture();
                if (asyncResults == null) {
                    asyncResults = new ArrayList<>();
                }
                asyncResults.add(fu);
                allResults[idx++] = Futures.toSupplier(fu);
            }
        }
        return toCompletionStage(allResults, asyncResults);
    }

    /**
     * Represents a "result not found" value.
     */
    public static final class NotFound {

        public static final NotFound EMPTY = new NotFound(null, null);

        public static NotFound from(EvalContext context) {
            return new NotFound(Objects.requireNonNull(context), null);
        }

        public static NotFound from(String name) {
            return new NotFound(null, Objects.requireNonNull(name));
        }

        private final Optional<String> name;
        private final EvalContext evalContext;

        private NotFound(EvalContext evalContext, String name) {
            this.name = Optional.ofNullable(name);
            this.evalContext = evalContext;
        }

        /**
         *
         * @return the base object or empty
         */
        public Optional<Object> getBase() {
            return evalContext != null ? Optional.ofNullable(evalContext.getBase()) : Optional.empty();
        }

        /**
         *
         * @return the name of the virtual property/function
         */
        public Optional<String> getName() {
            return evalContext != null ? Optional.of(evalContext.getName()) : name;
        }

        /**
         * @return the list of parameters, is never {@code null}
         */
        public List<Expression> getParams() {
            return evalContext != null ? evalContext.getParams() : Collections.emptyList();
        }

        public String asMessage() {
            String name = getName().orElse(null);
            if (name != null) {
                Object base = getBase().orElse(null);
                List<Expression> params = getParams();
                StringBuilder builder = new StringBuilder();
                if (base instanceof Map || base instanceof Mapper) {
                    builder.append("Key ")
                            .append("\"")
                            .append(name)
                            .append("\" not found in the");
                    if (isDataMap(base)) {
                        // Key "foo" not found in the template data map with keys []
                        builder.append(" template data map with keys ");
                        if (base instanceof Map) {
                            builder.append(((Map<?, ?>) base).keySet().stream()
                                    .filter(not(TemplateInstanceBase.DATA_MAP_KEY::equals)).collect(Collectors.toList()));
                        } else if (base instanceof Mapper) {
                            builder.append(((Mapper) base).mappedKeys().stream()
                                    .filter(not(TemplateInstanceBase.DATA_MAP_KEY::equals)).collect(Collectors.toList()));
                        }
                    } else {
                        // Key "foo" not found in the map with keys []
                        builder.append(" map with keys ");
                        if (base instanceof Map) {
                            builder.append(((Map<?, ?>) base).keySet());
                        } else if (base instanceof Mapper) {
                            builder.append(((Mapper) base).mappedKeys());
                        }
                    }
                } else if (!params.isEmpty()) {
                    // Method "getDiscount(value)" not found on the base object "org.acme.Item"
                    builder.append("Method ")
                            .append("\"")
                            .append(name)
                            .append("(")
                            .append(params.stream().map(Expression::toOriginalString).collect(Collectors.joining(",")))
                            .append(")")
                            .append("\" not found")
                            .append(" on the base object \"").append(base == null ? "null" : base.getClass().getName())
                            .append("\"");
                } else {
                    // Property "foo" not found on the base object "org.acme.Bar"
                    builder.append("Property ")
                            .append("\"")
                            .append(name)
                            .append("\" not found")
                            .append(" on the base object \"").append(base == null ? "null" : base.getClass().getName())
                            .append("\"");
                }
                return builder.toString();
            } else {
                return "NOT_FOUND";
            }
        }

        private boolean isDataMap(Object base) {
            if (base instanceof Map) {
                return ((Map<?, ?>) base).containsKey(TemplateInstanceBase.DATA_MAP_KEY);
            } else if (base instanceof Mapper) {
                return ((Mapper) base).get(TemplateInstanceBase.DATA_MAP_KEY) != null;
            }
            return false;
        }

        @Override
        public String toString() {
            return "NOT_FOUND";
        }
    }

}
