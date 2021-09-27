package io.quarkus.arc.processor;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;

/**
 * Allows a build-time extension to override the annotations that exist on bean classes.
 * <p>
 * The container should use {@link AnnotationStore} to obtain annotations of any {@link org.jboss.jandex.ClassInfo},
 * {@link org.jboss.jandex.FieldInfo} and {@link org.jboss.jandex.MethodInfo}.
 *
 * @see Builder
 */
public interface AnnotationsTransformer extends BuildExtension {

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
     *
     * @param transformationContext
     */
    void transform(TransformationContext transformationContext);

    /**
     * 
     * @return a new builder instance
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * A transformation context.
     */
    interface TransformationContext extends BuildContext {

        AnnotationTarget getTarget();

        /**
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
     * A convenient builder.
     */
    static final class Builder {

        private int priority;
        private Predicate<Kind> appliesTo;
        private Predicate<TransformationContext> predicate;

        private Builder() {
            this.priority = DEFAULT_PRIORITY;
        }

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

        /**
         * 
         * @param priority
         * @return self
         */
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * {@link TransformationContext#getAnnotations()} must contain ALL of the given annotations.
         * 
         * @param annotationNames
         * @return self
         */
        public Builder whenContainsAll(List<DotName> annotationNames) {
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
        public Builder whenContainsAll(DotName... annotationNames) {
            return whenContainsAll(List.of(annotationNames));
        }

        /**
         * {@link TransformationContext#getAnnotations()} must contain ALL of the given annotations.
         * 
         * @param annotationNames
         * @return self
         */
        @SafeVarargs
        public final Builder whenContainsAll(Class<? extends Annotation>... annotationNames) {
            return whenContainsAll(
                    Arrays.stream(annotationNames).map(a -> DotName.createSimple(a.getName())).collect(Collectors.toList()));
        }

        /**
         * {@link TransformationContext#getAnnotations()} must contain ANY of the given annotations.
         * 
         * @param annotationNames
         * @return self
         */
        public Builder whenContainsAny(List<DotName> annotationNames) {
            return when(context -> Annotations.containsAny(context.getAnnotations(), annotationNames));
        }

        /**
         * {@link TransformationContext#getAnnotations()} must contain ANY of the given annotations.
         * 
         * @param annotationNames
         * @return self
         */
        public Builder whenContainsAny(DotName... annotationNames) {
            return whenContainsAny(List.of(annotationNames));
        }

        /**
         * {@link TransformationContext#getAnnotations()} must contain ANY of the given annotations.
         * 
         * @param annotationNames
         * @return self
         */
        @SafeVarargs
        public final Builder whenContainsAny(Class<? extends Annotation>... annotationNames) {
            return whenContainsAny(
                    Arrays.stream(annotationNames).map(a -> DotName.createSimple(a.getName())).collect(Collectors.toList()));
        }

        /**
         * {@link TransformationContext#getAnnotations()} must NOT contain any of the given annotations.
         * 
         * @param annotationNames
         * @return self
         */
        public Builder whenContainsNone(List<DotName> annotationNames) {
            return when(context -> !Annotations.containsAny(context.getAnnotations(), annotationNames));
        }

        /**
         * {@link TransformationContext#getAnnotations()} must NOT contain any of the given annotations.
         * 
         * @param annotationNames
         * @return self
         */
        public Builder whenContainsNone(DotName... annotationNames) {
            return whenContainsNone(List.of(annotationNames));
        }

        /**
         * {@link TransformationContext#getAnnotations()} must NOT contain any of the given annotations.
         * 
         * @param annotationNames
         * @return self
         */
        @SafeVarargs
        public final Builder whenContainsNone(Class<? extends Annotation>... annotationNames) {
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
        public Builder when(Predicate<TransformationContext> when) {
            if (predicate == null) {
                predicate = when;
            } else {
                predicate = predicate.and(when);
            }
            return this;
        }

        /**
         * The given transformation logic is only performed if all conditions added via {@link #when(Predicate)} are met.
         * 
         * @param consumer
         * @return a new annotation transformer
         */
        public AnnotationsTransformer transform(Consumer<TransformationContext> consumer) {
            Predicate<Kind> appliesTo = this.appliesTo;
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
                    return appliesTo != null ? appliesTo.test(kind) : true;
                }

                @Override
                public void transform(TransformationContext context) {
                    if (predicate == null || predicate.test(context)) {
                        transform.accept(context);
                    }
                }

            };
        }

    }

}
