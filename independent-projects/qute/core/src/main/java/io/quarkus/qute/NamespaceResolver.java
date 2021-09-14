package io.quarkus.qute;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Namespace resolvers are used to find the current context object for an expression that starts with a namespace declaration.
 * <p>
 * For example the expression {@code data:colors} declares a namespace {@code data}.
 * 
 * @see EngineBuilder#addNamespaceResolver(NamespaceResolver)
 */
public interface NamespaceResolver extends Resolver, WithPriority {

    /**
     * 
     * @param namespace
     * @return a new builder instance
     */
    static Builder builder(String namespace) {
        return new Builder(namespace);
    }

    /**
     * A valid namespace consists of alphanumeric characters and underscores.
     * 
     * @return the namespace
     * @see Expression#getNamespace()
     */
    String getNamespace();

    /**
     * A convenient builder.
     */
    final class Builder {

        private final String namespace;
        private Function<EvalContext, CompletionStage<Object>> resolve;
        private int priority = WithPriority.DEFAULT_PRIORITY;

        Builder(String namespace) {
            this.namespace = Namespaces.requireValid(namespace);
        }

        public Builder resolve(Function<EvalContext, Object> func) {
            this.resolve = ctx -> CompletedStage.of(func.apply(ctx));
            return this;
        }

        public Builder resolveAsync(Function<EvalContext, CompletionStage<Object>> func) {
            this.resolve = func;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public NamespaceResolver build() {
            Objects.requireNonNull(resolve);
            return new NamespaceResolverImpl(namespace, priority, resolve);
        }

    }

    final class NamespaceResolverImpl implements NamespaceResolver {

        private final String namespace;
        private final int priority;
        private final Function<EvalContext, CompletionStage<Object>> resolve;

        public NamespaceResolverImpl(String namespace, int priority, Function<EvalContext, CompletionStage<Object>> resolve) {
            this.namespace = namespace;
            this.priority = priority;
            this.resolve = resolve;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public CompletionStage<Object> resolve(EvalContext context) {
            return resolve.apply(context);
        }

        @Override
        public String getNamespace() {
            return namespace;
        }

    }

}
