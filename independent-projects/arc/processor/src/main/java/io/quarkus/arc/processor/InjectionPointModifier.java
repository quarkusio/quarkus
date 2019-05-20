/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.arc.processor;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.Type;

/**
 * Holds all {@link InjectionPointsTransformer} instances and provides helper method to apply them on a given
 * type. This is used in {@link InjectionPointInfo}, prior to creating actual injection points.
 *
 * In order to operate on up-to-date annotations, this class also leverages {@link AnnotationStore}.
 *
 * @author Matej Novotny
 */
public class InjectionPointModifier {

    private List<InjectionPointsTransformer> transformers;
    private BuildExtension.BuildContext buildContext;
    private AnnotationStore annotationStore;

    InjectionPointModifier(List<InjectionPointsTransformer> transformers, BuildExtension.BuildContext buildContext) {
        this.buildContext = buildContext;
        this.transformers = transformers;
        this.annotationStore = buildContext != null ? buildContext.get(BuildExtension.Key.ANNOTATION_STORE) : null;
    }

    public Set<AnnotationInstance> applyTransformers(Type type, AnnotationTarget target, Set<AnnotationInstance> qualifiers) {
        // with no transformers, we just immediately return original set of qualifiers
        if (transformers.isEmpty()) {
            return qualifiers;
        }
        TransformationContextImpl transformationContext = new TransformationContextImpl(target, qualifiers,
                annotationStore);
        for (InjectionPointsTransformer transformer : transformers) {
            if (transformer.appliesTo(type)) {
                transformer.transform(transformationContext);
            }
        }
        return transformationContext.getQualifiers();
    }

    class TransformationContextImpl implements InjectionPointsTransformer.TransformationContext {

        private AnnotationTarget target;
        private Set<AnnotationInstance> qualifiers;
        private AnnotationStore annotationStore;

        TransformationContextImpl(AnnotationTarget target, Set<AnnotationInstance> qualifiers,
                AnnotationStore annotationStore) {
            this.target = target;
            this.qualifiers = qualifiers;
            this.annotationStore = annotationStore;
        }

        @Override
        public AnnotationTarget getTarget() {
            return target;
        }

        @Override
        public Set<AnnotationInstance> getQualifiers() {
            return qualifiers;
        }

        @Override
        public Collection<AnnotationInstance> getAllAnnotations() {
            if (annotationStore == null) {
                throw new IllegalStateException(
                        "Attempted to use TransformationContext#getAllAnnotations but AnnotationStore wasn't initialized.");
            }
            return annotationStore.getAnnotations(getTarget());
        }

        @Override
        public InjectionPointsTransformer.Transformation transform() {
            return new InjectionPointsTransformer.Transformation(this);
        }

        @Override
        public <V> V get(BuildExtension.Key<V> key) {
            return buildContext.get(key);
        }

        @Override
        public <V> V put(BuildExtension.Key<V> key, V value) {
            return buildContext.put(key, value);
        }

        public void setQualifiers(Set<AnnotationInstance> qualifiers) {
            this.qualifiers = qualifiers;
        }
    }
}
