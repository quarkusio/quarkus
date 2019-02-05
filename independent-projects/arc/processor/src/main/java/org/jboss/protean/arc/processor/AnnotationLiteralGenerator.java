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

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.util.AnnotationLiteral;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.protean.arc.ComputingCache;
import org.jboss.protean.arc.processor.AnnotationLiteralProcessor.Key;
import org.jboss.protean.arc.processor.AnnotationLiteralProcessor.Literal;
import org.jboss.protean.arc.processor.ResourceOutput.Resource;
import org.jboss.protean.gizmo.BytecodeCreator;
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

    static final String SHARED_SUFFIX = "_Shared";

    private static final Logger LOGGER = Logger.getLogger(AnnotationLiteralGenerator.class);

    /**
     *
     * @param beanDeployment
     * @param annotationLiterals
     * @return a collection of resources
     */
    Collection<Resource> generate(String name, BeanDeployment beanDeployment, ComputingCache<Key, Literal> annotationLiteralsCache) {
        List<Resource> resources = new ArrayList<>();
        annotationLiteralsCache.forEachEntry((key, literal) -> {
            ResourceClassOutput classOutput = new ResourceClassOutput(literal.isApplicationClass);
            createSharedAnnotationLiteral(classOutput, key, literal);
            resources.addAll(classOutput.getResources());
        });
        return resources;
    }
    
    static void createSharedAnnotationLiteral(ClassOutput classOutput, Key key, Literal literal) {
        // Ljavax/enterprise/util/AnnotationLiteral<Lcom/foo/MyQualifier;>;Lcom/foo/MyQualifier;
        String signature = String.format("Ljavax/enterprise/util/AnnotationLiteral<L%1$s;>;L%1$s;", key.annotationName.toString().replace('.', '/'));
        String generatedName = literal.className.replace('.', '/');

        ClassCreator annotationLiteral = ClassCreator.builder().classOutput(classOutput).className(generatedName).superClass(AnnotationLiteral.class)
                .interfaces(key.annotationName.toString()).signature(signature).build();

        MethodCreator constructor = annotationLiteral.getMethodCreator(Methods.INIT, "V",
                literal.constructorParams.stream().map(m -> m.returnType().name().toString()).toArray());
        constructor.invokeSpecialMethod(MethodDescriptor.ofConstructor(AnnotationLiteral.class), constructor.getThis());

        for (ListIterator<MethodInfo> iterator = literal.constructorParams.listIterator(); iterator.hasNext();) {
            MethodInfo param = iterator.next();
            String returnType = param.returnType().name().toString();
            // field
            annotationLiteral.getFieldCreator(param.name(), returnType).setModifiers(ACC_PRIVATE | ACC_FINAL);
            // constructor param
            constructor.writeInstanceField(FieldDescriptor.of(annotationLiteral.getClassName(), param.name(), returnType), constructor.getThis(),
                    constructor.getMethodParam(iterator.previousIndex()));
            // value method
            MethodCreator value = annotationLiteral.getMethodCreator(param.name(), returnType).setModifiers(ACC_PUBLIC);
            value.returnValue(value.readInstanceField(FieldDescriptor.of(annotationLiteral.getClassName(), param.name(), returnType), value.getThis()));
        }
        constructor.returnValue(null);
        
        annotationLiteral.close();
        LOGGER.debugf("Shared annotation literal generated: %s", literal.className);
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
            if(method.name().equals(Methods.CLINIT) || method.name().equals(Methods.INIT)) {
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
                retValue = loadValue(valueMethod, value, annotationClass, method);
            }
            valueMethod.returnValue(retValue);
        }
        annotationLiteral.close();
        LOGGER.debugf("Annotation literal generated: %s", literalName);
    }
    
    static ResultHandle loadValue(BytecodeCreator valueMethod, AnnotationValue value, ClassInfo annotationClass, MethodInfo method) {
        ResultHandle retValue;
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
                retValue = arrayValue(value, valueMethod, method, annotationClass);
                break;
            case ENUM:
                retValue = valueMethod
                        .readStaticField(FieldDescriptor.of(value.asEnumType().toString(), value.asEnum(), value.asEnumType().toString()));
                break;
            case NESTED:
            default:
                throw new UnsupportedOperationException("Unsupported value: " + value);
        }
        return retValue;
    }

    static ResultHandle arrayValue(AnnotationValue value, BytecodeCreator valueMethod, MethodInfo method, ClassInfo annotationClass) {
        ResultHandle retValue;
        switch (value.componentKind()) {
            case CLASS:
                Type[] classArray = value.asClassArray();
                retValue = valueMethod.newArray(componentType(method), valueMethod.load(classArray.length));
                for (int i = 0; i < classArray.length; i++) {
                    valueMethod.writeArrayValue(retValue, i, valueMethod.loadClass(classArray[i].name()
                            .toString()));
                }
                break;
            case STRING:
                String[] stringArray = value.asStringArray();
                retValue = valueMethod.newArray(componentType(method), valueMethod.load(stringArray.length));
                for (int i = 0; i < stringArray.length; i++) {
                    valueMethod.writeArrayValue(retValue, i, valueMethod.load(stringArray[i]));
                }
                break;
            case INTEGER:
                int[] intArray = value.asIntArray();
                retValue = valueMethod.newArray(componentType(method), valueMethod.load(intArray.length));
                for (int i = 0; i < intArray.length; i++) {
                    valueMethod.writeArrayValue(retValue, i, valueMethod.load(intArray[i]));
                }
                break;
            case LONG:
                long[] longArray = value.asLongArray();
                retValue = valueMethod.newArray(componentType(method), valueMethod.load(longArray.length));
                for (int i = 0; i < longArray.length; i++) {
                    valueMethod.writeArrayValue(retValue, i, valueMethod.load(longArray[i]));
                }
                break;
            case BYTE:
                byte[] byteArray = value.asByteArray();
                retValue = valueMethod.newArray(componentType(method), valueMethod.load(byteArray.length));
                for (int i = 0; i < byteArray.length; i++) {
                    valueMethod.writeArrayValue(retValue, i, valueMethod.load(byteArray[i]));
                }
                break;
            case CHARACTER:
                char[] charArray = value.asCharArray();
                retValue = valueMethod.newArray(componentType(method), valueMethod.load(charArray.length));
                for (int i = 0; i < charArray.length; i++) {
                    valueMethod.writeArrayValue(retValue, i, valueMethod.load(charArray[i]));
                }
                break;
            // TODO: handle other less common types of array components
            default:
                // Return empty array for empty arrays and unsupported types
                // For an empty array the component kind is UNKNOWN
                if (value.componentKind() != org.jboss.jandex.AnnotationValue.Kind.UNKNOWN) {
                    // Unsupported type - check if it is @Nonbinding, @Nonbinding array members should not be a problem in CDI
                    AnnotationInstance nonbinding = method.annotation(DotNames.NONBINDING);
                    if (nonbinding == null || nonbinding.target()
                            .kind() != Kind.METHOD) {
                        LOGGER.warnf("Unsupported array component type %s on %s - literal returns an empty array", method, annotationClass);
                    }
                }
                retValue = valueMethod.newArray(componentType(method), valueMethod.load(0));
        }
        return retValue;
    }

    static String componentType(MethodInfo method) {
        ArrayType arrayType = method.returnType().asArrayType();
        return arrayType.component().name().toString();
    }

    static String generatedSharedName(DotName annotationName) {
        // com.foo.MyQualifier -> com.foo.MyQualifier1_Shared_AnnotationLiteral
        return DotNames.packageName(annotationName) + "." + DotNames.simpleName(annotationName) + SHARED_SUFFIX + ANNOTATION_LITERAL_SUFFIX;
    }

    static String generatedLocalName(String targetPackage, String simpleName, String hash) {
        // com.foo.MyQualifier -> com.bar.MyQualifier_somehashvalue_AnnotationLiteral
        return targetPackage + "." + simpleName + hash + AnnotationLiteralGenerator.ANNOTATION_LITERAL_SUFFIX;
    }

}
