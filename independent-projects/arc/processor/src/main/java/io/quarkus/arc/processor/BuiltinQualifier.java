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

package io.quarkus.arc.processor;

import java.util.Collections;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.protean.gizmo.BytecodeCreator;
import org.jboss.protean.gizmo.FieldDescriptor;
import org.jboss.protean.gizmo.ResultHandle;

enum BuiltinQualifier {

    DEFAULT(AnnotationInstance.create(DotNames.DEFAULT, null, Collections.emptyList()),
            Default.Literal.class.getName()), ANY(AnnotationInstance.create(DotNames.ANY, null, Collections.emptyList()), Any.Literal.class.getName()),;

    private final AnnotationInstance instance;

    private final String literalType;

    private BuiltinQualifier(AnnotationInstance instance, String literalType) {
        this.instance = instance;
        this.literalType = literalType;
    }

    AnnotationInstance getInstance() {
        return instance;
    }

    ResultHandle getLiteralInstance(BytecodeCreator creator) {
        return creator.readStaticField(FieldDescriptor.of(literalType, "INSTANCE", literalType));
    }

    static BuiltinQualifier of(AnnotationInstance instance) {
        for (BuiltinQualifier qualifier : values()) {
            if (qualifier.getInstance().name().equals(instance.name())) {
                return qualifier;
            }
        }
        return null;
    }

}
