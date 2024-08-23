package io.quarkus.arc.processor;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

/**
 * Allows a build-time extension to override the annotations that exist on bean classes.
 * <p>
 * The container should use {@link AnnotationStore} to obtain annotations of any {@link org.jboss.jandex.ClassInfo},
 * {@link org.jboss.jandex.FieldInfo} and {@link org.jboss.jandex.MethodInfo}.
 *
 * @see Builder
 */
public interface AnnotationsTransformer extends AnnotationTransformation, BuildExtension {

    /**
     * By default, the transformation is applied to all kinds of targets.
     *
     * @param kind
     * @return {@code true} if the transformation applies to the specified kind, {@code false} otherwise
     */
    default boolean appliesTo(Kind kind) {
        return true;
    }

    /**
     * The transformation context is used to perform the transformation. In particular,
     * {@link TransformationContext#transform()}
     * returns a new transformation object that can be used to alter annotation metadata. Note that the transformation is not
     * applied until the {@link Transformation#done()} method is invoked.
     *
     * @param transformationContext
     */
    void transform(TransformationContext transformationContext);

    // ---
    // implementation of `AnnotationTransformation` methods

    @Override
    default int priority() {
        return getPriority();
    }

    @Override
    default boolean supports(Kind kind) {
        return appliesTo(kind);
    }

    @Override
    default void apply(AnnotationTransformation.TransformationContext context) {
        transform(new TransformationContext() {
            @Override
            public AnnotationTarget getTarget() {
                return context.declaration();
            }

            @Override
            public Collection<AnnotationInstance> getAnnotations() {
                return context.annotations();
            }

            @Override
            public Transformation transform() {
                return new Transformation(context);
            }

            @Override
            public <V> V get(Key<V> key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <V> V put(Key<V> key, V value) {
                throw new UnsupportedOperationException();
            }
        });
    }

    @Override
    default boolean requiresCompatibleMode() {
        return true;
    }

    // ---

    /**
     *
     * @return a new builder instance
     * @see #appliedToMethod()
     * @see #appliedToField()
     * @see #appliedToClass()
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     *
     * @return a new builder to transform methods
     */
    static MethodTransformerBuilder appliedToMethod() {
        return new MethodTransformerBuilder();
    }

    /**
     *
     * @return a new builder to transform fields
     */
    static FieldTransformerBuilder appliedToField() {
        return new FieldTransformerBuilder();
    }

    /**
     *
     * @return a new builder to transform class
     */
    static ClassTransformerBuilder appliedToClass() {
        return new ClassTransformerBuilder();
    }

    /**
     * A transformation context.
     */
    interface TransformationContext extends BuildContext {

        /**
         * Returns the annotated class, method or field.
         *
         * @return the annotation target
         */
        AnnotationTarget getTarget();

        /**
         * Returns the current set of annotations.
         * <p>
         * The initial set of annotations instances corresponds to {@link org.jboss.jandex.ClassInfo#classAnnotations()},
         * {@link org.jboss.jandex.FieldInfo#annotations()} and {@link org.jboss.jandex.MethodInfo#annotations()} respectively.
         *
         * @return the annotation instances
         */
        Collection<AnnotationInstance> getAnnotations();

        /**
         * The transformation is not applied until the {@link Transformation#done()} method is invoked.
         *
         * @return a new transformation
         */
        Transformation transform();

        default boolean isClass() {
            return getTarget().kind() == Kind.CLASS;
        }

        default boolean isField() {
            return getTarget().kind() == Kind.FIELD;
        }

        default boolean isMethod() {
            return getTarget().kind() == Kind.METHOD;
        }

    }

    /**
     * A common {@link AnnotationsTransformer} builder.
     */
    public final static class Builder extends AbstractBuilder<Builder> {

        protected Predicate<Kind> appliesTo;

        /**
         *
         * @param appliesToKind
         * @return self
         * @see AnnotationsTransformer#appliesTo(Kind)
         */
        public Builder appliesTo(Kind appliesToKind) {
            return appliesTo(kind -> kind == appliesToKind);
        }

        /**
         *
         * @param appliesTo
         * @return self
         * @see AnnotationsTransformer#appliesTo(Kind)
         */
        public Builder appliesTo(Predicate<Kind> appliesTo) {
            this.appliesTo = appliesTo;
            return this;
        }

        @Override
        public boolean test(Kind kind) {
            return appliesTo == null || appliesTo.test(kind);
        }

    }

    public final static class MethodTransformerBuilder extends AbstractBuilder<MethodTransformerBuilder> {

        /**
         * The method must meet the given condition.
         *
         * @param condition
         * @return self
         */
        public MethodTransformerBuilder whenMethod(Predicate<MethodInfo> condition) {
            return when(wrap(condition, MethodTransformerBuilder::extract));
        }

        @Override
        public boolean test(Kind kind) {
            return kind == Kind.METHOD;
        }

        private static MethodInfo extract(TransformationContext ctx) {
            return ctx.getTarget().asMethod();
        }

    }

    public final static class FieldTransformerBuilder extends AbstractBuilder<FieldTransformerBuilder> {

        /**
         * The field must meet the given condition.
         *
         * @param condition
         * @return self
         */
        public FieldTransformerBuilder whenField(Predicate<FieldInfo> condition) {
            return when(wrap(condition, FieldTransformerBuilder::extract));
        }

        @Override
        public boolean test(Kind kind) {
            return kind == Kind.FIELD;
        }

        private static FieldInfo extract(TransformationContext ctx) {
            return ctx.getTarget().asField();
        }

    }

    public final static class ClassTransformerBuilder extends AbstractBuilder<ClassTransformerBuilder> {

        /**
         * The class must meet the given condition.
         *
         * @param condition
         * @return self
         */
        public ClassTransformerBuilder whenClass(Predicate<ClassInfo> condition) {
            return when(wrap(condition, ClassTransformerBuilder::extract));
        }

        @Override
        public boolean test(Kind kind) {
            return kind == Kind.CLASS;
        }

        private static ClassInfo extract(TransformationContext ctx) {
            return ctx.getTarget().asClass();
        }

    }

    public abstract static class AbstractBuilder<THIS extends AbstractBuilder<THIS>>
            implements Predicate<Kind> {

        protected int priority;
        protected Predicate<TransformationContext> predicate;

        private AbstractBuilder() {
            this.priority = DEFAULT_PRIORITY;
        }

        /**
         *
         * @param priority
         * @return self
         */
        public THIS priority(int priority) {
            this.priority = priority;
            return self();
        }

        /**
         * {@link TransformationContext#getAnnotations()} must contain ALL of the given annotations.
         *
         * @param annotationNames
         * @return self
         */
        public THIS whenContainsAll(List<DotName> annotationNames) {
            return when(context -> {
                for (DotName annotationName : annotationNames) {
                    if (!Annotations.contains(context.getAnnotations(), annotationName)) {
                        return false;
                    }
                }
                return true;
            });
        }

        /**
         * {@link TransformationContext#getAnnotations()} must contain ALL of the given annotations.
         *
         * @param annotationNames
         * @return self
         */
        public THIS whenContainsAll(DotName... annotationNames) {
            return whenContainsAll(List.of(annotationNames));
        }

        /**
         * {@link TransformationContext#getAnnotations()} must contain ALL of the given annotations.
         *
         * @param annotationNames
         * @return self
         */
        @SafeVarargs
        public final THIS whenContainsAll(Class<? extends Annotation>... annotationNames) {
            return whenContainsAll(
                    Arrays.stream(annotationNames).map(a -> DotName.createSimple(a.getName())).collect(Collectors.toList()));
        }

        /**
         * {@link TransformationContext#getAnnotations()} must contain ANY of the given annotations.
         *
         * @param annotationNames
         * @return self
         */
        public THIS whenContainsAny(List<DotName> annotationNames) {
            return when(context -> Annotations.containsAny(context.getAnnotations(), annotationNames));
        }

        /**
         * {@link TransformationContext#getAnnotations()} must contain ANY of the given annotations.
         *
         * @param annotationNames
         * @return self
         */
        public THIS whenContainsAny(DotName... annotationNames) {
            return whenContainsAny(List.of(annotationNames));
        }

        /**
         * {@link TransformationContext#getAnnotations()} must contain ANY of the given annotations.
         *
         * @param annotationNames
         * @return self
         */
        @SafeVarargs
        public final THIS whenContainsAny(Class<? extends Annotation>... annotationNames) {
            return whenContainsAny(
                    Arrays.stream(annotationNames).map(a -> DotName.createSimple(a.getName())).collect(Collectors.toList()));
        }

        /**
         * {@link TransformationContext#getAnnotations()} must NOT contain any of the given annotations.
         *
         * @param annotationNames
         * @return self
         */
        public THIS whenContainsNone(List<DotName> annotationNames) {
            return when(context -> !Annotations.containsAny(context.getAnnotations(), annotationNames));
        }

        /**
         * {@link TransformationContext#getAnnotations()} must NOT contain any of the given annotations.
         *
         * @param annotationNames
         * @return self
         */
        public THIS whenContainsNone(DotName... annotationNames) {
            return whenContainsNone(List.of(annotationNames));
        }

        /**
         * {@link TransformationContext#getAnnotations()} must NOT contain any of the given annotations.
         *
         * @param annotationNames
         * @return self
         */
        @SafeVarargs
        public final THIS whenContainsNone(Class<? extends Annotation>... annotationNames) {
            return whenContainsNone(
                    Arrays.stream(annotationNames).map(a -> DotName.createSimple(a.getName())).collect(Collectors.toList()));
        }

        /**
         * The transformation logic is only performed if the given predicate is evaluated to true. Multiple predicates are
         * logically-ANDed.
         *
         * @param predicate
         * @return self
         */
        public THIS when(Predicate<TransformationContext> when) {
            if (predicate == null) {
                predicate = when;
            } else {
                predicate = predicate.and(when);
            }
            return self();
        }

        /**
         * If all conditions are met then apply the transformation logic.
         * <p>
         * Unlike in {@link #transform(Consumer)} the transformation is automatically applied, i.e. a {@link Transformation}
         * is created and the {@link Transformation#done()} method is called automatically.
         *
         * @param consumer
         * @return a new annotation transformer
         */
        public AnnotationsTransformer thenTransform(Consumer<Transformation> consumer) {
            Consumer<Transformation> transform = Objects.requireNonNull(consumer);
            return transform(new Consumer<TransformationContext>() {

                @Override
                public void accept(TransformationContext context) {
                    Transformation transformation = context.transform();
                    transform.accept(transformation);
                    transformation.done();
                }
            });

        }

        /**
         * The transformation logic is performed only if all conditions are met.
         * <p>
         * This method should be used if you need to access the transformation context directly. Otherwise, the
         * {@link #thenTransform(Consumer)} is more convenient.
         *
         * @param consumer
         * @return a new annotation transformer
         * @see #thenTransform(Consumer)
         */
        public AnnotationsTransformer transform(Consumer<TransformationContext> consumer) {
            int priority = this.priority;
            Consumer<TransformationContext> transform = Objects.requireNonNull(consumer);
            Predicate<TransformationContext> predicate = this.predicate;
            return new AnnotationsTransformer() {

                @Override
                public int getPriority() {
                    return priority;
                }

                @Override
                public boolean appliesTo(Kind kind) {
                    return test(kind);
                }

                @Override
                public void transform(TransformationContext context) {
                    if (predicate == null || predicate.test(context)) {
                        transform.accept(context);
                    }
                }

            };
        }

        @SuppressWarnings("unchecked")
        protected THIS self() {
            return (THIS) this;
        }

        protected <TARGET> Predicate<TransformationContext> wrap(Predicate<TARGET> condition,
                Function<TransformationContext, TARGET> extractor) {
            return new Predicate<TransformationContext>() {

                @Override
                public boolean test(TransformationContext ctx) {
                    return condition.test(extractor.apply(ctx));
                }
            };
        }

    }

}
