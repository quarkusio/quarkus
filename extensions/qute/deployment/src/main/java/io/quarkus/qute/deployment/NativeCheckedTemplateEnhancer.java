package io.quarkus.qute.deployment;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.qute.runtime.TemplateProducer;

public class NativeCheckedTemplateEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    private static class NativeMethod {
        private final MethodInfo methodInfo;
        private final String templateId;
        private final List<String> parameterNames;
        private final CheckedTemplateAdapter adaptor;

        public NativeMethod(MethodInfo methodInfo, String templatePath, List<String> parameterNames,
                CheckedTemplateAdapter adaptor) {
            this.methodInfo = methodInfo;
            this.templateId = templatePath;
            this.parameterNames = parameterNames;
            this.adaptor = adaptor;
        }
    }

    private final Map<String, NativeMethod> methods = new HashMap<>();

    public void implement(MethodInfo methodInfo, String templatePath, List<String> parameterNames,
            CheckedTemplateAdapter adaptor) {
        // FIXME: this should support overloading by using the method signature as key, but requires moving JandexUtil stuff around
        methods.put(methodInfo.name(), new NativeMethod(methodInfo, templatePath, parameterNames, adaptor));
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new DynamicTemplateClassVisitor(className, methods, outputClassVisitor);
    }

    public static class DynamicTemplateClassVisitor extends ClassVisitor {

        private final Map<String, NativeMethod> methods;

        public DynamicTemplateClassVisitor(String className, Map<String, NativeMethod> methods,
                ClassVisitor outputClassVisitor) {
            super(Gizmo.ASM_API_VERSION, outputClassVisitor);
            this.methods = methods;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            NativeMethod nativeMethod = methods.get(name);
            if (nativeMethod != null) {
                // remove the native bit
                access = access & ~Modifier.NATIVE;
            }
            MethodVisitor ret = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (nativeMethod != null) {
                return new NativeMethodVisitor(nativeMethod, ret);
            }
            return ret;
        }

        public static class NativeMethodVisitor extends MethodVisitor {

            private NativeMethod nativeMethod;

            public NativeMethodVisitor(NativeMethod nativeMethod, MethodVisitor outputVisitor) {
                super(Gizmo.ASM_API_VERSION, outputVisitor);
                this.nativeMethod = nativeMethod;
            }

            @Override
            public void visitEnd() {
                visitCode();
                /*
                 * Template template =
                 * Arc.container().instance(TemplateProducer.class).get().getInjectableTemplate("HelloResource/typedTemplate");
                 */
                visitMethodInsn(Opcodes.INVOKESTATIC, "io/quarkus/arc/Arc", "container", "()Lio/quarkus/arc/ArcContainer;",
                        false);
                visitLdcInsn(org.objectweb.asm.Type.getType(TemplateProducer.class));
                visitLdcInsn(0);
                visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/annotation/Annotation");
                visitMethodInsn(Opcodes.INVOKEINTERFACE, "io/quarkus/arc/ArcContainer", "instance",
                        "(Ljava/lang/Class;[Ljava/lang/annotation/Annotation;)Lio/quarkus/arc/InstanceHandle;", true);
                visitMethodInsn(Opcodes.INVOKEINTERFACE, "io/quarkus/arc/InstanceHandle", "get",
                        "()Ljava/lang/Object;", true);
                visitTypeInsn(Opcodes.CHECKCAST, "io/quarkus/qute/runtime/TemplateProducer");
                visitLdcInsn(nativeMethod.templateId);
                visitMethodInsn(Opcodes.INVOKEVIRTUAL, "io/quarkus/qute/runtime/TemplateProducer", "getInjectableTemplate",
                        "(Ljava/lang/String;)Lio/quarkus/qute/Template;", false);

                /*
                 * TemplateInstance instance = template.instance();
                 */
                // we store it on the stack because local vars are too much trouble
                visitMethodInsn(Opcodes.INVOKEINTERFACE, "io/quarkus/qute/Template", "instance",
                        "()Lio/quarkus/qute/TemplateInstance;", true);

                String templateInstanceBinaryName = "io/quarkus/qute/TemplateInstance";
                // some adaptors are required to return a different type such as MailTemplateInstance
                if (nativeMethod.adaptor != null) {
                    nativeMethod.adaptor.convertTemplateInstance(this);
                    templateInstanceBinaryName = nativeMethod.adaptor.templateInstanceBinaryName();
                }

                int slot = 0; // arg slots start at 0 for static methods
                List<Type> parameters = nativeMethod.methodInfo.parameters();
                for (int i = 0; i < nativeMethod.parameterNames.size(); i++) {
                    Type parameterType = parameters.get(i);
                    /*
                     * instance = instance.data("name", name);
                     */
                    visitLdcInsn(nativeMethod.parameterNames.get(i)); // first arg name
                    visitVarInsn(AsmUtil.getLoadOpcode(parameterType), slot); // slot-th arg value
                    AsmUtil.boxIfRequired(this, parameterType);

                    visitMethodInsn(Opcodes.INVOKEINTERFACE, templateInstanceBinaryName, "data",
                            "(Ljava/lang/String;Ljava/lang/Object;)L" + templateInstanceBinaryName + ";", true);

                    slot += AsmUtil.getParameterSize(parameterType);
                }
                /*
                 * return instance;
                 */
                visitInsn(Opcodes.ARETURN);

                visitMaxs(0, 0);
                super.visitEnd();
            }
        }
    }
}
