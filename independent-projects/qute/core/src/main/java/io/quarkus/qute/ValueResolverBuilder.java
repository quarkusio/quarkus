package io.quarkus.qute;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Builder for {@link ValueResolver}.
 */
public final class ValueResolverBuilder {

    private int priority;
    private Predicate<EvalContext> appliesTo;
    private Function<EvalContext, CompletionStage<Object>> resolve;

    ValueResolverBuilder() {
        priority = ValueResolver.DEFAULT_PRIORITY;
    }

    public ValueResolverBuilder priority(int value) {
        this.priority = value;
        return this;
    }

    /**
     * And applies to a part of an expression where the base class is assignable to the specified class.
     * <p>
     * The {@link ValueResolver#appliesTo(EvalContext)} logic defined earlier is replaced with a composite predicate.
     * 
     * @param name
     * @return self
     */
    public ValueResolverBuilder applyToBaseClass(Class<?> baseClass) {
        Predicate<EvalContext> p = new Predicate<EvalContext>() {

            @Override
            public boolean test(EvalContext ec) {
                return ValueResolver.matchClass(ec, baseClass);
            }
        };
        if (appliesTo != null) {
            appliesTo = appliesTo.and(p);
        } else {
            appliesTo = p;
        }
        return this;
    }

    /**
     * And applies to a part of an expression where the name is equal to the specified value.
     * <p>
     * The {@link ValueResolver#appliesTo(EvalContext)} logic defined earlier is replaced with a composite predicate.
     * 
     * @param name
     * @return self
     */
    public ValueResolverBuilder applyToName(String name) {
        Predicate<EvalContext> p = new Predicate<EvalContext>() {

            @Override
            public boolean test(EvalContext ec) {
                return ec.getName().equals(name);
            }
        };
        if (appliesTo != null) {
            appliesTo = appliesTo.and(p);
        } else {
            appliesTo = p;
        }
        return this;
    }

    /**
     * And applies to a part of an expression where the number of parameters is equal to zero.
     * <p>
     * The {@link ValueResolver#appliesTo(EvalContext)} logic defined earlier is replaced with a composite predicate.
     * 
     * @return self
     */
    public ValueResolverBuilder applyToNoParameters() {
        Predicate<EvalContext> p = new Predicate<EvalContext>() {

            @Override
            public boolean test(EvalContext ec) {
                return ec.getParams().size() == 0;
            }
        };
        if (appliesTo != null) {
            appliesTo = appliesTo.and(p);
        } else {
            appliesTo = p;
        }
        return this;
    }

    /**
     * And applies to a part of an expression where the number of parameters is equal to the specified size.
     * <p>
     * The {@link ValueResolver#appliesTo(EvalContext)} logic defined earlier is replaced with a composite predicate.
     * 
     * @param size
     * @return self
     */
    public ValueResolverBuilder applyToParameters(int size) {
        Predicate<EvalContext> p = new Predicate<EvalContext>() {

            @Override
            public boolean test(EvalContext ec) {
                return ec.getParams().size() == size;
            }
        };
        if (appliesTo != null) {
            appliesTo = appliesTo.and(p);
        } else {
            appliesTo = p;
        }
        return this;
    }

    /**
     * The {@link ValueResolver#appliesTo(EvalContext)} logic defined earlier is replaced with the specified predicate.
     * 
     * @param predicate
     * @return self
     */
    public ValueResolverBuilder appliesTo(Predicate<EvalContext> predicate) {
        this.appliesTo = predicate;
        return this;
    }

    public ValueResolverBuilder resolveSync(Function<EvalContext, Object> fun) {
        this.resolve = new Function<EvalContext, CompletionStage<Object>>() {
            @Override
            public CompletionStage<Object> apply(EvalContext context) {
                return CompletedStage.of(fun.apply(context));
            }
        };
        return this;
    }

    public ValueResolverBuilder resolveAsync(Function<EvalContext, CompletionStage<Object>> fun) {
        this.resolve = fun;
        return this;
    }

    public ValueResolverBuilder resolveWith(Object value) {
        return resolveAsync(ec -> CompletedStage.of(value));
    }

    public ValueResolver build() {
        return new ValueResolverImpl(priority, appliesTo, resolve);
    }

    private static final class ValueResolverImpl implements ValueResolver {

        private final int priority;
        private final Predicate<EvalContext> appliesTo;
        private final Function<EvalContext, CompletionStage<Object>> resolve;

        public ValueResolverImpl(int priority, Predicate<EvalContext> appliesTo,
                Function<EvalContext, CompletionStage<Object>> resolve) {
            this.priority = priority;
            this.appliesTo = appliesTo;
            this.resolve = resolve;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public boolean appliesTo(EvalContext context) {
            return appliesTo != null ? appliesTo.test(context) : true;
        }

        @Override
        public CompletionStage<Object> resolve(EvalContext context) {
            return resolve.apply(context);
        }

    }

}
