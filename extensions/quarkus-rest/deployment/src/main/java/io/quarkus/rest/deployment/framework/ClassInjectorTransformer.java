package io.quarkus.rest.deployment.framework;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import org.jboss.jandex.FieldInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.rest.deployment.framework.EndpointIndexer.ParameterExtractor;
import io.quarkus.rest.runtime.injection.QuarkusRestInjectionContext;
import io.quarkus.rest.runtime.injection.QuarkusRestInjectionTarget;

public class ClassInjectorTransformer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    private static final String QUARKUS_REST_INJECTION_TARGET_BINARY_NAME = QuarkusRestInjectionTarget.class.getName()
            .replace('.', '/');

    private static final String QUARKUS_REST_INJECTION_CONTEXT_BINARY_NAME = QuarkusRestInjectionContext.class.getName()
            .replace('.', '/');
    private static final String QUARKUS_REST_INJECTION_CONTEXT_DESCRIPTOR = "L" + QUARKUS_REST_INJECTION_CONTEXT_BINARY_NAME
            + ";";
    private static final String INJECT_METHOD_NAME = "__quarkus_rest_inject";
    private static final String INJECT_METHOD_DESCRIPTOR = "(" + QUARKUS_REST_INJECTION_CONTEXT_DESCRIPTOR + ")V";

    private final Map<FieldInfo, ParameterExtractor> fieldExtractors;

    public ClassInjectorTransformer(Map<FieldInfo, ParameterExtractor> fieldExtractors) {
        this.fieldExtractors = fieldExtractors;
    }

    @Override
    public ClassVisitor apply(String classname, ClassVisitor visitor) {
        return new ClassInjectorVisitor(Opcodes.ASM8, visitor, fieldExtractors);
    }

    static class ClassInjectorVisitor extends ClassVisitor {

        private Map<FieldInfo, ParameterExtractor> fieldExtractors;
        private String thisName;

        public ClassInjectorVisitor(int api, ClassVisitor classVisitor, Map<FieldInfo, ParameterExtractor> fieldExtractors) {
            super(api, classVisitor);
            this.fieldExtractors = fieldExtractors;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            String[] newInterfaces = new String[interfaces.length + 1];
            newInterfaces[0] = QUARKUS_REST_INJECTION_TARGET_BINARY_NAME;
            System.arraycopy(interfaces, 0, newInterfaces, 1, interfaces.length);
            super.visit(version, access, name, signature, superName, newInterfaces);
            thisName = name;
        }

        @Override
        public void visitEnd() {
            // FIXME: handle setters
            // FIXME: handle multi fields
            // FIXME: handle converters
            MethodVisitor injectMethod = visitMethod(Opcodes.ACC_PUBLIC, INJECT_METHOD_NAME, INJECT_METHOD_DESCRIPTOR, null,
                    null);
            injectMethod.visitParameter("ctx", 0 /* modifiers */);
            injectMethod.visitCode();
            for (Entry<FieldInfo, ParameterExtractor> entry : fieldExtractors.entrySet()) {
                FieldInfo fieldInfo = entry.getKey();
                ParameterExtractor extractor = entry.getValue();
                switch (extractor.getType()) {
                    case BEAN:
                        // this
                        injectMethod.visitIntInsn(Opcodes.ALOAD, 0);
                        // our bean param field
                        injectMethod.visitFieldInsn(Opcodes.GETFIELD, thisName, fieldInfo.name(),
                                AsmUtil.getDescriptor(fieldInfo.type(), name -> null));
                        // ctx param
                        injectMethod.visitIntInsn(Opcodes.ALOAD, 1);
                        // call inject on our bean param field
                        injectMethod.visitMethodInsn(Opcodes.INVOKEINTERFACE, QUARKUS_REST_INJECTION_TARGET_BINARY_NAME,
                                INJECT_METHOD_NAME,
                                INJECT_METHOD_DESCRIPTOR, true);
                        break;
                    case ASYNC_RESPONSE:
                        // FIXME
                        break;
                    case BODY:
                        // FIXME
                        break;
                    case CONTEXT:
                        // FIXME
                        break;
                    case FORM:
                        getParameter(injectMethod, "getFormParameter", fieldInfo, extractor);
                        break;
                    case HEADER:
                        getParameter(injectMethod, "getHeader", fieldInfo, extractor);
                        break;
                    case MATRIX:
                        getParameter(injectMethod, "getMatrixParameter", fieldInfo, extractor);
                        break;
                    case COOKIE:
                        getParameter(injectMethod, "getCookieParameter", fieldInfo, extractor);
                        break;
                    case PATH:
                        getParameter(injectMethod, "getPathParameter", fieldInfo, extractor);
                        break;
                    case QUERY:
                        getParameter(injectMethod, "getQueryParameter", fieldInfo, extractor);
                        break;
                    default:
                        break;

                }
            }
            injectMethod.visitInsn(Opcodes.RETURN);
            injectMethod.visitEnd();
            injectMethod.visitMaxs(0, 0);

            super.visitEnd();
        }

        private void getParameter(MethodVisitor injectMethod, String methodName, FieldInfo fieldInfo,
                ParameterExtractor extractor) {
            // this (for the put field)
            injectMethod.visitIntInsn(Opcodes.ALOAD, 0);
            // ctx param
            injectMethod.visitIntInsn(Opcodes.ALOAD, 1);
            // name param
            injectMethod.visitLdcInsn(extractor.getName());
            // call getQueryParameter on the ctx
            injectMethod.visitMethodInsn(Opcodes.INVOKEINTERFACE, QUARKUS_REST_INJECTION_CONTEXT_BINARY_NAME, methodName,
                    "(Ljava/lang/String;)Ljava/lang/String;", true);
            // store our param field
            injectMethod.visitFieldInsn(Opcodes.PUTFIELD, thisName, fieldInfo.name(),
                    AsmUtil.getDescriptor(fieldInfo.type(), name -> null));
        }
    }
}
