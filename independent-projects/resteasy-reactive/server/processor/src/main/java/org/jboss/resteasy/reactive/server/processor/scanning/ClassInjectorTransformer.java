package org.jboss.resteasy.reactive.server.processor.scanning;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.PrimitiveType.Primitive;
import org.jboss.jandex.Type.Kind;
import org.jboss.resteasy.reactive.common.model.ParameterType;
import org.jboss.resteasy.reactive.common.processor.AsmUtil;
import org.jboss.resteasy.reactive.common.processor.IndexedParameter;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.TypeArgMapper;
import org.jboss.resteasy.reactive.common.util.DeploymentUtils;
import org.jboss.resteasy.reactive.common.util.types.TypeSignatureParser;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.multipart.DefaultFileUpload;
import org.jboss.resteasy.reactive.server.core.multipart.MultipartSupport;
import org.jboss.resteasy.reactive.server.core.parameters.MultipartFormParamExtractor;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ArrayConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.DelegatingParameterConverterSupplier;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverter;
import org.jboss.resteasy.reactive.server.core.parameters.converters.ParameterConverterSupplier;
import org.jboss.resteasy.reactive.server.core.parameters.converters.RuntimeResolvedConverter;
import org.jboss.resteasy.reactive.server.injection.ResteasyReactiveInjectionContext;
import org.jboss.resteasy.reactive.server.injection.ResteasyReactiveInjectionTarget;
import org.jboss.resteasy.reactive.server.processor.ServerIndexedParameter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkus.gizmo.Gizmo;

public class ClassInjectorTransformer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    private static final String WEB_APPLICATION_EXCEPTION_BINARY_NAME = WebApplicationException.class.getName().replace('.',
            '/');
    private static final String NOT_FOUND_EXCEPTION_BINARY_NAME = NotFoundException.class.getName().replace('.', '/');
    private static final String BAD_REQUEST_EXCEPTION_BINARY_NAME = BadRequestException.class.getName().replace('.', '/');

    private static final String PARAMETER_CONVERTER_BINARY_NAME = ParameterConverter.class.getName()
            .replace('.', '/');
    private static final String PARAMETER_CONVERTER_DESCRIPTOR = "L" + PARAMETER_CONVERTER_BINARY_NAME + ";";

    private static final String QUARKUS_REST_INJECTION_TARGET_BINARY_NAME = ResteasyReactiveInjectionTarget.class.getName()
            .replace('.', '/');

    private static final String QUARKUS_REST_INJECTION_CONTEXT_BINARY_NAME = ResteasyReactiveInjectionContext.class.getName()
            .replace('.', '/');
    private static final String QUARKUS_REST_INJECTION_CONTEXT_DESCRIPTOR = "L" + QUARKUS_REST_INJECTION_CONTEXT_BINARY_NAME
            + ";";
    private static final String INJECT_METHOD_NAME = "__quarkus_rest_inject";
    private static final String INJECT_METHOD_DESCRIPTOR = "(" + QUARKUS_REST_INJECTION_CONTEXT_DESCRIPTOR + ")V";

    private static final String QUARKUS_REST_DEPLOYMENT_BINARY_NAME = Deployment.class.getName().replace('.', '/');
    private static final String QUARKUS_REST_DEPLOYMENT_DESCRIPTOR = "L" + QUARKUS_REST_DEPLOYMENT_BINARY_NAME + ";";

    public static final String INIT_CONVERTER_METHOD_NAME = "__quarkus_init_converter__";
    private static final String INIT_CONVERTER_FIELD_NAME = "__quarkus_converter__";
    private static final String INIT_CONVERTER_METHOD_DESCRIPTOR = "(" + QUARKUS_REST_DEPLOYMENT_DESCRIPTOR + ")V";

    private static final String MULTIPART_SUPPORT_BINARY_NAME = MultipartSupport.class.getName().replace('.', '/');

    private static final String OBJECT_BINARY_NAME = Object.class.getName().replace('.', '/');
    private static final String OBJECT_DESCRIPTOR = "L" + OBJECT_BINARY_NAME + ";";

    private static final String STRING_BINARY_NAME = String.class.getName().replace('.', '/');
    private static final String STRING_DESCRIPTOR = "L" + STRING_BINARY_NAME + ";";

    private static final String BYTE_ARRAY_DESCRIPTOR = "[B";

    private static final String INPUT_STREAM_BINARY_NAME = InputStream.class.getName().replace('.', '/');
    private static final String INPUT_STREAM_DESCRIPTOR = "L" + INPUT_STREAM_BINARY_NAME + ";";

    private static final String LIST_BINARY_NAME = List.class.getName().replace('.', '/');
    private static final String LIST_DESCRIPTOR = "L" + LIST_BINARY_NAME + ";";

    private static final String TYPE_BINARY_NAME = java.lang.reflect.Type.class.getName().replace('.', '/');
    private static final String TYPE_DESCRIPTOR = "L" + TYPE_BINARY_NAME + ";";

    private static final String CLASS_BINARY_NAME = Class.class.getName().replace('.', '/');
    private static final String CLASS_DESCRIPTOR = "L" + CLASS_BINARY_NAME + ";";

    private static final String MEDIA_TYPE_BINARY_NAME = MediaType.class.getName().replace('.', '/');
    private static final String MEDIA_TYPE_DESCRIPTOR = "L" + MEDIA_TYPE_BINARY_NAME + ";";

    private static final String DEPLOYMENT_UTILS_BINARY_NAME = DeploymentUtils.class.getName().replace('.', '/');
    private static final String DEPLOYMENT_UTILS_DESCRIPTOR = "L" + DEPLOYMENT_UTILS_BINARY_NAME + ";";

    private static final String TYPE_DESCRIPTOR_PARSER_BINARY_NAME = TypeSignatureParser.class.getName().replace('.', '/');
    private static final String TYPE_DESCRIPTOR_PARSER_DESCRIPTOR = "L" + TYPE_DESCRIPTOR_PARSER_BINARY_NAME + ";";

    private static final String FILE_BINARY_NAME = File.class.getName().replace('.', '/');
    private static final String FILE_DESCRIPTOR = "L" + FILE_BINARY_NAME + ";";

    private static final String PATH_BINARY_NAME = Path.class.getName().replace('.', '/');
    private static final String PATH_DESCRIPTOR = "L" + PATH_BINARY_NAME + ";";

    private static final String DEFAULT_FILE_UPLOAD_BINARY_NAME = DefaultFileUpload.class.getName().replace('.', '/');
    private static final String DEFAULT_FILE_UPLOAD_DESCRIPTOR = "L" + DEFAULT_FILE_UPLOAD_BINARY_NAME + ";";

    private static final String FILE_UPLOAD_BINARY_NAME = FileUpload.class.getName().replace('.', '/');

    private static final String RESTEASY_REACTIVE_REQUEST_CONTEXT_BINARY_NAME = ResteasyReactiveRequestContext.class.getName()
            .replace('.', '/');
    private static final String RESTEASY_REACTIVE_REQUEST_CONTEXT_DESCRIPTOR = "L"
            + RESTEASY_REACTIVE_REQUEST_CONTEXT_BINARY_NAME + ";";

    private final Map<FieldInfo, ServerIndexedParameter> fieldExtractors;
    private final boolean superTypeIsInjectable;

    /**
     * If this is true then we will create a new bean param instance, rather than assuming it has been created for us
     */
    private final boolean requireCreateBeanParams;
    private IndexView indexView;

    public ClassInjectorTransformer(Map<FieldInfo, ServerIndexedParameter> fieldExtractors, boolean superTypeIsInjectable,
            boolean requireCreateBeanParams, IndexView indexView) {
        this.fieldExtractors = fieldExtractors;
        this.superTypeIsInjectable = superTypeIsInjectable;
        this.requireCreateBeanParams = requireCreateBeanParams;
        this.indexView = indexView;
    }

    @Override
    public ClassVisitor apply(String classname, ClassVisitor visitor) {
        return new ClassInjectorVisitor(Gizmo.ASM_API_VERSION, visitor,
                fieldExtractors, superTypeIsInjectable,
                requireCreateBeanParams, indexView);
    }

    static class ClassInjectorVisitor extends ClassVisitor {

        private Map<FieldInfo, ServerIndexedParameter> fieldExtractors;
        private Map<FieldInfo, ServerIndexedParameter> partTypes;
        private String thisName;
        private boolean superTypeIsInjectable;
        private String superTypeName;
        private final boolean requireCreateBeanParams;
        private IndexView indexView;
        private boolean seenClassInit;

        public ClassInjectorVisitor(int api, ClassVisitor classVisitor, Map<FieldInfo, ServerIndexedParameter> fieldExtractors,
                boolean superTypeIsInjectable, boolean requireCreateBeanParams, IndexView indexView) {
            super(api, classVisitor);
            this.fieldExtractors = fieldExtractors;
            this.superTypeIsInjectable = superTypeIsInjectable;
            this.requireCreateBeanParams = requireCreateBeanParams;
            this.indexView = indexView;
            this.partTypes = new HashMap<>();
            // collect part types
            for (Entry<FieldInfo, ServerIndexedParameter> entry : fieldExtractors.entrySet()) {
                FieldInfo fieldInfo = entry.getKey();
                ServerIndexedParameter extractor = entry.getValue();
                switch (extractor.getType()) {
                    case FORM:
                        MultipartFormParamExtractor.Type multipartFormType = getMultipartFormType(extractor);
                        if (multipartFormType == MultipartFormParamExtractor.Type.PartType) {
                            this.partTypes.put(fieldInfo, extractor);
                        }
                }
            }
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
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            // if we have part types and we're doing the class init method, add our init for their special fields
            if (!partTypes.isEmpty() && name.equals("<clinit>")) {
                this.seenClassInit = true;
                return new MethodVisitor(Gizmo.ASM_API_VERSION, mv) {
                    @Override
                    public void visitEnd() {
                        for (Entry<FieldInfo, ServerIndexedParameter> entry : partTypes.entrySet()) {
                            generateMultipartFormStaticInit(this, entry.getKey(), entry.getValue());
                        }
                        super.visitEnd();
                    }
                };
            }
            return mv;
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
                        String typeDescriptor = AsmUtil.getDescriptor(fieldInfo.type(), name -> null);
                        if (requireCreateBeanParams) {
                            String type = fieldInfo.type().name().toString().replace(".", "/");
                            injectMethod.visitTypeInsn(Opcodes.NEW, type);
                            injectMethod.visitInsn(Opcodes.DUP);
                            injectMethod.visitMethodInsn(Opcodes.INVOKESPECIAL, type, "<init>", "()V", false);
                            injectMethod.visitInsn(Opcodes.DUP_X1);
                            injectMethod.visitFieldInsn(Opcodes.PUTFIELD, thisName, fieldInfo.name(),
                                    typeDescriptor);
                        } else {
                            // our bean param field
                            injectMethod.visitFieldInsn(Opcodes.GETFIELD, thisName, fieldInfo.name(),
                                    typeDescriptor);
                        }
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
            // and type info for Form bodies
            for (Entry<FieldInfo, ServerIndexedParameter> entry : fieldExtractors.entrySet()) {
                FieldInfo fieldInfo = entry.getKey();
                ServerIndexedParameter extractor = entry.getValue();
                switch (extractor.getType()) {
                    case FORM:
                        MultipartFormParamExtractor.Type multipartFormType = getMultipartFormType(extractor);
                        if (multipartFormType == MultipartFormParamExtractor.Type.PartType) {
                            generateMultipartFormFields(fieldInfo, extractor);
                        }
                        // fall-through on purpose
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

            if (!seenClassInit && !partTypes.isEmpty()) {
                // add a class init method for the part types special fields
                MethodVisitor mv = super.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
                for (Entry<FieldInfo, ServerIndexedParameter> entry : partTypes.entrySet()) {
                    generateMultipartFormStaticInit(mv, entry.getKey(), entry.getValue());
                }
                mv.visitInsn(Opcodes.RETURN);
                mv.visitEnd();
                mv.visitMaxs(0, 0);
            }
            super.visitEnd();
        }

        private void generateMultipartFormFields(FieldInfo fieldInfo, ServerIndexedParameter extractor) {
            /*
             * private static Class map_type;
             * private static Type map_genericType;
             * private static MediaType map_mediaType;
             */
            super.visitField(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE, fieldInfo.name() + "_type", CLASS_DESCRIPTOR, null, null)
                    .visitEnd();
            super.visitField(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE, fieldInfo.name() + "_genericType", TYPE_DESCRIPTOR, null,
                    null).visitEnd();
            super.visitField(Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE, fieldInfo.name() + "_mediaType", MEDIA_TYPE_DESCRIPTOR,
                    null, null).visitEnd();
        }

        private void generateMultipartFormStaticInit(MethodVisitor mv, FieldInfo fieldInfo, ServerIndexedParameter extractor) {
            /*
             * generic:
             * map_type = DeploymentUtils.loadClass("java.util.Map");
             * map_genericType = TypeSignatureParser.parse("Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;");
             * map_mediaType = MediaType.valueOf("application/json");
             * dumb class:
             * Class var0 = DeploymentUtils.loadClass("org.acme.getting.started.Person");
             * person_type = var0;
             * person_genericType = var0;
             * person_mediaType = MediaType.valueOf("application/json");
             */
            org.jboss.jandex.Type type = fieldInfo.type();
            // extract the component type if not single
            if (!extractor.isSingle()) {
                boolean isArray = type.kind() == org.jboss.jandex.Type.Kind.ARRAY;
                // it's T[] or List<T>
                type = isArray ? type.asArrayType().component()
                        : type.asParameterizedType().arguments().get(0);
            }
            // type
            mv.visitLdcInsn(type.name().toString());
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, DEPLOYMENT_UTILS_BINARY_NAME, "loadClass",
                    "(" + STRING_DESCRIPTOR + ")" + CLASS_DESCRIPTOR, false);
            mv.visitFieldInsn(Opcodes.PUTSTATIC, this.thisName, fieldInfo.name() + "_type", CLASS_DESCRIPTOR);
            // generic type
            if ((type.kind() != org.jboss.jandex.Type.Kind.CLASS) && (type.kind() != org.jboss.jandex.Type.Kind.PRIMITIVE)) {
                TypeArgMapper typeArgMapper = new TypeArgMapper(fieldInfo.declaringClass(), indexView);
                String signature = AsmUtil.getSignature(type, typeArgMapper);
                mv.visitLdcInsn(signature);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, TYPE_DESCRIPTOR_PARSER_BINARY_NAME, "parse",
                        "(" + STRING_DESCRIPTOR + ")" + TYPE_DESCRIPTOR, false);
                mv.visitFieldInsn(Opcodes.PUTSTATIC, this.thisName, fieldInfo.name() + "_genericType", TYPE_DESCRIPTOR);
            } else {
                mv.visitFieldInsn(Opcodes.GETSTATIC, this.thisName, fieldInfo.name() + "_type", CLASS_DESCRIPTOR);
                mv.visitFieldInsn(Opcodes.PUTSTATIC, this.thisName, fieldInfo.name() + "_genericType", TYPE_DESCRIPTOR);
            }
            // media type
            // this must not be null, otherwise it's not a part type
            String mediaType = extractor.getAnns().get(ResteasyReactiveDotNames.PART_TYPE_NAME).value().asString();
            mv.visitLdcInsn(mediaType);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, MEDIA_TYPE_BINARY_NAME, "valueOf",
                    "(" + STRING_DESCRIPTOR + ")" + MEDIA_TYPE_DESCRIPTOR, false);
            mv.visitFieldInsn(Opcodes.PUTSTATIC, this.thisName, fieldInfo.name() + "_mediaType", MEDIA_TYPE_DESCRIPTOR);
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
                // array converter wants the array instance type
                if (converter instanceof ArrayConverter.ArraySupplier) {
                    org.jboss.jandex.Type componentType = fieldInfo.type().asArrayType().component();
                    initConverterMethod.visitLdcInsn(componentType.name().toString('.'));
                    // [instance, instance, converter, componentType]
                    initConverterMethod.visitMethodInsn(Opcodes.INVOKESPECIAL, delegatorBinaryName, "<init>",
                            "(" + PARAMETER_CONVERTER_DESCRIPTOR + STRING_DESCRIPTOR + ")V", false);
                } else {
                    initConverterMethod.visitMethodInsn(Opcodes.INVOKESPECIAL, delegatorBinaryName, "<init>",
                            "(" + PARAMETER_CONVERTER_DESCRIPTOR + ")V", false);
                }
            }

            // store the converter in the static field
            initConverterMethod.visitFieldInsn(Opcodes.PUTSTATIC, thisName, converterFieldName, PARAMETER_CONVERTER_DESCRIPTOR);

            initConverterMethod.visitInsn(Opcodes.RETURN);
            initConverterMethod.visitEnd();
            initConverterMethod.visitMaxs(0, 0);
        }

        private ParameterConverterSupplier removeRuntimeResolvedConverterDelegate(ParameterConverterSupplier converter) {
            if (converter instanceof RuntimeResolvedConverter.Supplier) {
                ParameterConverterSupplier delegate = ((RuntimeResolvedConverter.Supplier) converter).getDelegate();
                if (delegate != null) {
                    return delegate;
                }
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
            MultipartFormParamExtractor.Type multipartType = getMultipartFormType(extractor);
            if (multipartType == null) {
                loadParameter(injectMethod, methodName, extractor, extraSingleParameter, extraEncodedParam, encoded);
            } else {
                loadMultipartParameter(injectMethod, fieldInfo, extractor, multipartType);
            }
            Label valueWasNull = null;
            if (!extractor.isOptional()) {
                valueWasNull = new Label();
                // dup to test it
                injectMethod.visitInsn(Opcodes.DUP);
                injectMethod.visitJumpInsn(Opcodes.IFNULL, valueWasNull);
            }
            convertParameter(injectMethod, extractor, fieldInfo);
            // inject this (for the put field) before the injected value
            injectMethod.visitIntInsn(Opcodes.ALOAD, 0);
            injectMethod.visitInsn(Opcodes.SWAP);
            if (fieldInfo.type().kind() == Kind.PRIMITIVE) {
                // this already does the right checkcast
                AsmUtil.unboxIfRequired(injectMethod, fieldInfo.type());
            } else {
                // FIXME: this is totally wrong wrt. generics
                // Do not replace this with toString('/') because it doesn't use the given separator for array types
                injectMethod.visitTypeInsn(Opcodes.CHECKCAST, fieldInfo.type().name().toString().replace('.', '/'));
            }
            // store our param field
            injectMethod.visitFieldInsn(Opcodes.PUTFIELD, thisName, fieldInfo.name(),
                    AsmUtil.getDescriptor(fieldInfo.type(), name -> null));
            Label endLabel = new Label();
            injectMethod.visitJumpInsn(Opcodes.GOTO, endLabel);

            if (valueWasNull != null) {
                // if the value was null, we don't set it
                injectMethod.visitLabel(valueWasNull);
            }
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

        private void loadMultipartParameter(MethodVisitor injectMethod, FieldInfo fieldInfo, ServerIndexedParameter param,
                MultipartFormParamExtractor.Type multipartType) {
            switch (multipartType) {
                case String:
                    /*
                     * return single ? MultipartSupport.getString(name, context)
                     * : MultipartSupport.getStrings(name, context);
                     */
                    invokeMultipartSupport(param, injectMethod, "getString", STRING_DESCRIPTOR);
                    break;
                case ByteArray:
                    /*
                     * return single ? MultipartSupport.getByteArray(name, context)
                     * : MultipartSupport.getByteArrays(name, context);
                     */
                    invokeMultipartSupport(param, injectMethod, "getByteArray", BYTE_ARRAY_DESCRIPTOR);
                    break;
                case InputStream:
                    /*
                     * return single ? MultipartSupport.getInputStream(name, context)
                     * : MultipartSupport.getInputStreams(name, context);
                     */
                    invokeMultipartSupport(param, injectMethod, "getInputStream", INPUT_STREAM_DESCRIPTOR);
                    break;
                case FileUpload:
                    /*
                     * // special case
                     * if (name.equals(FileUpload.ALL))
                     * return MultipartSupport.getFileUploads(context);
                     * return single ? MultipartSupport.getFileUpload(name, context)
                     * : MultipartSupport.getFileUploads(name, context);
                     */
                    if (param.getName().equals(FileUpload.ALL)) {
                        // ctx param
                        injectMethod.visitIntInsn(Opcodes.ALOAD, 1);
                        injectMethod.visitTypeInsn(Opcodes.CHECKCAST, RESTEASY_REACTIVE_REQUEST_CONTEXT_BINARY_NAME);
                        injectMethod.visitMethodInsn(Opcodes.INVOKESTATIC, MULTIPART_SUPPORT_BINARY_NAME, "getFileUploads",
                                "(" + RESTEASY_REACTIVE_REQUEST_CONTEXT_DESCRIPTOR + ")" + LIST_DESCRIPTOR, false);
                    } else {
                        invokeMultipartSupport(param, injectMethod, "getFileUpload", DEFAULT_FILE_UPLOAD_DESCRIPTOR);
                    }
                    break;
                case File:
                    /*
                     * if (single) {
                     * FileUpload upload = MultipartSupport.getFileUpload(name, context);
                     * return upload != null ? upload.uploadedFile().toFile() : null;
                     * } else {
                     * return MultipartSupport.getJavaIOFileUploads(name, context);
                     * }
                     */
                    if (param.isSingle()) {
                        // name param
                        injectMethod.visitLdcInsn(param.getName());
                        // ctx param
                        injectMethod.visitIntInsn(Opcodes.ALOAD, 1);
                        injectMethod.visitTypeInsn(Opcodes.CHECKCAST, RESTEASY_REACTIVE_REQUEST_CONTEXT_BINARY_NAME);
                        injectMethod.visitMethodInsn(Opcodes.INVOKESTATIC, MULTIPART_SUPPORT_BINARY_NAME, "getFileUpload",
                                "(" + STRING_DESCRIPTOR + RESTEASY_REACTIVE_REQUEST_CONTEXT_DESCRIPTOR + ")"
                                        + DEFAULT_FILE_UPLOAD_DESCRIPTOR,
                                false);
                        Label ifNull = new Label();
                        Label endIf = new Label();
                        // dup for the ifnull
                        injectMethod.visitInsn(Opcodes.DUP);
                        injectMethod.visitJumpInsn(Opcodes.IFNULL, ifNull);
                        // if not null
                        injectMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, DEFAULT_FILE_UPLOAD_BINARY_NAME, "uploadedFile",
                                "()" + PATH_DESCRIPTOR, false);
                        injectMethod.visitMethodInsn(Opcodes.INVOKEINTERFACE, PATH_BINARY_NAME, "toFile",
                                "()" + FILE_DESCRIPTOR, true);
                        injectMethod.visitJumpInsn(Opcodes.GOTO, endIf);
                        // else
                        injectMethod.visitLabel(ifNull);
                        // get rid of the null file upload
                        injectMethod.visitInsn(Opcodes.POP);
                        injectMethod.visitInsn(Opcodes.ACONST_NULL);
                        injectMethod.visitLabel(endIf);
                    } else {
                        // name param
                        injectMethod.visitLdcInsn(param.getName());
                        // ctx param
                        injectMethod.visitIntInsn(Opcodes.ALOAD, 1);
                        injectMethod.visitTypeInsn(Opcodes.CHECKCAST, RESTEASY_REACTIVE_REQUEST_CONTEXT_BINARY_NAME);
                        injectMethod.visitMethodInsn(Opcodes.INVOKESTATIC, MULTIPART_SUPPORT_BINARY_NAME,
                                "getJavaIOFileUploads",
                                "(" + STRING_DESCRIPTOR + RESTEASY_REACTIVE_REQUEST_CONTEXT_DESCRIPTOR + ")" + LIST_DESCRIPTOR,
                                false);
                    }
                    break;
                case Path:
                    /*
                     * if (single) {
                     * FileUpload upload = MultipartSupport.getFileUpload(name, context);
                     * return upload != null ? upload.uploadedFile() : null;
                     * } else {
                     * return MultipartSupport.getJavaPathFileUploads(name, context);
                     * }
                     */
                    if (param.isSingle()) {
                        // name param
                        injectMethod.visitLdcInsn(param.getName());
                        // ctx param
                        injectMethod.visitIntInsn(Opcodes.ALOAD, 1);
                        injectMethod.visitTypeInsn(Opcodes.CHECKCAST, RESTEASY_REACTIVE_REQUEST_CONTEXT_BINARY_NAME);
                        injectMethod.visitMethodInsn(Opcodes.INVOKESTATIC, MULTIPART_SUPPORT_BINARY_NAME, "getFileUpload",
                                "(" + STRING_DESCRIPTOR + RESTEASY_REACTIVE_REQUEST_CONTEXT_DESCRIPTOR + ")"
                                        + DEFAULT_FILE_UPLOAD_DESCRIPTOR,
                                false);
                        Label ifNull = new Label();
                        Label endIf = new Label();
                        injectMethod.visitInsn(Opcodes.DUP);
                        injectMethod.visitJumpInsn(Opcodes.IFNULL, ifNull);
                        // if not null
                        injectMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, DEFAULT_FILE_UPLOAD_BINARY_NAME, "uploadedFile",
                                "()" + PATH_DESCRIPTOR, false);
                        injectMethod.visitJumpInsn(Opcodes.GOTO, endIf);
                        // else
                        injectMethod.visitLabel(ifNull);
                        // get rid of the null file upload
                        injectMethod.visitInsn(Opcodes.POP);
                        injectMethod.visitInsn(Opcodes.ACONST_NULL);
                        injectMethod.visitLabel(endIf);
                    } else {
                        // name param
                        injectMethod.visitLdcInsn(param.getName());
                        // ctx param
                        injectMethod.visitIntInsn(Opcodes.ALOAD, 1);
                        injectMethod.visitTypeInsn(Opcodes.CHECKCAST, RESTEASY_REACTIVE_REQUEST_CONTEXT_BINARY_NAME);
                        injectMethod.visitMethodInsn(Opcodes.INVOKESTATIC, MULTIPART_SUPPORT_BINARY_NAME,
                                "getJavaPathFileUploads",
                                "(" + STRING_DESCRIPTOR + RESTEASY_REACTIVE_REQUEST_CONTEXT_DESCRIPTOR + ")" + LIST_DESCRIPTOR,
                                false);
                    }
                    break;
                case PartType:
                    /*
                     * if (single) {
                     * String param = (String) context.getFormParameter(name, true, false);
                     * return MultipartSupport.convertFormAttribute(param, typeClass, genericType, MediaType.valueOf(mimeType),
                     * context,
                     * name);
                     * } else {
                     * List<String> params = (List<String>) context.getFormParameter(name, false, false);
                     * return MultipartSupport.convertFormAttributes(params, typeClass, genericType,
                     * MediaType.valueOf(mimeType), context, name);
                     * }
                     */
                    // ctx param
                    injectMethod.visitIntInsn(Opcodes.ALOAD, 1);
                    // name
                    injectMethod.visitLdcInsn(param.getName());
                    // single
                    injectMethod.visitLdcInsn(param.isSingle());
                    // encoded
                    injectMethod.visitLdcInsn(false);
                    injectMethod.visitMethodInsn(Opcodes.INVOKEINTERFACE, QUARKUS_REST_INJECTION_CONTEXT_BINARY_NAME,
                            "getFormParameter",
                            "(Ljava/lang/String;ZZ)Ljava/lang/Object;", true);
                    injectMethod.visitTypeInsn(Opcodes.CHECKCAST, param.isSingle() ? STRING_BINARY_NAME : LIST_BINARY_NAME);
                    // class, generic type, media type, context, name
                    injectMethod.visitFieldInsn(Opcodes.GETSTATIC, this.thisName, fieldInfo.name() + "_type", CLASS_DESCRIPTOR);
                    injectMethod.visitFieldInsn(Opcodes.GETSTATIC, this.thisName, fieldInfo.name() + "_genericType",
                            TYPE_DESCRIPTOR);
                    injectMethod.visitFieldInsn(Opcodes.GETSTATIC, this.thisName, fieldInfo.name() + "_mediaType",
                            MEDIA_TYPE_DESCRIPTOR);
                    injectMethod.visitIntInsn(Opcodes.ALOAD, 1);
                    injectMethod.visitTypeInsn(Opcodes.CHECKCAST, RESTEASY_REACTIVE_REQUEST_CONTEXT_BINARY_NAME);
                    injectMethod.visitLdcInsn(param.getName());
                    String firstParamDescriptor;
                    String returnDescriptor;
                    String methodName;
                    if (param.isSingle()) {
                        firstParamDescriptor = STRING_DESCRIPTOR;
                        returnDescriptor = OBJECT_DESCRIPTOR;
                        methodName = "convertFormAttribute";
                    } else {
                        firstParamDescriptor = LIST_DESCRIPTOR;
                        returnDescriptor = LIST_DESCRIPTOR;
                        methodName = "convertFormAttributes";
                    }
                    injectMethod.visitMethodInsn(Opcodes.INVOKESTATIC, MULTIPART_SUPPORT_BINARY_NAME, methodName,
                            "(" + firstParamDescriptor + CLASS_DESCRIPTOR + TYPE_DESCRIPTOR + MEDIA_TYPE_DESCRIPTOR
                                    + RESTEASY_REACTIVE_REQUEST_CONTEXT_DESCRIPTOR + STRING_DESCRIPTOR + ")" + returnDescriptor,
                            false);
                    break;
                default:
                    throw new RuntimeException("Unknown multipart type: " + multipartType);
            }
        }

        private void invokeMultipartSupport(ServerIndexedParameter param, MethodVisitor injectMethod,
                String singleOperationName, String singleOperationReturnDescriptor) {
            if (param.isSingle()) {
                // name param
                injectMethod.visitLdcInsn(param.getName());
                // ctx param
                injectMethod.visitIntInsn(Opcodes.ALOAD, 1);
                injectMethod.visitTypeInsn(Opcodes.CHECKCAST, RESTEASY_REACTIVE_REQUEST_CONTEXT_BINARY_NAME);
                injectMethod.visitMethodInsn(Opcodes.INVOKESTATIC, MULTIPART_SUPPORT_BINARY_NAME, singleOperationName,
                        "(" + STRING_DESCRIPTOR + RESTEASY_REACTIVE_REQUEST_CONTEXT_DESCRIPTOR + ")"
                                + singleOperationReturnDescriptor,
                        false);

            } else {
                // name param
                injectMethod.visitLdcInsn(param.getName());
                // ctx param
                injectMethod.visitIntInsn(Opcodes.ALOAD, 1);
                injectMethod.visitTypeInsn(Opcodes.CHECKCAST, RESTEASY_REACTIVE_REQUEST_CONTEXT_BINARY_NAME);
                injectMethod.visitMethodInsn(Opcodes.INVOKESTATIC, MULTIPART_SUPPORT_BINARY_NAME, singleOperationName + "s",
                        "(" + STRING_DESCRIPTOR + RESTEASY_REACTIVE_REQUEST_CONTEXT_DESCRIPTOR + ")" + LIST_DESCRIPTOR,
                        false);
            }
        }

        private MultipartFormParamExtractor.Type getMultipartFormType(ServerIndexedParameter param) {
            if (param.getType() != ParameterType.FORM) {
                // not a multipart type
                return null;
            }
            AnnotationInstance partType = param.getAnns().get(ResteasyReactiveDotNames.PART_TYPE_NAME);
            String mimeType = null;
            if (partType != null && partType.value() != null) {
                mimeType = partType.value().asString();
                // remove what ends up being the default
                if (MediaType.TEXT_PLAIN.equals(mimeType)) {
                    mimeType = null;
                }
            }
            // FileUpload/File/Path have more importance than part type
            if (param.getElementType().equals(FileUpload.class.getName())) {
                return MultipartFormParamExtractor.Type.FileUpload;
            } else if (param.getElementType().equals(File.class.getName())) {
                return MultipartFormParamExtractor.Type.File;
            } else if (param.getElementType().equals(Path.class.getName())) {
                return MultipartFormParamExtractor.Type.Path;
            } else if (param.getElementType().equals(String.class.getName())) {
                return MultipartFormParamExtractor.Type.String;
            } else if (param.getElementType().equals(InputStream.class.getName())) {
                return MultipartFormParamExtractor.Type.InputStream;
            } else if (param.getParamType().kind() == Kind.ARRAY
                    && param.getParamType().asArrayType().component().kind() == Kind.PRIMITIVE
                    && param.getParamType().asArrayType().component().asPrimitiveType().primitive() == Primitive.BYTE) {
                return MultipartFormParamExtractor.Type.ByteArray;
            } else if (mimeType != null && !mimeType.equals(MediaType.TEXT_PLAIN)) {
                return MultipartFormParamExtractor.Type.PartType;
            } else {
                // not a multipart type
                return null;
            }
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
