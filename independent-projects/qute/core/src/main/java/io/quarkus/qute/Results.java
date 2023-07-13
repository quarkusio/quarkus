package io.quarkus.qute;

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

    static CompletionStage<ResultNode> process(List<CompletionStage<ResultNode>> results) {
        // Collect async results first
        @SuppressWarnings("unchecked")
        Supplier<ResultNode>[] allResults = new Supplier[results.size()];
        List<CompletableFuture<ResultNode>> asyncResults = null;
        int idx = 0;
        for (CompletionStage<ResultNode> result : results) {
            if (result instanceof CompletedStage) {
                allResults[idx++] = (CompletedStage<ResultNode>) result;
                // No async computation needed
                continue;
            } else {
                CompletableFuture<ResultNode> fu = result.toCompletableFuture();
                if (asyncResults == null) {
                    asyncResults = new ArrayList<>();
                }
                asyncResults.add(fu);
                allResults[idx++] = Futures.toSupplier(fu);
            }
        }
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
                boolean isDataMap = isDataMap(base);
                // Entry "foo" not found in the data map
                // Property "foo" not found on base object "org.acme.Bar"
                // Method "getDiscount(value)" not found on base object "org.acme.Item"
                StringBuilder builder = new StringBuilder();
                if (isDataMap) {
                    builder.append("Entry ");
                } else if (params.isEmpty()) {
                    builder.append("Property ");
                } else {
                    builder.append("Method ");
                }
                builder.append("\"").append(name);
                if (!params.isEmpty()) {
                    builder.append("(");
                    builder.append(params.stream().map(Expression::toOriginalString).collect(Collectors.joining(",")));
                    builder.append(")");
                }
                builder.append("\" not found");
                if (isDataMap) {
                    builder.append(" in the data map");
                } else {
                    builder.append(" on the base object \"").append(base == null ? "null" : base.getClass().getName())
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
