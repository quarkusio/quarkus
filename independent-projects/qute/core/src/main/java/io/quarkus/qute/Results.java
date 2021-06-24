package io.quarkus.qute;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public final class Results {

    /**
     * This field will be removed at some point post Quarkus 2.1.
     * 
     * @deprecated Use {@link #notFound(EvalContext)} or {@link #notFound(String)} instead
     */
    @Deprecated
    public static final CompletionStage<Object> NOT_FOUND = CompletableFuture.completedFuture(Result.NOT_FOUND);
    public static final CompletableFuture<Object> FALSE = CompletableFuture.completedFuture(false);
    public static final CompletableFuture<Object> TRUE = CompletableFuture.completedFuture(true);
    public static final CompletableFuture<Object> NULL = CompletableFuture.completedFuture(null);

    private Results() {
    }

    /**
     * 
     * @param result
     * @return {@code true} if the value represents a "not found" result
     */
    public static boolean isNotFound(Object result) {
        return Result.NOT_FOUND == result || result instanceof NotFound;
    }

    public static CompletionStage<Object> notFound(EvalContext evalContext) {
        return CompletableFuture.completedFuture(NotFound.from(evalContext));
    }

    public static CompletionStage<Object> notFound(String name) {
        return CompletableFuture.completedFuture(NotFound.from(name));
    }

    public static CompletionStage<Object> notFound() {
        return CompletableFuture.completedFuture(NotFound.EMPTY);
    }

    /**
     * This enum will be removed at some point post Quarkus 2.1.
     * 
     * @deprecated {@link NotFound} instead.
     */
    @Deprecated
    public enum Result {

        NOT_FOUND;

        @Override
        public String toString() {
            return "NOT_FOUND";
        }
    }

    /**
     * Represents various types of "result not found" values.
     */
    public interface NotFound {

        static final NotFound EMPTY = new NotFound() {

            @Override
            public String toString() {
                return "NOT_FOUND";
            }

        };

        public static NotFound from(EvalContext context) {
            return new EvalContextNotFound(Objects.requireNonNull(context));
        }

        public static NotFound from(String name) {
            return new NameOnlyNotFound(Objects.requireNonNull(name));
        }

        /**
         * 
         * @return the base object or empty
         */
        default Optional<Object> getBase() {
            return Optional.empty();
        }

        /**
         * 
         * @return the name of the virtual property/function
         */
        default Optional<String> getName() {
            return Optional.empty();
        }

        /**
         * @return the list of parameters, is never {@code null}
         */
        default List<Expression> getParams() {
            return Collections.emptyList();
        }

        default String asMessage() {
            String name = getName().orElse(null);
            if (name != null) {
                // Property "foo" not found on base object "org.acme.Bar"
                // Method "getDiscount(value)" not found on base object "org.acme.Item"
                List<Expression> params = getParams();
                StringBuilder builder = new StringBuilder();
                if (params.isEmpty()) {
                    builder.append("Property ");
                } else {
                    builder.append("Method ");
                }
                builder.append("\"").append(name);
                if (!params.isEmpty()) {
                    builder.append("(");
                    builder.append(getParams().stream().map(Expression::toOriginalString).collect(Collectors.joining(",")));
                    builder.append(")");
                }
                builder.append("\" not found");
                Object base = getBase().orElse(null);
                if (!(base instanceof Map)
                        // Just ignore the data map
                        || !((Map<?, ?>) base).containsKey(TemplateInstanceBase.DATA_MAP_KEY)) {
                    builder.append(" on base object \"").append(base == null ? "null" : base.getClass().getName()).append("\"");
                }
                return builder.toString();
            } else {
                return "NOT_FOUND";
            }
        }

    }

    static final class NameOnlyNotFound implements NotFound {

        private final Optional<String> name;

        NameOnlyNotFound(String name) {
            this.name = Optional.of(name);
        }

        @Override
        public Optional<String> getName() {
            return name;
        }

        @Override
        public String toString() {
            return "NOT_FOUND";
        }

    }

    static final class EvalContextNotFound implements NotFound {

        private final EvalContext evalContext;

        EvalContextNotFound(EvalContext evalContext) {
            this.evalContext = evalContext;
        }

        @Override
        public Optional<Object> getBase() {
            return Optional.ofNullable(evalContext.getBase());
        }

        @Override
        public Optional<String> getName() {
            return Optional.of(evalContext.getName());
        }

        @Override
        public List<Expression> getParams() {
            return evalContext.getParams();
        }

        @Override
        public String toString() {
            return "NOT_FOUND";
        }

    }

}
