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

package org.jboss.shamrock.jaxrs.runtime.graal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.sun.xml.internal.bind.v2.model.annotation.LocatableAnnotation", onlyWith = InternalLocatableAnnotationSubstitutions.Selector.class)
final class InternalLocatableAnnotationSubstitutions {

    @Substitute
    public static <A extends Annotation> A create(A annotation, Locatable parentSourcePos ) {
        return annotation;
    }

    @Substitute
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        throw new RuntimeException("Not implemented");
    }


    @TargetClass(className = "com.sun.xml.internal.bind.v2.model.annotation.Locatable", onlyWith = InternalLocatableAnnotationSubstitutions.Selector.class)
    static final class Locatable {

    }

    static final class Selector implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("com.sun.xml.internal.bind.v2.model.annotation.LocatableAnnotation");
                return true;
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                return false;
            }
        }
    }
}
