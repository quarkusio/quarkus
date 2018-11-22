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
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

public final class Annotations {

    private Annotations() {
    }

    static AnnotationInstance find(Collection<AnnotationInstance> annotations, DotName name) {
        if (annotations.isEmpty()) {
            return null;
        }
        for (AnnotationInstance annotationInstance : annotations) {
            if (annotationInstance.name().equals(name)) {
                return annotationInstance;
            }
        }
        return null;
    }

    static boolean contains(Collection<AnnotationInstance> annotations, DotName name) {
        if (annotations.isEmpty()) {
            return false;
        }
        for (AnnotationInstance annotationInstance : annotations) {
            if (annotationInstance.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    static boolean containsAny(Collection<AnnotationInstance> annotations, Iterable<DotName> names) {
        if (annotations.isEmpty()) {
            return false;
        }
        for (AnnotationInstance annotationInstance : annotations) {
            for (DotName name : names) {
                if (annotationInstance.name().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    static Object convertAnnotationValue(AnnotationValue value, MethodInfo method) {
        if (value.kind().equals(org.jboss.jandex.AnnotationValue.Kind.ARRAY)) {
            // Array members must be Nonbinding
            return "new " + method.returnType().asArrayType().component().name() + "[]{}";
        } else if (value.kind().equals(org.jboss.jandex.AnnotationValue.Kind.BOOLEAN)) {
            return value.asBoolean();
        } else if (value.kind().equals(org.jboss.jandex.AnnotationValue.Kind.STRING)) {
            return "\"" + value.asString() + "\"";
        } else if (value.kind().equals(org.jboss.jandex.AnnotationValue.Kind.ENUM)) {
            return Types.convertNested(method.returnType().name()) + "." + value.asEnum();
        } else {
            return value.toString();
        }
    }

}
