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

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

/**
 * Allows a build-time extension to alter qualifiers on an injection point.
 * <p>
 *
 * @author Matej Novotny
 */
public interface InjectionPointsTransformer extends BuildExtension {

    /**
     * Returns true if this transformer is meant to be applied to the supplied {@code requiredType}.
     *
     * @param requiredType the declared type of the injection point
     */
    boolean appliesTo(Type requiredType);

    /**
     * Method is invoked for each injection point that returns true from {@link #appliesTo(Type)}.
     * For further filtering (declaring class, qualifiers present and so on), user can use helper methods
     * present within {@link TransformationContext}.
     *
     * @param transformationContext
     */
    void transform(TransformationContext transformationContext);

    interface TransformationContext extends BuildExtension.BuildContext {

        /**
         * Returns {@link AnnotationTarget} representing this injection point.
         *
         * @return the annotation target of this injection point
         */
        AnnotationTarget getTarget();

        /**
         * Returns current set of annotations instances - qualifiers.
         *
         * @return the annotation instances
         */
        Collection<AnnotationInstance> getQualifiers();

        /**
         * Retrieves all annotations attached to the {@link AnnotationTarget} that this transformer operates on
         * even if they were altered by {@code AnnotationsTransformer}. This method is preferred to manual inspection
         * of {@link AnnotationTarget} which may, in some corner cases, hold outdated information.
         *
         * The resulting set of annotations contains all annotations, not just CDI qualifiers.
         * If the annotation target is a method, then this set contains annotations that belong to the method itself
         * as well as to its parameters.
         *
         * @return collection of all annotations related to given {@link AnnotationTarget}
         */
        Collection<AnnotationInstance> getAllAnnotations();

        /**
         * /**
         * The transformation is not applied until {@link Transformation#done()} is invoked.
         *
         * @return a new transformation
         */
        Transformation transform();

    }

    final class Transformation {
        private final InjectionPointModifier.TransformationContextImpl transformationContext;

        private final Set<AnnotationInstance> modified;

        Transformation(InjectionPointModifier.TransformationContextImpl transformationContext) {
            this.transformationContext = transformationContext;
            this.modified = new HashSet<>(transformationContext.getQualifiers());
        }

        /**
         *
         * @param annotation
         * @return self
         */
        public Transformation add(AnnotationInstance annotation) {
            modified.add(annotation);
            return this;
        }

        /**
         *
         * @param annotations
         * @return self
         */
        public Transformation addAll(Collection<AnnotationInstance> annotations) {
            modified.addAll(annotations);
            return this;
        }

        /**
         *
         * @param annotations
         * @return self
         */
        public Transformation addAll(AnnotationInstance... annotations) {
            Collections.addAll(modified, annotations);
            return this;
        }

        /**
         * NOTE: The annotation target is derived from the {@link TransformationContext}.
         *
         * @param annotationType
         * @param values
         * @return self
         */
        public Transformation add(Class<? extends Annotation> annotationType, AnnotationValue... values) {
            add(DotNames.create(annotationType.getName()), values);
            return this;
        }

        /**
         * NOTE: The annotation target is derived from the {@link TransformationContext}.
         *
         * @param name
         * @param values
         * @return self
         */
        public Transformation add(DotName name, AnnotationValue... values) {
            add(AnnotationInstance.create(name, transformationContext.getTarget(), values));
            return this;
        }

        /**
         *
         * @param predicate
         * @return self
         */
        public Transformation remove(Predicate<AnnotationInstance> predicate) {
            modified.removeIf(predicate);
            return this;
        }

        /**
         *
         * @return self
         */
        public Transformation removeAll() {
            modified.clear();
            return this;
        }

        /**
         * Applies the transformation.
         *
         * @see TransformationContext#getQualifiers()
         */
        public void done() {
            transformationContext.setQualifiers(modified);
        }
    }
}
