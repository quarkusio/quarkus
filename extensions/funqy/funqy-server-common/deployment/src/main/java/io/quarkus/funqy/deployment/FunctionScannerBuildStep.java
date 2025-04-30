package io.quarkus.funqy.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.quarkus.funqy.deployment.ReflectionRegistrationUtil.*;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.Transformation;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.funqy.Context;
import io.quarkus.funqy.Funq;
import io.quarkus.funqy.runtime.FunctionRecorder;
import io.quarkus.gizmo.Gizmo;

public class FunctionScannerBuildStep {
    public static final DotName FUNQ = DotName.createSimple(Funq.class.getName());
    public static final DotName CONTEXT = DotName.createSimple(Context.class.getName());

    @BuildStep
    public void scanFunctions(BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            BuildProducer<FunctionBuildItem> functions) {
        IndexView index = beanArchiveIndexBuildItem.getIndex();
        Collection<AnnotationInstance> funqs = index.getAnnotations(FUNQ);
        Set<ClassInfo> classes = new HashSet<>();
        Set<String> classNames = new HashSet<>();
        for (AnnotationInstance funqMethod : funqs) {
            MethodInfo method = funqMethod.target().asMethod();
            String className = method.declaringClass().name().toString();
            classNames.add(className);
            classes.add(method.declaringClass());
            String methodName = method.name();

            if (!Modifier.isPublic(method.flags())) {
                throw new RuntimeException(
                        String.format("Method '%s' annotated with '@Funq' declared in the class '%s' is not public.",
                                methodName, className));
            }

            String functionName = null;
            if (funqMethod.value() != null) {
                functionName = funqMethod.value().asString();
            }
            if (functionName != null && functionName.isEmpty())
                functionName = null;
            functions.produce(new FunctionBuildItem(className, methodName, method.descriptor(), functionName));

            String source = FunctionScannerBuildStep.class.getSimpleName() + " > " + method.declaringClass() + "[" + method
                    + "]";

            Type returnType = method.returnType();
            if (returnType.kind() != Type.Kind.VOID) {
                reflectiveHierarchy.produce(ReflectiveHierarchyBuildItem
                        .builder(returnType)
                        .index(index)
                        .ignoreTypePredicate(IGNORE_TYPE_FOR_REFLECTION_PREDICATE)
                        .ignoreFieldPredicate(IGNORE_FIELD_FOR_REFLECTION_PREDICATE)
                        .ignoreMethodPredicate(IGNORE_METHOD_FOR_REFLECTION_PREDICATE)
                        .source(source)
                        .build());
            }
            for (short i = 0; i < method.parametersCount(); i++) {
                Type parameterType = method.parameterType(i);
                if (!hasAnnotation(method, i, CONTEXT)) {
                    reflectiveHierarchy.produce(ReflectiveHierarchyBuildItem
                            .builder(parameterType)
                            .index(index)
                            .ignoreTypePredicate(IGNORE_TYPE_FOR_REFLECTION_PREDICATE)
                            .ignoreFieldPredicate(IGNORE_FIELD_FOR_REFLECTION_PREDICATE)
                            .ignoreMethodPredicate(IGNORE_METHOD_FOR_REFLECTION_PREDICATE)
                            .source(source)
                            .build());
                }
            }
        }
        Set<ClassInfo> withoutDefaultCtor = new HashSet<>();
        for (ClassInfo clazz : classes) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(clazz.name().toString())
                    .reason(getClass().getName())
                    .methods().fields().build());
            if (!clazz.hasNoArgsConstructor()) {
                withoutDefaultCtor.add(clazz);
            }
        }
        unremovableBeans.produce(new UnremovableBeanBuildItem(b -> classNames.contains(b.getBeanClass().toString())));
        generateDefaultConstructors(transformers, withoutDefaultCtor);

        // we need to use an annotation transformer here instead of an AdditionalBeanBuildItem because
        // the use of the latter along with the BeanArchiveIndexBuildItem results in build cycles
        annotationsTransformer.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

            @Override
            public boolean appliesTo(AnnotationTarget.Kind kind) {
                return kind == AnnotationTarget.Kind.CLASS;
            }

            @Override
            public void transform(TransformationContext transformationContext) {
                ClassInfo clazz = transformationContext.getTarget().asClass();
                if (!classes.contains(clazz))
                    return;
                if (BuiltinScope.isDeclaredOn(clazz)) {
                    // nothing to do as the presence of a scope will automatically qualify the class as a bean
                    return;
                }
                Transformation transformation = transformationContext.transform();
                transformation.add(BuiltinScope.DEPENDENT.getName());
                if (clazz.declaredAnnotation(DotNames.TYPED) == null) {
                    // Add @Typed(MySubresource.class)
                    transformation.add(createTypedAnnotationInstance(clazz));
                }
                transformation.done();
            }
        }));
    }

    private static boolean hasAnnotation(MethodInfo method, short paramPosition, DotName annotation) {
        for (AnnotationInstance annotationInstance : method.annotations()) {
            AnnotationTarget target = annotationInstance.target();
            if (target != null && target.kind() == AnnotationTarget.Kind.METHOD_PARAMETER
                    && target.asMethodParameter().position() == paramPosition
                    && annotationInstance.name().equals(annotation)) {
                return true;
            }
        }
        return false;
    }

    @BuildStep
    @Record(STATIC_INIT)
    public FunctionInitializedBuildItem staticInit(FunctionRecorder recorder, List<FunctionBuildItem> functions,
            RecorderContext context) {
        if (functions == null || functions.isEmpty())
            return null;
        recorder.init();
        for (FunctionBuildItem function : functions) {
            if (function.getFunctionName() == null) {
                recorder.register(context.classProxy(function.getClassName()), function.getMethodName(),
                        function.getDescriptor());
            } else {
                recorder.register(context.classProxy(function.getClassName()), function.getMethodName(),
                        function.getDescriptor(), function.getFunctionName());
            }
        }
        return FunctionInitializedBuildItem.SINGLETON;
    }

    private static void generateDefaultConstructors(BuildProducer<BytecodeTransformerBuildItem> transformers,
            Set<ClassInfo> withoutDefaultCtor) {

        for (ClassInfo classInfo : withoutDefaultCtor) {
            // don't generate constructor for normal scoped beans as the Quarkus Arc does that for us
            final BuiltinScope scope = BuiltinScope.from(classInfo);
            if (scope != null && scope.getInfo().isNormal()) {
                continue;
            }

            // keep it super simple - only generate default constructor is the object is a direct descendant of Object
            if (!(classInfo.superClassType() != null && classInfo.superClassType().name().equals(DotNames.OBJECT))) {
                return;
            }

            final String name = classInfo.name().toString();
            transformers
                    .produce(new BytecodeTransformerBuildItem(name, new BiFunction<String, ClassVisitor, ClassVisitor>() {
                        @Override
                        public ClassVisitor apply(String className, ClassVisitor classVisitor) {
                            ClassVisitor cv = new ClassVisitor(Gizmo.ASM_API_VERSION, classVisitor) {

                                @Override
                                public void visit(int version, int access, String name, String signature, String superName,
                                        String[] interfaces) {
                                    super.visit(version, access, name, signature, superName, interfaces);
                                    MethodVisitor ctor = visitMethod(Modifier.PUBLIC | Opcodes.ACC_SYNTHETIC, "<init>", "()V",
                                            null,
                                            null);
                                    ctor.visitCode();
                                    ctor.visitVarInsn(Opcodes.ALOAD, 0);
                                    ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
                                    ctor.visitInsn(Opcodes.RETURN);
                                    ctor.visitMaxs(1, 1);
                                    ctor.visitEnd();
                                }
                            };
                            return cv;
                        }
                    }));
        }
    }

    private AnnotationInstance createTypedAnnotationInstance(ClassInfo clazz) {
        return AnnotationInstance.create(DotNames.TYPED, clazz,
                new AnnotationValue[] { AnnotationValue.createArrayValue("value",
                        new AnnotationValue[] { AnnotationValue.createClassValue("value",
                                Type.create(clazz.name(), org.jboss.jandex.Type.Kind.CLASS)) }) });
    }

}
