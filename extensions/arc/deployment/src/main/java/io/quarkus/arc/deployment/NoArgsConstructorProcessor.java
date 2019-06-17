package io.quarkus.arc.deployment;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiFunction;

import javax.enterprise.context.NormalScope;
import javax.inject.Inject;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.processor.BeanDeploymentValidator;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionTargetInfo;
import io.quarkus.arc.processor.InjectionTargetInfo.TargetKind;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;

public class NoArgsConstructorProcessor {

    private static final Logger LOGGER = Logger.getLogger(NoArgsConstructorProcessor.class);

    // Copied from java.lang.Class
    // https://github.com/wildfly/jandex/issues/60
    private static final int ANNOTATION = 0x00002000;

    @Inject
    BeanArchiveIndexBuildItem beanArchiveIndex;

    @Inject
    CombinedIndexBuildItem combinedIndex;

    @Inject
    BuildProducer<BytecodeTransformerBuildItem> transformers;

    @Inject
    BuildProducer<BeanDeploymentValidatorBuildItem> validators;

    @BuildStep
    public void addMissingConstructors() throws Exception {
        Set<ClassInfo> targetClasses = new HashSet<>();
        Set<DotName> normalScopes = initNormalScopes();

        for (DotName normalScope : normalScopes) {
            collectTargetClasses(targetClasses, normalScope);
        }
        for (Iterator<ClassInfo> iterator = targetClasses.iterator(); iterator.hasNext();) {
            ClassInfo targetClass = iterator.next();
            if (targetClass.hasNoArgsConstructor()) {
                // Skip all classes with no-args constructors
                iterator.remove();
            }
        }

        if (targetClasses.isEmpty()) {
            return;
        }

        Set<DotName> transformedClasses = new HashSet<>();
        for (ClassInfo targetClass : targetClasses) {
            String superClassName;
            if (DotNames.OBJECT.equals(targetClass.superName())) {
                // Bean class extends java.lang.Object
                superClassName = "java/lang/Object";
            } else {
                ClassInfo superClass = combinedIndex.getIndex().getClassByName(targetClass.superName());
                if (superClass != null && superClass.hasNoArgsConstructor()) {
                    // Bean class extends a class with no-args constructor
                    superClassName = superClass.name().toString().replace('.', '/');
                } else {
                    superClassName = null;
                }
            }

            if (superClassName != null) {
                transformedClasses.add(targetClass.name());
                LOGGER.debugf("Adding no-args constructor to %s", targetClass);
                transformers.produce(new BytecodeTransformerBuildItem(targetClass.name().toString(),
                        new BiFunction<String, ClassVisitor, ClassVisitor>() {
                            @Override
                            public ClassVisitor apply(String className, ClassVisitor classVisitor) {
                                ClassVisitor cv = new ClassVisitor(Opcodes.ASM6, classVisitor) {

                                    @Override
                                    public void visit(int version, int access, String name, String signature, String superName,
                                            String[] interfaces) {
                                        super.visit(version, access, name, signature, superName, interfaces);
                                        MethodVisitor mv = visitMethod(Modifier.PUBLIC, "<init>", "()V", null,
                                                null);
                                        mv.visitCode();
                                        mv.visitVarInsn(Opcodes.ALOAD, 0);
                                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superClassName, "<init>", "()V", false);
                                        // NOTE: it seems that we do not need to handle final fields?
                                        mv.visitInsn(Opcodes.RETURN);
                                        mv.visitMaxs(1, 1);
                                        mv.visitEnd();
                                    }
                                };
                                return cv;
                            }
                        }));
            }
        }

        if (!transformedClasses.isEmpty()) {
            // Skip validation for the transformed classes
            validators.produce(new BeanDeploymentValidatorBuildItem(new BeanDeploymentValidator() {

                @Override
                public boolean skipValidation(InjectionTargetInfo target, ValidationRule rule) {
                    return ValidationRule.NO_ARGS_CONSTRUCTOR.equals(rule) && target.kind() == TargetKind.BEAN
                            && transformedClasses.contains(target.asBean().getBeanClass());
                }
            }));
        }
    }

    private Set<DotName> initNormalScopes() {
        Set<DotName> normalScopes = new HashSet<>();
        normalScopes.add(BuiltinScope.APPLICATION.getName());
        normalScopes.add(BuiltinScope.REQUEST.getName());
        combinedIndex.getIndex().getAnnotations(DotName.createSimple(NormalScope.class.getName())).stream()
                .filter(NoArgsConstructorProcessor::isTargetAnnotation)
                .map(AnnotationInstance::name)
                .forEach(normalScopes::add);
        return normalScopes;
    }

    private void collectTargetClasses(Set<ClassInfo> targetClasses, DotName normalScope) {
        for (AnnotationInstance annotationInstance : beanArchiveIndex.getIndex()
                .getAnnotations(normalScope)) {
            if (annotationInstance.target().kind() == Kind.CLASS) {
                targetClasses.add(annotationInstance.target().asClass());
            }
        }
    }

    private static boolean isTargetAnnotation(AnnotationInstance annotationInstance) {
        return annotationInstance.target().kind() == Kind.CLASS
                && ((annotationInstance.target().asClass().flags() & ANNOTATION) != 0);
    }

}
