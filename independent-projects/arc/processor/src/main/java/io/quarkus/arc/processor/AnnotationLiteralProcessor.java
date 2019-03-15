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

import io.quarkus.arc.ComputingCache;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

/**
 *
 * @author Martin Kouba
 */
class AnnotationLiteralProcessor {

    private final ComputingCache<Key, Literal> cache;

    AnnotationLiteralProcessor(boolean shared, Predicate<DotName> applicationClassPredicate) {
        this.cache = shared ? new ComputingCache<>(key -> {
            return new Literal(AnnotationLiteralGenerator.generatedSharedName(key.annotationName),
                    applicationClassPredicate.test(key.annotationName),
                    key.annotationClass.methods()
                            .stream()
                            .filter(m -> !m.name().equals(Methods.CLINIT) && !m.name().equals(Methods.INIT))
                            .collect(Collectors.toList()));
        }) : null;
    }

    boolean hasLiteralsToGenerate() {
        return cache != null && !cache.isEmpty();
    }

    ComputingCache<Key, Literal> getCache() {
        return cache;
    }

    /**
     *
     * @param bytecode
     * @param classOutput
     * @param annotationClass
     * @param annotationInstance
     * @param targetPackage Target package is only used if annotation literals are not shared
     * @return an annotation literal result handle
     */
    ResultHandle process(BytecodeCreator bytecode, ClassOutput classOutput, ClassInfo annotationClass,
            AnnotationInstance annotationInstance,
            String targetPackage) {
        Objects.requireNonNull(annotationClass, "Annotation class not available: " + annotationInstance);
        if (cache != null) {
            Literal literal = cache.getValue(new Key(annotationInstance.name(), annotationClass));

            Map<String, AnnotationValue> annotationValues = annotationInstance.values().stream()
                    .collect(Collectors.toMap(AnnotationValue::name, Function.identity()));

            ResultHandle[] constructorParams = new ResultHandle[literal.constructorParams.size()];

            for (ListIterator<MethodInfo> iterator = literal.constructorParams.listIterator(); iterator.hasNext();) {
                MethodInfo method = iterator.next();
                AnnotationValue value = annotationValues.get(method.name());
                if (value == null) {
                    value = method.defaultValue();
                }
                if (value == null) {
                    throw new NullPointerException("Value not set for " + method);
                }
                ResultHandle retValue = AnnotationLiteralGenerator.loadValue(bytecode, value, annotationClass, method);
                constructorParams[iterator.previousIndex()] = retValue;
            }
            return bytecode
                    .newInstance(MethodDescriptor.ofConstructor(literal.className,
                            literal.constructorParams.stream().map(m -> m.returnType().name().toString()).toArray()),
                            constructorParams);
        } else {
            String literalClassName = AnnotationLiteralGenerator.generatedLocalName(targetPackage,
                    DotNames.simpleName(annotationClass),
                    Hashes.sha1(annotationInstance.toString()));
            AnnotationLiteralGenerator.createAnnotationLiteral(classOutput, annotationClass, annotationInstance,
                    literalClassName);
            return bytecode.newInstance(MethodDescriptor.ofConstructor(literalClassName));
        }
    }

    static class Literal {

        final String className;

        final boolean isApplicationClass;

        final List<MethodInfo> constructorParams;

        public Literal(String className, boolean isApplicationClass, List<MethodInfo> constructorParams) {
            this.className = className;
            this.isApplicationClass = isApplicationClass;
            this.constructorParams = constructorParams;
        }

    }

    static class Key {

        final DotName annotationName;

        final ClassInfo annotationClass;

        public Key(DotName name, ClassInfo annotationClass) {
            this.annotationName = name;
            this.annotationClass = annotationClass;
        }

        @Override
        public int hashCode() {
            return Objects.hash(annotationName);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Key other = (Key) obj;
            return Objects.equals(annotationName, other.annotationName);
        }

    }

}
