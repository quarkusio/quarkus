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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.protean.arc.processor.AnnotationStore.TransformationContextImpl;
import org.jboss.protean.arc.processor.AnnotationsTransformer.TransformationContext;

/**
 * Convenient helper class.
 */
public final class Transformation {

    private final TransformationContextImpl transformationContext;

    private final List<AnnotationInstance> modified;

    public Transformation(TransformationContextImpl transformationContext) {
        this.transformationContext = transformationContext;
        this.modified = new ArrayList<>(transformationContext.getAnnotations());
    }

    public Transformation add(AnnotationInstance annotation) {
        modified.add(annotation);
        return this;
    }

    public Transformation addAll(Collection<AnnotationInstance> annotations) {
        modified.addAll(annotations);
        return this;
    }
    
    public Transformation addAll(AnnotationInstance... annotations) {
        Collections.addAll(modified, annotations);
        return this;
    }
    
    public Transformation add(Class<? extends Annotation> annotationType, AnnotationValue... values) {
        add(DotNames.create(annotationType.getName()), values);
        return this;
    }

    public Transformation add(DotName name, AnnotationValue... values) {
        add(AnnotationInstance.create(name, transformationContext.getTarget(), values));
        return this;
    }

    public Transformation remove(Predicate<AnnotationInstance> predicate) {
        modified.removeIf(predicate);
        return this;
    }
    
    public Transformation removeAll() {
        modified.clear();
        return this;
    }

    /**
     * Applies the transformation.
     * 
     * @see TransformationContext#getAnnotations()
     */
    public void done() {
        transformationContext.setAnnotations(modified);
    }

}