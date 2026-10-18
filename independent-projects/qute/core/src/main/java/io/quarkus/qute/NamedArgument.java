package io.quarkus.qute;

import java.util.concurrent.CompletionStage;

/**
 * Used to supply named arguments for {@link FragmentNamespaceResolver}.
 */
public final class NamedArgument {

    private final String name;

    private volatile Object value;

    public NamedArgument(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public NamedArgument setValue(Object value) {
        this.value = value;
        return this;
    }

    public String getName() {
        return name;
    }

    public static final class ParamNamespaceResolver implements NamespaceResolver {

        private final String name;

        private final int priority;

        public ParamNamespaceResolver() {
            this("param", -1);
        }

        public ParamNamespaceResolver(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }

        @Override
        public CompletionStage<Object> resolve(EvalContext context) {
            return CompletedStage.of(new NamedArgument(context.getName()));
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public String getNamespace() {
            return name;
        }

    }

    public static final class SetValueResolver implements ValueResolver {

        public boolean appliesTo(EvalContext context) {
            if (context.getParams().size() != 1) {
                return false;
            }
            String name = context.getName();
            return (name.equals("=") || name.equals("set"))
                    && ValueResolvers.matchClass(context, NamedArgument.class);
        }

        @Override
        public CompletionStage<Object> resolve(EvalContext context) {
            NamedArgument argument = (NamedArgument) context.getBase();
            return context.evaluate(context.getParams().get(0)).thenApply(v -> argument.setValue(v));
        }

    }
}
