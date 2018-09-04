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

    /**
     *
     * @param beanDeployment
     * @param annotationLiterals
     * @return a collection of resources
     */
    Collection<Resource> generate(String name, BeanDeployment beanDeployment, ComputingCache<CacheKey, String> annotationLiteralsCache) {
        ResourceClassOutput classOutput = new ResourceClassOutput();

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
        String signature = String.format("Ljavax/enterprise/util/AnnotationLiteral<L%1$s;>;L%1$s;", annotationClass.name().toString().replace(".", "/"));
        String generatedName = literalName.replace(".", "/");

        ClassCreator annotationLiteral = ClassCreator.builder().classOutput(classOutput).className(generatedName).superClass(AnnotationLiteral.class)
                .interfaces(annotationClass.name().toString()).signature(signature).build();

        for (MethodInfo method : annotationClass.methods()) {
            MethodCreator valueMethod = annotationLiteral.getMethodCreator(MethodDescriptor.of(method));
            AnnotationValue value = annotationValues.get(method.name());
            if (value == null) {
                value = method.defaultValue();
            }
            ResultHandle retValue = null;
            if (value != null) {
                switch (value.kind()) {
                    case BOOLEAN:
                        retValue = value != null ? valueMethod.load(value.asBoolean()) : valueMethod.load(false);
                        break;
                    case STRING:
                        retValue = value != null ? valueMethod.load(value.asString()) : valueMethod.loadNull();
                        break;
                    case BYTE:
                        retValue = value != null ? valueMethod.load(value.asByte()) : valueMethod.load(0);
                        break;
                    case SHORT:
                        retValue = value != null ? valueMethod.load(value.asShort()) : valueMethod.load(0);
                        break;
                    case LONG:
                        retValue = value != null ? valueMethod.load(value.asLong()) : valueMethod.load(0L);
                        break;
                    case INTEGER:
                        retValue = value != null ? valueMethod.load(value.asInt()) : valueMethod.load(0);
                        break;
                    case FLOAT:
                        retValue = value != null ? valueMethod.load(value.asFloat()) : valueMethod.load(0.0f);
                        break;
                    case DOUBLE:
                        retValue = value != null ? valueMethod.load(value.asDouble()) : valueMethod.load(0.0d);
                        break;
                    case CHARACTER:
                        retValue = value != null ? valueMethod.load(value.asChar()) : valueMethod.load('\u0000');
                        break;
                    case CLASS:
                        retValue = value != null ? valueMethod.loadClass(value.asClass().toString()) : valueMethod.loadNull();
                        break;
                    case ARRAY:
                        // Always return an empty array
                        // Array members must be Nonbinding
                        retValue = value != null ? valueMethod.newArray(componentType(method), valueMethod.load(0)) : valueMethod.loadNull();
                        break;
                    case ENUM:
                        retValue = value != null
                                ? valueMethod.readStaticField(FieldDescriptor.of(value.asEnumType().toString(), value.asEnum(), value.asEnumType().toString()))
                                : valueMethod.loadNull();
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
