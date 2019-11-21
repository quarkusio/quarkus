package io.quarkus.qute;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/*
 * Namespace resolver.
 */
public interface NamespaceResolver extends Resolver {

    static Builder builder(String namespace) {
        return new Builder(namespace);
    }

    /**
     * 
     * @return the namespace
     * @see Expression#namespace
     */
    String getNamespace();

    /**
     *
     */
    class Builder {

        private final String namespace;
        private Function<EvalContext, CompletionStage<Object>> resolve;

        Builder(String namespace) {
            this.namespace = namespace;
        }

        public Builder resolve(Function<EvalContext, Object> func) {
            this.resolve = ctx -> CompletableFuture.completedFuture(func.apply(ctx));
            return this;
        }

        public Builder resolveAsync(Function<EvalContext, CompletionStage<Object>> func) {
            this.resolve = func;
            return this;
        }

        public NamespaceResolver build() {
            Objects.requireNonNull(resolve);
            return new NamespaceResolver() {

                @Override
                public CompletionStage<Object> resolve(EvalContext context) {
                    return resolve.apply(context);
                }

                @Override
                public String getNamespace() {
                    return namespace;
                }
            };
        }

    }

}
