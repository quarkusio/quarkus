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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.util.AnnotationLiteral;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.protean.arc.ComputingCache;
import org.jboss.protean.arc.processor.AnnotationLiteralProcessor.CacheKey;
import org.jboss.protean.arc.processor.ResourceOutput.Resource;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.ClassOutput;
import org.jboss.protean.gizmo.FieldDescriptor;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;

/**
 *
 * @author Martin Kouba
 */
public class AnnotationLiteralGenerator extends AbstractGenerator {

    static final String ANNOTATION_LITERAL_SUFFIX = "_AnnotationLiteral";

    private static final Logger LOGGER = Logger.getLogger(AnnotationLiteralGenerator.class);

    private static final String INIT = "<init>";
    private static final String CLINIT = "<clinit>";

    /**
     *
     * @param beanDeployment
     * @param annotationLiterals
     * @return a collection of resources
     */
    Collection<Resource> generate(String name, BeanDeployment beanDeployment, ComputingCache<CacheKey, String> annotationLiteralsCache) {
        ResourceClassOutput classOutput = new ResourceClassOutput(true);

        annotationLiteralsCache.forEachEntry(
                (key, literalName) -> createAnnotationLiteral(classOutput, beanDeployment.getIndex().getClassByName(key.name), key.values, literalName));

        return classOutput.getResources();
    }

    static void createAnnotationLiteral(ClassOutput classOutput, ClassInfo annotationClass, AnnotationInstance annotationInstance, String literalName) {
        createAnnotationLiteral(classOutput, annotationClass, annotationInstance.values(), literalName);
    }

    static void createAnnotationLiteral(ClassOutput classOutput, ClassInfo annotationClass, List<AnnotationValue> values, String literalName) {

        Map<String, AnnotationValue> annotationValues = values.stream().collect(Collectors.toMap(AnnotationValue::name, Function.identity()));

        // Ljavax/enterprise/util/AnnotationLiteral<Lcom/foo/MyQualifier;>;Lcom/foo/MyQualifier;
        String signature = String.format("Ljavax/enterprise/util/AnnotationLiteral<L%1$s;>;L%1$s;", annotationClass.name().toString().replace('.', '/'));
        String generatedName = literalName.replace('.', '/');

        ClassCreator annotationLiteral = ClassCreator.builder().classOutput(classOutput).className(generatedName).superClass(AnnotationLiteral.class)
                .interfaces(annotationClass.name().toString()).signature(signature).build();

        for (MethodInfo method : annotationClass.methods()) {
            if(method.name().equals(CLINIT) || method.name().equals(INIT)) {
                continue;
            }
            MethodCreator valueMethod = annotationLiteral.getMethodCreator(MethodDescriptor.of(method));
            AnnotationValue value = annotationValues.get(method.name());
            if (value == null) {
                value = method.defaultValue();
            }
            ResultHandle retValue = null;
            if (value == null) {
                switch (method.returnType().kind()) {
                    case CLASS:
                    case ARRAY:
                        retValue = valueMethod.loadNull();
                        break;
                    case PRIMITIVE:
                        PrimitiveType primitiveType = method.returnType().asPrimitiveType();
                        switch (primitiveType.primitive()) {
                            case BOOLEAN:
                                retValue = valueMethod.load(false);
                                break;
                            case BYTE:
                            case SHORT:
                            case INT:
                                retValue = valueMethod.load(0);
                                break;
                            case LONG:
                                retValue = valueMethod.load(0L);
                                break;
                            case FLOAT:
                                retValue = valueMethod.load(0.0f);
                                break;
                            case DOUBLE:
                                retValue = valueMethod.load(0.0d);
                                break;
                            case CHAR:
                                retValue = valueMethod.load('\u0000');
                                break;
                        }
                        break;
                    default:
                        break;
                }
            } else {
                switch (value.kind()) {
                    case BOOLEAN:
                        retValue = valueMethod.load(value.asBoolean());
                        break;
                    case STRING:
                        retValue = valueMethod.load(value.asString());
                        break;
                    case BYTE:
                        retValue = valueMethod.load(value.asByte());
                        break;
                    case SHORT:
                        retValue = valueMethod.load(value.asShort());
                        break;
                    case LONG:
                        retValue = valueMethod.load(value.asLong());
                        break;
                    case INTEGER:
                        retValue = valueMethod.load(value.asInt());
                        break;
                    case FLOAT:
                        retValue = valueMethod.load(value.asFloat());
                        break;
                    case DOUBLE:
                        retValue = valueMethod.load(value.asDouble());
                        break;
                    case CHARACTER:
                        retValue = valueMethod.load(value.asChar());
                        break;
                    case CLASS:
                        retValue = valueMethod.loadClass(value.asClass().toString());
                        break;
                    case ARRAY:
                        switch (value.componentKind()) {
                            case CLASS:
                                Type[] classArray = value.asClassArray();
                                retValue = valueMethod.newArray(componentType(method), valueMethod.load(classArray.length));
                                for (int i = 0; i < classArray.length; i++) {
                                    valueMethod.writeArrayValue(retValue, i, valueMethod.loadClass(classArray[i].name().toString()));
                                }
                                break;
                            // TODO other types of array components
                            // Note that array members should be Nonbinding in CDI
                            default:
                                // For an empty array component kind is UNKNOWN

                                String arrayType = componentType(method);
                                if (!arrayType.equals(Class.class.getName())) {
                                    LOGGER.warnf("Unsupported array component type %s on %s - literal returns an empty array", method, annotationClass);
                                }
                                retValue = valueMethod.newArray(arrayType, valueMethod.load(0));
                        }
                        break;
                    case ENUM:
                        retValue = valueMethod
                                .readStaticField(FieldDescriptor.of(value.asEnumType().toString(), value.asEnum(), value.asEnumType().toString()));
                        break;
                    case NESTED:
                    default:
                        throw new UnsupportedOperationException();
                }
            }
            valueMethod.returnValue(retValue);
        }
        annotationLiteral.close();
        LOGGER.debugf("Annotation literal generated: " + literalName);
    }

    static String componentType(MethodInfo method) {
        ArrayType arrayType = method.returnType().asArrayType();
        return arrayType.component().name().toString();
    }

    static String generatedSharedName(String prefix, String simpleName, AtomicInteger index) {
        // com.foo.MyQualifier -> org.jboss.protean.arc.setup.Default_MyQualifier1_AnnotationLiteral
        return ComponentsProviderGenerator.SETUP_PACKAGE + "." + prefix + "_" + simpleName + index.incrementAndGet()
                + AnnotationLiteralGenerator.ANNOTATION_LITERAL_SUFFIX;
    }

    static String generatedLocalName(String targetPackage, String simpleName, AtomicInteger index) {
        // com.foo.MyQualifier -> com.bar.MyQualifier1_AnnotationLiteral
        return targetPackage + "." + simpleName + index.incrementAndGet() + AnnotationLiteralGenerator.ANNOTATION_LITERAL_SUFFIX;
    }

}
