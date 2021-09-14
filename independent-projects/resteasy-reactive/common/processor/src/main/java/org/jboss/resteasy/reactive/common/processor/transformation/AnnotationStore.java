package org.jboss.resteasy.reactive.common.processor.transformation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

/**
 * Applies {@link AnnotationsTransformer}s and caches the results of transformations.
 *
 * @author Martin Kouba
 * @see AnnotationsTransformer
 */
public final class AnnotationStore {

    private final ConcurrentMap<AnnotationTargetKey, Collection<AnnotationInstance>> transformed;

    private final EnumMap<Kind, List<AnnotationsTransformer>> transformersMap;

    public AnnotationStore(Collection<AnnotationsTransformer> transformers) {
        if (transformers == null || transformers.isEmpty()) {
            this.transformed = null;
            this.transformersMap = null;
        } else {
            this.transformed = new ConcurrentHashMap<>();
            this.transformersMap = new EnumMap<>(Kind.class);
            this.transformersMap.put(Kind.CLASS, initTransformers(Kind.CLASS, transformers));
            this.transformersMap.put(Kind.METHOD, initTransformers(Kind.METHOD, transformers));
            this.transformersMap.put(Kind.FIELD, initTransformers(Kind.FIELD, transformers));
        }
    }

    /**
     * All {@link AnnotationsTransformer}s are applied and the result is cached.
     * 
     * @param target
     * @return the annotation instance for the given target
     */
    public Collection<AnnotationInstance> getAnnotations(AnnotationTarget target) {
        if (transformed != null) {
            return transformed.computeIfAbsent(new AnnotationTargetKey(target), this::transform);
        }
        return getOriginalAnnotations(target);
    }

    /**
     * 
     * @param target
     * @param name
     * @return the annotation instance if present, {@code null} otherwise
     * @see #getAnnotations(AnnotationTarget)
     */
    public AnnotationInstance getAnnotation(AnnotationTarget target, DotName name) {
        return Annotations.find(getAnnotations(target), name);
    }

    /**
     * 
     * @param target
     * @param name
     * @return {@code true} if the specified target contains the specified annotation, @{code false} otherwise
     * @see #getAnnotations(AnnotationTarget)
     */
    public boolean hasAnnotation(AnnotationTarget target, DotName name) {
        return Annotations.contains(getAnnotations(target), name);
    }

    /**
     * 
     * @param target
     * @param names
     * @return {@code true} if the specified target contains any of the specified annotations, @{code false} otherwise
     * @see #getAnnotations(AnnotationTarget)
     */
    public boolean hasAnyAnnotation(AnnotationTarget target, Iterable<DotName> names) {
        return Annotations.containsAny(getAnnotations(target), names);
    }

    private Collection<AnnotationInstance> transform(AnnotationTargetKey key) {
        AnnotationTarget target = key.target;
        Collection<AnnotationInstance> annotations = getOriginalAnnotations(target);
        List<AnnotationsTransformer> transformers = transformersMap.get(target.kind());
        if (transformers.isEmpty()) {
            return annotations;
        }
        TransformationContextImpl transformationContext = new TransformationContextImpl(target, annotations);
        for (AnnotationsTransformer transformer : transformers) {
            transformer.transform(transformationContext);
        }
        return transformationContext.getAnnotations();
    }

    private Collection<AnnotationInstance> getOriginalAnnotations(AnnotationTarget target) {
        switch (target.kind()) {
            case CLASS:
                return target.asClass().classAnnotations();
            case METHOD:
                // Note that the returning collection also contains method params annotations
                return target.asMethod().annotations();
            case FIELD:
                return target.asField().annotations();
            default:
                throw new IllegalArgumentException("Unsupported annotation target");
        }
    }

    private List<AnnotationsTransformer> initTransformers(Kind kind, Collection<AnnotationsTransformer> transformers) {
        List<AnnotationsTransformer> found = new ArrayList<>();
        for (AnnotationsTransformer transformer : transformers) {
            if (transformer.appliesTo(kind)) {
                found.add(transformer);
            }
        }
        if (found.isEmpty()) {
            return Collections.emptyList();
        }
        found.sort(AnnotationsTransformer::compare);
        return found;
    }

    static class TransformationContextImpl extends AnnotationsTransformationContext<Collection<AnnotationInstance>>
            implements AnnotationsTransformer.TransformationContext {

        public TransformationContextImpl(AnnotationTarget target,
                Collection<AnnotationInstance> annotations) {
            super(target, annotations);
        }

        @Override
        public Transformation transform() {
            return new Transformation(new ArrayList<>(getAnnotations()), getTarget(), this::setAnnotations);
        }

    }

    /**
     * We cannot use annotation target directly as a key in a Map. Only {@link MethodInfo} overrides equals/hashCode.
     */
    static final class AnnotationTargetKey {

        final AnnotationTarget target;

        public AnnotationTargetKey(AnnotationTarget target) {
            this.target = target;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            AnnotationTargetKey other = (AnnotationTargetKey) obj;
            if (target.kind() != other.target.kind()) {
                return false;
            }
            switch (target.kind()) {
                case METHOD:
                    return target.asMethod().equals(other.target);
                case FIELD:
                    FieldInfo field = target.asField();
                    FieldInfo otherField = other.target.asField();
                    return Objects.equals(field.name(), otherField.name())
                            && Objects.equals(field.declaringClass().name(), otherField.declaringClass().name());
                case CLASS:
                    return target.asClass().name().equals(other.target.asClass().name());
                default:
                    throw unsupportedAnnotationTarget(target);
            }
        }

        @Override
        public int hashCode() {
            switch (target.kind()) {
                case METHOD:
                    return target.asMethod().hashCode();
                case FIELD:
                    return Objects.hash(target.asField().name(), target.asField().declaringClass().name());
                case CLASS:
                    return target.asClass().name().hashCode();
                default:
                    throw unsupportedAnnotationTarget(target);
            }
        }

    }

    private static IllegalArgumentException unsupportedAnnotationTarget(AnnotationTarget target) {
        return new IllegalArgumentException("Unsupported annotation target: " + target.kind());
    }

}
