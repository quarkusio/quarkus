/*
 * Copyright 2018 Red Hat, Inc.
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

package org.jboss.protean.arc.processor;

import java.util.Collection;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;

/**
 * Allows a build-time extension to override the annotations that exist on bean classes.
 * <p>
 * The container should use {@link AnnotationStore} to obtain annotations of any {@link org.jboss.jandex.ClassInfo}, {@link org.jboss.jandex.FieldInfo} and
 * {@link org.jboss.jandex.MethodInfo}.
 *
 * @author Martin Kouba
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
         * The transformation is not applied until {@link Transformation#done()} is invoked.
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

}
