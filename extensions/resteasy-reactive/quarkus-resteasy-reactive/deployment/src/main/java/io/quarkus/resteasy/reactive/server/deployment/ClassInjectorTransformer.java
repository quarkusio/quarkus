package io.quarkus.resteasy.reactive.server.deployment;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Type.Kind;
import org.jboss.resteasy.reactive.common.processor.IndexedParameter;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.core.parameters.converters.DelegatingParameterConverterSupplier;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverterSupplier;
import org.jboss.resteasy.reactive.server.core.parameters.converters.RuntimeResolvedConverter;
import org.jboss.resteasy.reactive.server.injection.QuarkusRestInjectionContext;
import org.jboss.resteasy.reactive.server.injection.QuarkusRestInjectionTarget;
import org.jboss.resteasy.reactive.server.processor.ServerIndexedParameter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkus.deployment.util.AsmUtil;

public class ClassInjectorTransformer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    private static final String WEB_APPLICATION_EXCEPTION_BINARY_NAME = WebApplicationException.class.getName().replace('.',
            '/');
    private static final String NOT_FOUND_EXCEPTION_BINARY_NAME = NotFoundException.class.getName().replace('.', '/');
    private static final String BAD_REQUEST_EXCEPTION_BINARY_NAME = BadRequestException.class.getName().replace('.', '/');

    private static final String PARAMETER_CONVERTER_BINARY_NAME = ParameterConverter.class.getName()
            .replace('.', '/');
    private static final String PARAMETER_CONVERTER_DESCRIPTOR = "L" + PARAMETER_CONVERTER_BINARY_NAME + ";";

    private static final String QUARKUS_REST_INJECTION_TARGET_BINARY_NAME = QuarkusRestInjectionTarget.class.getName()
            .replace('.', '/');

    private static final String QUARKUS_REST_INJECTION_CONTEXT_BINARY_NAME = QuarkusRestInjectionContext.class.getName()
            .replace('.', '/');
    private static final String QUARKUS_REST_INJECTION_CONTEXT_DESCRIPTOR = "L" + QUARKUS_REST_INJECTION_CONTEXT_BINARY_NAME
            + ";";
    private static final String INJECT_METHOD_NAME = "__quarkus_rest_inject";
    private static final String INJECT_METHOD_DESCRIPTOR = "(" + QUARKUS_REST_INJECTION_CONTEXT_DESCRIPTOR + ")V";

    private static final String QUARKUS_REST_DEPLOYMENT_BINARY_NAME = Deployment.class.getName().replace('.', '/');
    private static final String QUARKUS_REST_DEPLOYMENT_DESCRIPTOR = "L" + QUARKUS_REST_DEPLOYMENT_BINARY_NAME + ";";

    static final String INIT_CONVERTER_METHOD_NAME = "__quarkus_init_converter__";
    private static final String INIT_CONVERTER_FIELD_NAME = "__quarkus_converter__";
    private static final String INIT_CONVERTER_METHOD_DESCRIPTOR = "(" + QUARKUS_REST_DEPLOYMENT_DESCRIPTOR + ")V";

    private final Map<FieldInfo, ServerIndexedParameter> fieldExtractors;
    private final boolean superTypeIsInjectable;

    public ClassInjectorTransformer(Map<FieldInfo, ServerIndexedParameter> fieldExtractors, boolean superTypeIsInjectable) {
        this.fieldExtractors = fieldExtractors;
        this.superTypeIsInjectable = superTypeIsInjectable;
    }

    @Override
    public ClassVisitor apply(String classname, ClassVisitor visitor) {
        return new ClassInjectorVisitor(Opcodes.ASM8, visitor, fieldExtractors, superTypeIsInjectable);
    }

    static class ClassInjectorVisitor extends ClassVisitor {

        private Map<FieldInfo, ServerIndexedParameter> fieldExtractors;
        private String thisName;
        private boolean superTypeIsInjectable;
        private String superTypeName;

        public ClassInjectorVisitor(int api, ClassVisitor classVisitor, Map<FieldInfo, ServerIndexedParameter> fieldExtractors,
                boolean superTypeIsInjectable) {
            super(api, classVisitor);
            this.fieldExtractors = fieldExtractors;
            this.superTypeIsInjectable = superTypeIsInjectable;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            // make the class public otherwise we can't call its static init converters
            access &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED);
            access |= Opcodes.ACC_PUBLIC;
            // if our supertype is already injectable we don't have to implement it again
            if (!superTypeIsInjectable) {
                String[] newInterfaces = new String[interfaces.length + 1];
                newInterfaces[0] = QUARKUS_REST_INJECTION_TARGET_BINARY_NAME;
                System.arraycopy(interfaces, 0, newInterfaces, 1, interfaces.length);
                super.visit(version, access, name, signature, superName, newInterfaces);
            } else {
                super.visit(version, access, name, signature, superName, interfaces);
            }
            superTypeName = superName;
            thisName = name;
        }

        @Override
        public void visitEnd() {
            // FIXME: handle setters
            // FIXME: handle multi fields
            MethodVisitor injectMethod = visitMethod(Opcodes.ACC_PUBLIC, INJECT_METHOD_NAME, INJECT_METHOD_DESCRIPTOR, null,
                    null);
            injectMethod.visitParameter("ctx", 0 /* modifiers */);
            injectMethod.visitCode();
            if (superTypeIsInjectable) {
                // this
                injectMethod.visitIntInsn(Opcodes.ALOAD, 0);
                // ctx param
                injectMethod.visitIntInsn(Opcodes.ALOAD, 1);
                // call inject on our bean param field
                injectMethod.visitMethodInsn(Opcodes.INVOKESPECIAL, superTypeName,
                        INJECT_METHOD_NAME,
                        INJECT_METHOD_DESCRIPTOR, false);
            }
            for (Entry<FieldInfo, ServerIndexedParameter> entry : fieldExtractors.entrySet()) {
                FieldInfo fieldInfo = entry.getKey();
                ServerIndexedParameter extractor = entry.getValue();
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
                    case BODY:
                        // spec says not supported
                        break;
                    case CONTEXT:
                        // already set by CDI
                        break;
                    case FORM:
                        injectParameterWithConverter(injectMethod, "getFormParameter", fieldInfo, extractor, true, true,
                                fieldInfo.hasAnnotation(ResteasyReactiveDotNames.ENCODED));
                        break;
                    case HEADER:
                        injectParameterWithConverter(injectMethod, "getHeader", fieldInfo, extractor, true, false, false);
                        break;
                    case MATRIX:
                        injectParameterWithConverter(injectMethod, "getMatrixParameter", fieldInfo, extractor, true, true,
                                fieldInfo.hasAnnotation(ResteasyReactiveDotNames.ENCODED));
                        break;
                    case COOKIE:
                        injectParameterWithConverter(injectMethod, "getCookieParameter", fieldInfo, extractor, false, false,
                                false);
                        break;
                    case PATH:
                        injectParameterWithConverter(injectMethod, "getPathParameter", fieldInfo, extractor, false, true,
                                fieldInfo.hasAnnotation(ResteasyReactiveDotNames.ENCODED));
                        break;
                    case QUERY:
                        injectParameterWithConverter(injectMethod, "getQueryParameter", fieldInfo, extractor, true, true,
                                fieldInfo.hasAnnotation(ResteasyReactiveDotNames.ENCODED));
                        break;
                    default:
                        break;

                }
            }
            injectMethod.visitInsn(Opcodes.RETURN);
            injectMethod.visitEnd();
            injectMethod.visitMaxs(0, 0);

            // now generate initialisers for every field converter
            for (Entry<FieldInfo, ServerIndexedParameter> entry : fieldExtractors.entrySet()) {
                FieldInfo fieldInfo = entry.getKey();
                ServerIndexedParameter extractor = entry.getValue();
                switch (extractor.getType()) {
                    case FORM:
                    case HEADER:
                    case MATRIX:
                    case COOKIE:
                    case PATH:
                    case QUERY:
                        ParameterConverterSupplier converter = extractor.getConverter();
                        // do we need converters?
                        if (converter != null) {
                            generateConverterInitMethod(fieldInfo, converter, extractor.isSingle());
                        }

                        break;
                }
            }

            super.visitEnd();
        }

        private void generateConverterInitMethod(FieldInfo fieldInfo, ParameterConverterSupplier converter, boolean single) {
            String converterFieldName = INIT_CONVERTER_FIELD_NAME + fieldInfo.name();
            FieldVisitor field = visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, converterFieldName,
                    PARAMETER_CONVERTER_DESCRIPTOR, null, null);
            field.visitEnd();

            // get rid of runtime delegates since we always delegate to deployment to find one
            // can be a List/Set/Sorted set delegator -> RuntimeDelegating -> generated converter|null
            // can be a RuntimeDelegating -> generated converter
            converter = removeRuntimeResolvedConverterDelegate(converter);

            String delegateBinaryName = null;
            if (converter instanceof DelegatingParameterConverterSupplier) {
                ParameterConverterSupplier delegate = removeRuntimeResolvedConverterDelegate(
                        ((DelegatingParameterConverterSupplier) converter).getDelegate());
                if (delegate != null)
                    delegateBinaryName = delegate.getClassName().replace('.', '/');
            } else {
                delegateBinaryName = converter.getClassName().replace('.', '/');
            }

            MethodVisitor initConverterMethod = visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                    INIT_CONVERTER_METHOD_NAME + fieldInfo.name(),
                    INIT_CONVERTER_METHOD_DESCRIPTOR, null,
                    null);
            initConverterMethod.visitParameter("deployment", 0 /* modifiers */);
            initConverterMethod.visitCode();
            // deployment param
            initConverterMethod.visitIntInsn(Opcodes.ALOAD, 0);
            // this class
            initConverterMethod.visitLdcInsn(Type.getType("L" + thisName + ";"));
            // param name
            initConverterMethod.visitLdcInsn(fieldInfo.name());
            // single
            initConverterMethod.visitLdcInsn(single);
            initConverterMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, QUARKUS_REST_DEPLOYMENT_BINARY_NAME,
                    "getRuntimeParamConverter",
                    "(Ljava/lang/Class;Ljava/lang/String;Z)" + PARAMETER_CONVERTER_DESCRIPTOR, false);

            // now if we have a backup delegate, let's call it

            // stack: [converter]
            if (delegateBinaryName != null) {
                // check if we have a delegate
                Label notNull = new Label();
                initConverterMethod.visitInsn(Opcodes.DUP);
                // stack: [converter, converter]
                // if we got a converter, skip this
                initConverterMethod.visitJumpInsn(Opcodes.IFNONNULL, notNull);
                // stack: [converter]
                initConverterMethod.visitInsn(Opcodes.POP);
                // stack: []
                // let's instantiate our delegate
                initConverterMethod.visitTypeInsn(Opcodes.NEW, delegateBinaryName);
                // stack: [converter]
                initConverterMethod.visitInsn(Opcodes.DUP);
                // stack: [converter, converter]
                initConverterMethod.visitMethodInsn(Opcodes.INVOKESPECIAL, delegateBinaryName, "<init>",
                        "()V", false);
                // stack: [converter]
                // If we don't cast this to ParameterConverter, ASM in computeFrames will call getCommonSuperType
                // and try to load our generated class before we can load it, so we insert this cast to avoid that
                initConverterMethod.visitTypeInsn(Opcodes.CHECKCAST, PARAMETER_CONVERTER_BINARY_NAME);
                // end default delegate
                initConverterMethod.visitLabel(notNull);
            }

            // FIXME: throw if we don't have a converter

            // we have our element converter, see if we need to use list/set/sortedset converter around it

            if (converter instanceof DelegatingParameterConverterSupplier) {
                // stack: [converter]
                // let's instantiate our composite delegator
                String delegatorBinaryName = converter.getClassName().replace('.', '/');
                initConverterMethod.visitTypeInsn(Opcodes.NEW, delegatorBinaryName);
                // stack: [converter, instance]
                initConverterMethod.visitInsn(Opcodes.DUP_X1);
                // [instance, converter, instance]
                initConverterMethod.visitInsn(Opcodes.SWAP);
                // [instance, instance, converter]
                initConverterMethod.visitMethodInsn(Opcodes.INVOKESPECIAL, delegatorBinaryName, "<init>",
                        "(" + PARAMETER_CONVERTER_DESCRIPTOR + ")V", false);
            }

            // store the converter in the static field
            initConverterMethod.visitFieldInsn(Opcodes.PUTSTATIC, thisName, converterFieldName, PARAMETER_CONVERTER_DESCRIPTOR);

            initConverterMethod.visitInsn(Opcodes.RETURN);
            initConverterMethod.visitEnd();
            initConverterMethod.visitMaxs(0, 0);
        }

        private ParameterConverterSupplier removeRuntimeResolvedConverterDelegate(ParameterConverterSupplier converter) {
            if (converter instanceof RuntimeResolvedConverter.Supplier) {
                return ((RuntimeResolvedConverter.Supplier) converter).getDelegate();
            }
            return converter;
        }

        private void injectParameterWithConverter(MethodVisitor injectMethod, String methodName, FieldInfo fieldInfo,
                ServerIndexedParameter extractor, boolean extraSingleParameter, boolean extraEncodedParam, boolean encoded) {
            // spec says: 
            /*
             * 3.2 Fields and Bean Properties
             * if the field or property is annotated with @MatrixParam, @QueryParam or @PathParam then an implementation
             * MUST generate an instance of NotFoundException (404 status) that wraps the thrown exception and no
             * entity; if the field or property is annotated with @HeaderParam or @CookieParam then an implementation
             * MUST generate an instance of BadRequestException (400 status) that wraps the thrown exception and
             * no entity.
             * 3.3.2 Parameters
             * Exceptions thrown during construction of @FormParam annotated parameter values are treated the same as if
             * the parameter were annotated with @HeaderParam.
             */
            Label tryStart, tryEnd = null, tryWebAppHandler = null, tryHandler = null;
            switch (extractor.getType()) {
                case MATRIX:
                case QUERY:
                case PATH:
                case HEADER:
                case COOKIE:
                case FORM:
                    tryStart = new Label();
                    tryEnd = new Label();
                    tryWebAppHandler = new Label();
                    tryHandler = new Label();
                    injectMethod.visitTryCatchBlock(tryStart, tryEnd, tryWebAppHandler, WEB_APPLICATION_EXCEPTION_BINARY_NAME);
                    injectMethod.visitTryCatchBlock(tryStart, tryEnd, tryHandler, "java/lang/Throwable");
                    injectMethod.visitLabel(tryStart);
                    break;
            }
            // push the parameter value
            loadParameter(injectMethod, methodName, extractor, extraSingleParameter, extraEncodedParam, encoded);
            Label valueWasNull = new Label();
            // dup to test it
            injectMethod.visitInsn(Opcodes.DUP);
            injectMethod.visitJumpInsn(Opcodes.IFNULL, valueWasNull);
            convertParameter(injectMethod, extractor, fieldInfo);
            // inject this (for the put field) before the injected value
            injectMethod.visitIntInsn(Opcodes.ALOAD, 0);
            injectMethod.visitInsn(Opcodes.SWAP);
            if (fieldInfo.type().kind() == Kind.PRIMITIVE) {
                // this already does the right checkcast
                AsmUtil.unboxIfRequired(injectMethod, fieldInfo.type());
            } else {
                // FIXME: this is totally wrong wrt. generics
                injectMethod.visitTypeInsn(Opcodes.CHECKCAST, fieldInfo.type().name().toString('/'));
            }
            // store our param field
            injectMethod.visitFieldInsn(Opcodes.PUTFIELD, thisName, fieldInfo.name(),
                    AsmUtil.getDescriptor(fieldInfo.type(), name -> null));
            Label endLabel = new Label();
            injectMethod.visitJumpInsn(Opcodes.GOTO, endLabel);

            // if the value was null, we don't set it
            injectMethod.visitLabel(valueWasNull);
            // we have a null value for the object we wanted to inject on the stack
            injectMethod.visitInsn(Opcodes.POP);

            if (tryEnd != null) {
                // skip the catch block
                injectMethod.visitJumpInsn(Opcodes.GOTO, endLabel);
                injectMethod.visitLabel(tryEnd);
                // start the web app catch block
                injectMethod.visitLabel(tryWebAppHandler);
                // exception is on the stack, just throw it
                injectMethod.visitInsn(Opcodes.ATHROW);
                // start the catch block
                injectMethod.visitLabel(tryHandler);

                String exceptionBinaryName;
                switch (extractor.getType()) {
                    case MATRIX:
                    case QUERY:
                    case PATH:
                        exceptionBinaryName = NOT_FOUND_EXCEPTION_BINARY_NAME;
                        break;
                    case HEADER:
                    case COOKIE:
                    case FORM:
                        exceptionBinaryName = BAD_REQUEST_EXCEPTION_BINARY_NAME;
                        break;
                    default:
                        throw new IllegalStateException(
                                "Should not have been trying to catch exceptions for parameter of type " + extractor.getType());
                }
                // [x]
                injectMethod.visitTypeInsn(Opcodes.NEW, exceptionBinaryName);
                // [x, instance]
                injectMethod.visitInsn(Opcodes.DUP_X1);
                // [instance, x, instance]
                injectMethod.visitInsn(Opcodes.SWAP);
                // [instance, instance, x]
                injectMethod.visitMethodInsn(Opcodes.INVOKESPECIAL, exceptionBinaryName, "<init>", "(Ljava/lang/Throwable;)V",
                        false);
                injectMethod.visitInsn(Opcodes.ATHROW);
            }

            // really done
            injectMethod.visitLabel(endLabel);
        }

        private void convertParameter(MethodVisitor injectMethod, ServerIndexedParameter extractor, FieldInfo fieldInfo) {
            ParameterConverterSupplier converter = extractor.getConverter();
            if (converter != null) {
                // load our converter
                String converterFieldName = INIT_CONVERTER_FIELD_NAME + fieldInfo.name();
                injectMethod.visitFieldInsn(Opcodes.GETSTATIC, thisName, converterFieldName, PARAMETER_CONVERTER_DESCRIPTOR);
                // at this point we have [val, converter] and we need to reverse that order
                injectMethod.visitInsn(Opcodes.SWAP);
                // now call the convert method on the converter
                injectMethod.visitMethodInsn(Opcodes.INVOKEINTERFACE, PARAMETER_CONVERTER_BINARY_NAME, "convert",
                        "(Ljava/lang/Object;)Ljava/lang/Object;", true);
                // now we got ourselves a converted value
            }
        }

        private void loadParameter(MethodVisitor injectMethod, String methodName, IndexedParameter extractor,
                boolean extraSingleParameter, boolean extraEncodedParam, boolean encoded) {
            // ctx param
            injectMethod.visitIntInsn(Opcodes.ALOAD, 1);
            // name param
            injectMethod.visitLdcInsn(extractor.getName());
            String methodSignature;
            if (extraEncodedParam && extraSingleParameter) {
                injectMethod.visitLdcInsn(extractor.isSingle());
                injectMethod.visitLdcInsn(encoded);
                methodSignature = "(Ljava/lang/String;ZZ)Ljava/lang/Object;";
            } else if (extraEncodedParam) {
                injectMethod.visitLdcInsn(encoded);
                methodSignature = "(Ljava/lang/String;Z)Ljava/lang/String;";
            } else if (extraSingleParameter) {
                // single param
                injectMethod.visitLdcInsn(extractor.isSingle());
                methodSignature = "(Ljava/lang/String;Z)Ljava/lang/Object;";
            } else {
                methodSignature = "(Ljava/lang/String;)Ljava/lang/String;";
            }
            // call methodName on the ctx
            injectMethod.visitMethodInsn(Opcodes.INVOKEINTERFACE, QUARKUS_REST_INJECTION_CONTEXT_BINARY_NAME, methodName,
                    methodSignature, true);
            // deal with default value
            if (extractor.getDefaultValue() != null) {
                // dup to test it
                injectMethod.visitInsn(Opcodes.DUP);
                Label wasNonNullTarget = new Label();
                Label setDefaultValueTarget = new Label();
                injectMethod.visitJumpInsn(Opcodes.IFNULL, setDefaultValueTarget);
                // it's not null, do we allow empty values?
                // only if this supports multiple values
                if (extractor.isObtainedAsCollection()) {
                    // dup to test it
                    injectMethod.visitInsn(Opcodes.DUP);
                    // check if it's not an empty collection
                    injectMethod.visitTypeInsn(Opcodes.CHECKCAST, "java/util/Collection");
                    injectMethod.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Collection", "isEmpty",
                            "()Z", true);
                    injectMethod.visitJumpInsn(Opcodes.IFNE, setDefaultValueTarget);
                }
                injectMethod.visitJumpInsn(Opcodes.GOTO, wasNonNullTarget);
                injectMethod.visitLabel(setDefaultValueTarget);
                // it was null or empty, so let's eat the null value on the stack
                injectMethod.visitInsn(Opcodes.POP);
                // replace it with the default value
                injectMethod.visitLdcInsn(extractor.getDefaultValue());
                injectMethod.visitLabel(wasNonNullTarget);
            }
        }
    }
}
