package io.quarkus.qute;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Namespace resolvers are used to find the current context object for an expression that starts with a namespace declaration.
 * <p>
 * For example the expression {@code data:colors} declares a namespace {@code data}.
 * 
 * @see EngineBuilder#addNamespaceResolver(NamespaceResolver)
 */
public interface NamespaceResolver extends Resolver {

    /**
     * 
     * @param namespace
     * @return a new builder instance
     */
    static Builder builder(String namespace) {
        return new Builder(namespace);
    }

    /**
     * 
     * @return the namespace
     * @see ExpressionImpl#namespace
     */
    String getNamespace();

    final class Builder {

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
