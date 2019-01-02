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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.protean.arc.ComputingCache;
import org.jboss.protean.gizmo.ClassOutput;

/**
 *
 * @author Martin Kouba
 */
public class AnnotationLiteralProcessor {

    private final AtomicInteger index;

    private final ComputingCache<CacheKey, String> cache;

    public AnnotationLiteralProcessor(String name, boolean shared) {
        this.index = new AtomicInteger(1);
        this.cache = shared ? new ComputingCache<>(key -> AnnotationLiteralGenerator.generatedSharedName(name, DotNames.simpleName(key.name), index)) : null;
    }

    boolean hasLiteralsToGenerate() {
        return cache != null && !cache.isEmpty();
    }

    ComputingCache<CacheKey, String> getCache() {
        return cache;
    }

    /**
     *
     * @param classOutput
     * @param annotationClass
     * @return an annotation literal class name
     */
    String process(ClassOutput classOutput, ClassInfo annotationClass, AnnotationInstance annotationInstance, String targetPackage) {
        if (cache != null) {
            return cache.getValue(new CacheKey(annotationInstance.name(), annotationInstance.values()));
        }
        String literalName = AnnotationLiteralGenerator.generatedLocalName(targetPackage, DotNames.simpleName(annotationClass.name()), index);
        AnnotationLiteralGenerator.createAnnotationLiteral(classOutput, annotationClass, annotationInstance, literalName);
        return literalName;
    }

    static class CacheKey {

        final DotName name;

        final List<AnnotationValue> values;

        public CacheKey(DotName name, List<AnnotationValue> values) {
            this.name = name;
            this.values = values;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((values == null) ? 0 : values.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof CacheKey)) {
                return false;
            }
            CacheKey other = (CacheKey) obj;
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!name.equals(other.name)) {
                return false;
            }
            if (values == null) {
                if (other.values != null) {
                    return false;
                }
            } else if (!values.equals(other.values)) {
                return false;
            }
            return true;
        }

    }

}
