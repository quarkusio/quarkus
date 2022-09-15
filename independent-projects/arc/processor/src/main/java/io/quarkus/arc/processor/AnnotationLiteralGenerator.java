package io.quarkus.arc.processor;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;

import io.quarkus.arc.impl.ComputingCache;
import io.quarkus.arc.processor.AnnotationLiteralProcessor.AnnotationLiteralClassInfo;
import io.quarkus.arc.processor.AnnotationLiteralProcessor.CacheKey;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import jakarta.enterprise.util.AnnotationLiteral;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

/**
 * This is an internal companion of {@link AnnotationLiteralProcessor} that handles generating
 * annotation literal classes. See {@link #generate(ComputingCache, Set) generate()} for more info.
 */
public class AnnotationLiteralGenerator extends AbstractGenerator {
    private static final Logger LOGGER = Logger.getLogger(AnnotationLiteralGenerator.class);

    AnnotationLiteralGenerator(boolean generateSources) {
        super(generateSources);
    }

    /**
     * Creator of an {@link AnnotationLiteralProcessor} must call this method at an appropriate point
     * in time and write the result to an appropriate output. If not, the bytecode sequences generated
     * using the {@code AnnotationLiteralProcessor} will refer to non-existing classes.
     *
     * @param existingClasses names of classes that already exist and should not be generated again
     * @return the generated classes, never {@code null}
     */
    Collection<Resource> generate(ComputingCache<CacheKey, AnnotationLiteralClassInfo> cache,
            Set<String> existingClasses) {
        List<ResourceOutput.Resource> resources = new ArrayList<>();
        cache.forEachExistingValue(literal -> {
            ResourceClassOutput classOutput = new ResourceClassOutput(literal.isApplicationClass, generateSources);
            createAnnotationLiteralClass(classOutput, literal, existingClasses);
            resources.addAll(classOutput.getResources());
        });
        return resources;
    }

    /**
     * Creator of an {@link AnnotationLiteralProcessor} must call this method at an appropriate point
     * in time and write the result to an appropriate output. If not, the bytecode sequences generated
     * using the {@code AnnotationLiteralProcessor} will refer to non-existing classes.
     *
     * @param existingClasses names of classes that already exist and should not be generated again
     * @return the generated classes, never {@code null}
     */
    Collection<Future<Collection<Resource>>> generate(ComputingCache<CacheKey, AnnotationLiteralClassInfo> cache,
            Set<String> existingClasses, ExecutorService executor) {
        List<Future<Collection<Resource>>> futures = new ArrayList<>();
        cache.forEachExistingValue(literal -> {
            futures.add(executor.submit(new Callable<Collection<Resource>>() {
                @Override
                public Collection<Resource> call() throws Exception {
                    ResourceClassOutput classOutput = new ResourceClassOutput(literal.isApplicationClass, generateSources);
                    createAnnotationLiteralClass(classOutput, literal, existingClasses);
                    return classOutput.getResources();
                }
            }));
        });
        return futures;
    }

    /**
     * Based on given {@code literal} data, generates an annotation literal class into the given {@code classOutput}.
     * Does nothing if {@code existingClasses} indicates that the class to be generated already exists.
     * <p>
     * The generated annotation literal class is supposed to have a constructor that accepts values
     * of all annotation members.
     *
     * @param classOutput the output to which the class is written
     * @param literal data about the annotation literal class to be generated
     * @param existingClasses set of existing classes that shouldn't be generated again
     */
    private void createAnnotationLiteralClass(ClassOutput classOutput, AnnotationLiteralClassInfo literal,
            Set<String> existingClasses) {
        // Ljavax/enterprise/util/AnnotationLiteral<Lcom/foo/MyQualifier;>;Lcom/foo/MyQualifier;
        String signature = String.format("L%1$s<L%2$s;>;L%2$s;",
                AnnotationLiteral.class.getName().replace('.', '/'),
                literal.annotationClass.toString().replace('.', '/'));

        String generatedName = literal.generatedClassName.replace('.', '/');
        if (existingClasses.contains(generatedName)) {
            return;
        }

        ClassCreator annotationLiteral = ClassCreator.builder()
                .classOutput(classOutput)
                .className(generatedName)
                .superClass(AnnotationLiteral.class)
                .interfaces(literal.annotationName().toString())
                .signature(signature)
                .build();

        MethodCreator constructor = annotationLiteral.getMethodCreator(Methods.INIT, "V",
                literal.annotationMembers().stream().map(m -> m.returnType().name().toString()).toArray());
        constructor.invokeSpecialMethod(MethodDescriptor.ofConstructor(AnnotationLiteral.class), constructor.getThis());

        int constructorParameterIndex = 0;
        for (MethodInfo annotationMember : literal.annotationMembers()) {
            String type = annotationMember.returnType().name().toString();
            // field
            annotationLiteral.getFieldCreator(annotationMember.name(), type).setModifiers(ACC_PRIVATE | ACC_FINAL);

            // constructor: param -> field
            constructor.writeInstanceField(
                    FieldDescriptor.of(annotationLiteral.getClassName(), annotationMember.name(), type),
                    constructor.getThis(), constructor.getMethodParam(constructorParameterIndex));

            // annotation member method implementation
            MethodCreator value = annotationLiteral.getMethodCreator(annotationMember.name(), type).setModifiers(ACC_PUBLIC);
            value.returnValue(value.readInstanceField(
                    FieldDescriptor.of(annotationLiteral.getClassName(), annotationMember.name(), type), value.getThis()));

            constructorParameterIndex++;
        }
        constructor.returnValue(null);

        generateStaticFieldsWithDefaultValues(annotationLiteral, literal.annotationMembers());

        annotationLiteral.close();
        LOGGER.debugf("Annotation literal class generated: %s", literal.generatedClassName);
    }

    static String defaultValueStaticFieldName(MethodInfo annotationMember) {
        return annotationMember.name() + "_default_value";
    }

    private static boolean returnsClassOrClassArray(MethodInfo annotationMember) {
        boolean returnsClass = DotNames.CLASS.equals(annotationMember.returnType().name());
        boolean returnsClassArray = annotationMember.returnType().kind() == Type.Kind.ARRAY
                && DotNames.CLASS.equals(annotationMember.returnType().asArrayType().component().name());
        return returnsClass || returnsClassArray;
    }

    /**
     * Generates {@code public static final} fields for all the annotation members
     * that provide a default value and are of a class or class array type.
     * Also generates a static initializer that assigns the default value of those
     * annotation members to the generated fields.
     *
     * @param classCreator the class to which the fields and the static initializer should be added
     * @param annotationMembers the full set of annotation members of an annotation type
     */
    private static void generateStaticFieldsWithDefaultValues(ClassCreator classCreator, List<MethodInfo> annotationMembers) {
        List<MethodInfo> defaultOfClassType = new ArrayList<>();
        for (MethodInfo annotationMember : annotationMembers) {
            if (annotationMember.defaultValue() != null && returnsClassOrClassArray(annotationMember)) {
                defaultOfClassType.add(annotationMember);
            }
        }

        if (defaultOfClassType.isEmpty()) {
            return;
        }

        MethodCreator staticConstructor = classCreator.getMethodCreator(Methods.CLINIT, void.class);
        staticConstructor.setModifiers(ACC_STATIC);

        for (MethodInfo annotationMember : defaultOfClassType) {
            String type = annotationMember.returnType().name().toString();
            AnnotationValue defaultValue = annotationMember.defaultValue();

            FieldCreator fieldCreator = classCreator.getFieldCreator(defaultValueStaticFieldName(annotationMember), type);
            fieldCreator.setModifiers(ACC_PUBLIC | ACC_STATIC | ACC_FINAL);

            if (defaultValue.kind() == AnnotationValue.Kind.ARRAY) {
                Type[] clazzArray = defaultValue.asClassArray();
                ResultHandle array = staticConstructor.newArray(type, clazzArray.length);
                for (int i = 0; i < clazzArray.length; ++i) {
                    staticConstructor.writeArrayValue(array, staticConstructor.load(i),
                            staticConstructor.loadClass(clazzArray[i].name().toString()));
                }
                staticConstructor.writeStaticField(fieldCreator.getFieldDescriptor(), array);
            } else {
                staticConstructor.writeStaticField(fieldCreator.getFieldDescriptor(),
                        staticConstructor.loadClass(defaultValue.asClass().name().toString()));

            }
        }

        staticConstructor.returnValue(null);
    }
}
