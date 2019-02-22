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

package io.quarkus.runtime.graal;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(Method.class)
final class MethodReplacement {

    //https://github.com/oracle/graal/issues/649
    @Substitute
    public Object getDefaultValue() {
        return null;
    }

    @Alias
    public Type getGenericReturnType() {
        return null;
    }

    @Alias
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return null;
    }

    @Alias
    public Annotation[] getDeclaredAnnotations() {
        return null;
    }

    @Substitute
    public AnnotatedType getAnnotatedReturnType() {
        return new AnnotatedType() {
            @Override
            public Type getType() {
                return getGenericReturnType();
            }

            @Override
            public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                return MethodReplacement.class.getAnnotation(annotationClass);
            }

            @Override
            public Annotation[] getAnnotations() {
                return MethodReplacement.class.getDeclaredAnnotations();
            }

            @Override
            public Annotation[] getDeclaredAnnotations() {
                return MethodReplacement.class.getDeclaredAnnotations();
            }
        };
    }

}
