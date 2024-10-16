package io.quarkus.qute.deployment;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.BiFunction;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.runtime.TemplateProducer;

class TemplateRecordEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    private final ClassInfo interfaceToImplement;
    private final String recordClassName;
    private final String templateId;
    private final String fragmentId;
    private final String canonicalConstructorDescriptor;
    private final List<MethodParameterInfo> parameters;
    private final CheckedTemplateAdapter adapter;

    TemplateRecordEnhancer(ClassInfo interfaceToImplement, ClassInfo recordClass, String templateId, String fragmentId,
            String canonicalConstructorDescriptor, List<MethodParameterInfo> parameters, CheckedTemplateAdapter adapter) {
        this.interfaceToImplement = interfaceToImplement;
        this.recordClassName = recordClass.name().toString();
        this.templateId = templateId;
        this.fragmentId = fragmentId;
        this.canonicalConstructorDescriptor = canonicalConstructorDescriptor;
        this.parameters = parameters;
        this.adapter = adapter;
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new TemplateRecordClassVisitor(outputClassVisitor);
    }

    class TemplateRecordClassVisitor extends ClassVisitor {

        public TemplateRecordClassVisitor(ClassVisitor outputClassVisitor) {
            super(Gizmo.ASM_API_VERSION, outputClassVisitor);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor ret = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (name.equals(MethodDescriptor.INIT) && descriptor.equals(canonicalConstructorDescriptor)) {
                return new TemplateRecordConstructorVisitor(ret);
            }
            return ret;
        }

        @Override
        public void visitEnd() {
            String interfaceName = interfaceToImplement.name().toString();

            // private final TemplateInstance qute$templateInstance;
            FieldDescriptor quteTemplateInstanceDescriptor = FieldDescriptor.of(recordClassName, "qute$templateInstance",
                    interfaceName);
            FieldVisitor quteTemplateInstance = super.visitField(ACC_PRIVATE | ACC_FINAL,
                    quteTemplateInstanceDescriptor.getName(),
                    quteTemplateInstanceDescriptor.getType(), null, null);
            quteTemplateInstance.visitEnd();

            for (MethodInfo method : interfaceToImplement.methods()) {
                if (method.isSynthetic() || method.isBridge()) {
                    continue;
                }
                MethodDescriptor descriptor = MethodDescriptor.of(method);
                String[] exceptions = method.exceptions().stream().map(e -> toInternalClassName(e.name().toString()))
                        .toArray(String[]::new);
                MethodVisitor visitor = super.visitMethod(Opcodes.ACC_PUBLIC, descriptor.getName(),
                        descriptor.getDescriptor(), null, exceptions);
                visitor.visitCode();
                readQuteTemplateInstance(visitor);
                int idx = 1;
                for (Type paramType : method.parameterTypes()) {
                    // Load arguments on the stack
                    visitor.visitVarInsn(AsmUtil.getLoadOpcode(paramType), idx++);
                }
                invokeInterface(visitor, descriptor);
                returnAndEnd(visitor, method.returnType());
            }

            super.visitEnd();
        }

        private void readQuteTemplateInstance(MethodVisitor methodVisitor) {
            FieldDescriptor quteTemplateInstanceDescriptor = FieldDescriptor.of(recordClassName, "qute$templateInstance",
                    interfaceToImplement.name().toString());
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
            methodVisitor.visitFieldInsn(Opcodes.GETFIELD, quteTemplateInstanceDescriptor.getDeclaringClass(),
                    quteTemplateInstanceDescriptor.getName(), quteTemplateInstanceDescriptor.getType());
        }

        private void returnAndEnd(MethodVisitor methodVisitor, Type returnType) {
            methodVisitor.visitInsn(AsmUtil.getReturnInstruction(returnType));
            methodVisitor.visitEnd();
            methodVisitor.visitMaxs(-1, -1);
        }

    }

    class TemplateRecordConstructorVisitor extends MethodVisitor {

        public TemplateRecordConstructorVisitor(MethodVisitor outputVisitor) {
            super(Gizmo.ASM_API_VERSION, outputVisitor);
        }

        @Override
        public void visitInsn(int opcode) {
            if (opcode == Opcodes.RETURN) {
                visitVarInsn(Opcodes.ALOAD, 0);

                MethodDescriptor arcContainer = MethodDescriptor.ofMethod(Arc.class, "container", ArcContainer.class);
                MethodDescriptor arcContainerInstance = MethodDescriptor.ofMethod(ArcContainer.class, "instance",
                        InstanceHandle.class, Class.class, Annotation[].class);
                MethodDescriptor instanceHandleGet = MethodDescriptor.ofMethod(InstanceHandle.class, "get", Object.class);
                MethodDescriptor templateProducerGetInjectableTemplate = MethodDescriptor.ofMethod(TemplateProducer.class,
                        "getInjectableTemplate", Template.class, String.class);
                MethodDescriptor templateInstance = MethodDescriptor.ofMethod(Template.class, "instance",
                        TemplateInstance.class);
                MethodDescriptor templateGetFragment = MethodDescriptor.ofMethod(Template.class, "getFragment",
                        Template.Fragment.class, String.class);
                MethodDescriptor templateInstanceData = MethodDescriptor.ofMethod(TemplateInstance.class, "data",
                        TemplateInstance.class, String.class, Object.class);

                // Template template = Arc.container().instance(TemplateProducer.class).get().getInjectableTemplate("HelloResource/typedTemplate");
                visitMethodInsn(Opcodes.INVOKESTATIC, arcContainer.getDeclaringClass(), arcContainer.getName(),
                        arcContainer.getDescriptor(),
                        false);
                visitLdcInsn(org.objectweb.asm.Type.getType(TemplateProducer.class));
                visitLdcInsn(0);
                visitTypeInsn(Opcodes.ANEWARRAY, toInternalClassName(Annotation.class));
                invokeInterface(this, arcContainerInstance);
                invokeInterface(this, instanceHandleGet);
                visitTypeInsn(Opcodes.CHECKCAST, toInternalClassName(TemplateProducer.class));
                visitLdcInsn(templateId);
                visitMethodInsn(Opcodes.INVOKEVIRTUAL, templateProducerGetInjectableTemplate.getDeclaringClass(),
                        templateProducerGetInjectableTemplate.getName(),
                        templateProducerGetInjectableTemplate.getDescriptor(), false);
                if (fragmentId != null) {
                    // template = template.getFragment(id);
                    visitLdcInsn(fragmentId);
                    invokeInterface(this, templateGetFragment);
                }
                // templateInstance = template.instance();
                invokeInterface(this, templateInstance);

                if (adapter != null) {
                    adapter.convertTemplateInstance(this);
                    templateInstanceData = MethodDescriptor.ofMethod(adapter.templateInstanceBinaryName(), "data",
                            adapter.templateInstanceBinaryName(), String.class, Object.class);
                }

                int slot = 1;
                for (int i = 0; i < parameters.size(); i++) {
                    // instance = instance.data("name", value);
                    Type paramType = parameters.get(i).type();
                    visitLdcInsn(parameters.get(i).name());
                    visitVarInsn(AsmUtil.getLoadOpcode(paramType), slot);
                    AsmUtil.boxIfRequired(this, paramType);
                    invokeInterface(this, templateInstanceData);
                    slot += AsmUtil.getParameterSize(paramType);
                }

                FieldDescriptor quteTemplateInstanceDescriptor = FieldDescriptor.of(recordClassName, "qute$templateInstance",
                        interfaceToImplement.name().toString());
                visitFieldInsn(Opcodes.PUTFIELD, quteTemplateInstanceDescriptor.getDeclaringClass(),
                        quteTemplateInstanceDescriptor.getName(), quteTemplateInstanceDescriptor.getType());
                super.visitInsn(Opcodes.RETURN);
            } else {
                super.visitInsn(opcode);
            }
        }

    }

    private static void invokeInterface(MethodVisitor methodVisitor, MethodDescriptor descriptor) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, descriptor.getDeclaringClass(), descriptor.getName(),
                descriptor.getDescriptor(), true);
    }

    private static String toInternalClassName(Class<?> clazz) {
        return DescriptorUtils.objectToInternalClassName(clazz);
    }

    private static String toInternalClassName(String clazzName) {
        return DescriptorUtils.objectToInternalClassName(clazzName);
    }
}
