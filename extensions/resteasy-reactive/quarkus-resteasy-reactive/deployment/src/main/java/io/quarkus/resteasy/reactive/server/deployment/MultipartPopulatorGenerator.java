package io.quarkus.resteasy.reactive.server.deployment;

import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.*;

import java.io.File;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.processor.AsmUtil;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.common.processor.TypeArgMapper;
import org.jboss.resteasy.reactive.common.util.DeploymentUtils;
import org.jboss.resteasy.reactive.common.util.types.TypeSignatureParser;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.multipart.DefaultFileUpload;
import org.jboss.resteasy.reactive.server.injection.ResteasyReactiveInjectionContext;
import org.jboss.resteasy.reactive.server.spi.ServerHttpRequest;

import io.quarkus.deployment.bean.JavaBeanUtil;
import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.resteasy.reactive.server.runtime.multipart.MultipartSupport;

final class MultipartPopulatorGenerator {

    private static final Logger LOGGER = Logger.getLogger(MultipartPopulatorGenerator.class);

    private MultipartPopulatorGenerator() {
    }

    /**
     * Generates a class that populates a Pojo that is used as a @MultipartForm parameter in a resource method.
     * The generated class is called at runtime by the {@code __quarkus_rest_inject} method of the class
     * (which is added by {@link MultipartTransformer}).
     *
     * <p>
     * For example for a pojo like:
     *
     * <pre>
     * public class FormData {
     *
     *     &#64;RestForm
     *     &#64;PartType(MediaType.TEXT_PLAIN)
     *     private String foo;
     *
     *     &#64;RestForm
     *     &#64;PartType(MediaType.APPLICATION_JSON)
     *     public Map<String, String> map;
     *
     *     &#64;RestForm
     *     &#64;PartType(MediaType.APPLICATION_JSON)
     *     public Person person;
     *
     *     &#64;RestForm
     *     &#64;PartType(MediaType.TEXT_PLAIN)
     *     private Status status;
     *
     *     &#64;RestForm("htmlFile")
     *     private FileUpload htmlPart;
     *
     *     &#64;RestForm("htmlFile")
     *     public Path xmlPart;
     *
     *     &#64;RestForm
     *     public File txtFile;
     *
     *     public String getFoo() {
     *         return foo;
     *     }
     *
     *     public void setFoo(String foo) {
     *         this.foo = foo;
     *     }
     *
     *     public Status getStatus() {
     *         return status;
     *     }
     *
     *     public void setStatus(Status status) {
     *         this.status = status;
     *     }
     *
     *     public FileUpload getHtmlPart() {
     *         return htmlPart;
     *     }
     *
     *     public void setHtmlPart(FileUpload htmlPart) {
     *         this.htmlPart = htmlPart;
     *     }
     * }
     * </pre>
     *
     * <p>
     * the generated populator would look like:
     *
     * <pre>
     * public class FormData_generated_populator {
     *     private static Class map_type;
     *     private static Type map_genericType;
     *     private static MediaType map_mediaType;
     *     private static Class person_type;
     *     private static Type person_genericType;
     *     private static MediaType person_mediaType;
     *     private static Class status_type;
     *     private static Type status_genericType;
     *     private static MediaType status_mediaType;
     *
     *     static {
     *         map_type = DeploymentUtils.loadClass("java.util.Map");
     *         map_genericType = TypeSignatureParser.parse("Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;");
     *         map_mediaType = MediaType.valueOf("application/json");
     *         Class var0 = DeploymentUtils.loadClass("org.acme.getting.started.Person");
     *         person_type = var0;
     *         person_genericType = var0;
     *         person_mediaType = MediaType.valueOf("application/json");
     *         Class var1 = DeploymentUtils.loadClass("org.acme.getting.started.Status");
     *         status_type = var1;
     *         status_genericType = var1;
     *         status_mediaType = MediaType.valueOf("text/plain");
     *     }
     *
     *     public static void populate(FormData var0, ResteasyReactiveInjectionContext var1) {
     *         ResteasyReactiveRequestContext var3 = (ResteasyReactiveRequestContext) var1;
     *         ServerHttpRequest var5 = var3.serverRequest();
     *         String var2 = var5.getFormAttribute("foo");
     *         var0.setFoo((String) var2);
     *         QuarkusFileUpload var4 = MultipartSupport.getFileUpload("htmlFile", var3);
     *         var0.setHtmlPart((FileUpload) var4);
     *         String var6 = var5.getFormAttribute("map");
     *         Class var7 = map_type;
     *         Type var8 = map_genericType;
     *         MediaType var9 = map_mediaType;
     *         Object var10 = MultipartSupport.convertFormAttribute(var6, var7, var8, var9, var3);
     *         var0.map = (Map) var10;
     *         String var11 = var5.getFormAttribute("person");
     *         Class var12 = person_type;
     *         Type var13 = person_genericType;
     *         MediaType var14 = person_mediaType;
     *         Object var15 = MultipartSupport.convertFormAttribute(var11, var12, var13, var14, var3);
     *         var0.person = (Person) var15;
     *         String var16 = var5.getFormAttribute("status");
     *         Class var17 = status_type;
     *         Type var18 = status_genericType;
     *         MediaType var19 = status_mediaType;
     *         Object var20 = MultipartSupport.convertFormAttribute(var16, var17, var18, var19, var3);
     *         var0.setStatus((Status) var20);
     *         QuarkusFileUpload var21 = MultipartSupport.getFileUpload("txtFile", var3);
     *         File var22;
     *         if (var21 != null) {
     *             var22 = var21.uploadedFile().toFile();
     *         } else {
     *             var22 = null;
     *         }
     *
     *         var0.txtFile = (File) var22;
     *         QuarkusFileUpload var23 = MultipartSupport.getFileUpload("htmlFile", var3);
     *         Path var24;
     *         if (var23 != null) {
     *             var24 = var23.uploadedFile();
     *         } else {
     *             var24 = null;
     *         }
     *
     *         var0.xmlPart = (Path) var24;
     *     }
     * }
     * </pre>
     *
     */
    static String generate(ClassInfo multipartClassInfo, ClassOutput classOutput, IndexView index) {
        if (!multipartClassInfo.hasNoArgsConstructor()) {
            throw new IllegalArgumentException("Classes annotated with '@MultipartForm' must contain a no-args constructor. " +
                    "The constructor is missing on " + multipartClassInfo.name());
        }

        String multipartClassName = multipartClassInfo.name().toString();
        String generateClassName = multipartClassName + "_generated_populator";
        try (ClassCreator cc = new ClassCreator(classOutput, generateClassName, null, Object.class.getName())) {
            MethodCreator populate = cc.getMethodCreator(DotNames.POPULATE_METHOD_NAME, void.class.getName(),
                    multipartClassName,
                    ResteasyReactiveInjectionContext.class.getName());
            populate.setModifiers(Modifier.PUBLIC | Modifier.STATIC);

            MethodCreator clinit = cc.getMethodCreator("<clinit>", void.class);
            clinit.setModifiers(Modifier.PUBLIC | Modifier.STATIC);

            ResultHandle instanceHandle = populate.getMethodParam(0);
            ResultHandle rrCtxHandle = populate.checkCast(populate.getMethodParam(1), ResteasyReactiveRequestContext.class);
            ResultHandle serverReqHandle = populate.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(ResteasyReactiveRequestContext.class, "serverRequest", ServerHttpRequest.class),
                    rrCtxHandle);

            // go up the class hierarchy until we reach Object
            ClassInfo currentClassInHierarchy = multipartClassInfo;
            while (true) {
                List<FieldInfo> fields = currentClassInHierarchy.fields();
                for (FieldInfo field : fields) {
                    if (Modifier.isStatic(field.flags())) { // nothing we need to do about static fields
                        continue;
                    }

                    AnnotationInstance formParamInstance = field.annotation(REST_FORM_PARAM);
                    if (formParamInstance == null) {
                        formParamInstance = field.annotation(FORM_PARAM);
                    }
                    if (formParamInstance == null) { // fields not annotated with @RestForm or @FormParam are completely ignored
                        continue;
                    }

                    boolean useFieldAccess = false;
                    String setterName = JavaBeanUtil.getSetterName(field.name());
                    Type fieldType = field.type();
                    DotName fieldDotName = fieldType.name();
                    MethodInfo setter = currentClassInHierarchy.method(setterName, fieldType);
                    if (setter == null) {
                        // even if the field is private, it will be transformed to be made public
                        useFieldAccess = true;
                    }
                    if (!useFieldAccess && !Modifier.isPublic(setter.flags())) {
                        throw new IllegalArgumentException(
                                "Setter '" + setterName + "' of class '" + multipartClassInfo + "' must be public");
                    }

                    if (fieldDotName.equals(DotNames.INPUT_STREAM_NAME)
                            || fieldDotName.equals(DotNames.INPUT_STREAM_READER_NAME)) {
                        // don't support InputStream as it's too easy to get into trouble
                        throw new IllegalArgumentException(
                                "InputStream and InputStreamReader are not supported as a field type of a Multipart POJO class. Offending field is '"
                                        + field.name() + "' of class '" + multipartClassName + "'");
                    }

                    String formAttrName = field.name();
                    boolean formAttrNameSet = false;
                    AnnotationValue formParamValue = formParamInstance.value();
                    if (formParamValue != null) {
                        formAttrNameSet = true;
                        formAttrName = formParamValue.asString();
                    }

                    // TODO: not sure this is correct, but it seems to be what RESTEasy does and it also makes most sense in the context of a POJO
                    String partType = MediaType.TEXT_PLAIN;
                    AnnotationInstance partTypeInstance = field.annotation(ResteasyReactiveDotNames.PART_TYPE_NAME);
                    if (partTypeInstance != null) {
                        AnnotationValue partTypeValue = partTypeInstance.value();
                        if (partTypeValue != null) {
                            partType = partTypeValue.asString();
                        }
                    }

                    ResultHandle formAttrNameHandle = populate.load(formAttrName);
                    AssignableResultHandle resultHandle = populate.createVariable(Object.class);

                    if (isFileRelatedType(fieldDotName)) {
                        // uploaded file are present in the RoutingContext and are extracted using MultipartSupport#getFileUpload

                        ResultHandle fileUploadHandle = populate.invokeStaticMethod(
                                MethodDescriptor.ofMethod(MultipartSupport.class, "getFileUpload", DefaultFileUpload.class,
                                        String.class, ResteasyReactiveRequestContext.class),
                                formAttrNameHandle, rrCtxHandle);
                        if (fieldDotName.equals(DotNames.FIELD_UPLOAD_NAME)) {
                            populate.assign(resultHandle, fileUploadHandle);
                        } else if (fieldDotName.equals(DotNames.PATH_NAME) || fieldDotName.equals(DotNames.FILE_NAME)) {
                            BranchResult fileUploadNullBranch = populate.ifNull(fileUploadHandle);
                            BytecodeCreator fileUploadNullTrue = fileUploadNullBranch.trueBranch();
                            fileUploadNullTrue.assign(resultHandle, populate.loadNull());
                            fileUploadNullTrue.breakScope();
                            BytecodeCreator fileUploadFalse = fileUploadNullBranch.falseBranch();
                            ResultHandle pathHandle = fileUploadFalse.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(DefaultFileUpload.class, "uploadedFile", Path.class),
                                    fileUploadHandle);
                            if (fieldDotName.equals(DotNames.PATH_NAME)) {
                                fileUploadFalse.assign(resultHandle, pathHandle);
                            } else {
                                fileUploadFalse.assign(resultHandle, fileUploadFalse.invokeInterfaceMethod(
                                        MethodDescriptor.ofMethod(Path.class, "toFile", File.class), pathHandle));
                            }
                            fileUploadFalse.breakScope();
                        }
                    } else if (isListOfFileUpload(fieldType)) {
                        // in this case we allow injection of all the uploaded file as long as a name
                        // was not provided in @RestForm (which makes no semantic sense)
                        if (formAttrNameSet) {
                            Type fieldGenericType = fieldType.asParameterizedType().arguments().get(0);
                            ResultHandle fileUploadHandle;
                            if (fieldGenericType.name().equals(DotNames.FIELD_UPLOAD_NAME)) {
                                fileUploadHandle = populate.invokeStaticMethod(
                                        MethodDescriptor.ofMethod(MultipartSupport.class, "getFileUploads",
                                                List.class,
                                                String.class, ResteasyReactiveRequestContext.class),
                                        formAttrNameHandle, rrCtxHandle);
                            } else if (fieldGenericType.name().equals(DotNames.PATH_NAME)) {
                                fileUploadHandle = populate.invokeStaticMethod(
                                        MethodDescriptor.ofMethod(MultipartSupport.class, "getJavaPathFileUploads",
                                                List.class,
                                                String.class, ResteasyReactiveRequestContext.class),
                                        formAttrNameHandle, rrCtxHandle);
                            } else if (fieldGenericType.name().equals(DotNames.FILE_NAME)) {
                                fileUploadHandle = populate.invokeStaticMethod(
                                        MethodDescriptor.ofMethod(MultipartSupport.class, "getJavaIOFileUploads",
                                                List.class,
                                                String.class, ResteasyReactiveRequestContext.class),
                                        formAttrNameHandle, rrCtxHandle);
                            } else {
                                throw new IllegalArgumentException(
                                        "Unhandled genetic type '" + fieldGenericType.name().toString() + "'");
                            }
                            populate.assign(resultHandle, fileUploadHandle);
                        } else {
                            ResultHandle allFileUploadsHandle = populate.invokeStaticMethod(
                                    MethodDescriptor.ofMethod(MultipartSupport.class, "getFileUploads", List.class,
                                            ResteasyReactiveRequestContext.class),
                                    rrCtxHandle);
                            populate.assign(resultHandle, allFileUploadsHandle);
                        }
                    } else {
                        // this is a common enough mistake, so let's provide a good error message
                        failIfFileTypeUsedAsGenericType(field, fieldType, fieldDotName);

                        if (fieldType.kind() == Type.Kind.ARRAY) {
                            if (fieldType.asArrayType().component().name().equals(DotNames.BYTE_NAME)) {
                                throw new IllegalArgumentException(
                                        "'byte[]' cannot be used to read multipart file contents. Offending field is '"
                                                + field.name() + "' of class '"
                                                + field.declaringClass().name()
                                                + "'. If you need to read the contents of the uploaded file, use 'Path' or 'File' as the field type and use File IO APIs to read the bytes, while making sure you annotate the endpoint with '@Blocking'");
                            }
                        }
                        if (fieldDotName.equals(DotNames.STRING_NAME) && partType.equals(MediaType.TEXT_PLAIN)) {
                            // in this case all we need to do is read the value of the form attribute

                            populate.assign(resultHandle,
                                    populate.invokeVirtualMethod(MethodDescriptor.ofMethod(ResteasyReactiveRequestContext.class,
                                            "getFormParameter", Object.class, String.class, boolean.class, boolean.class),
                                            rrCtxHandle,
                                            formAttrNameHandle, populate.load(true), populate.load(false)));
                        } else {
                            // we need to use the field type and the media type to locate a MessageBodyReader

                            FieldDescriptor typeField = cc.getFieldCreator(field.name() + "_type", Class.class)
                                    .setModifiers(Modifier.PRIVATE | Modifier.STATIC).getFieldDescriptor();
                            FieldDescriptor genericTypeField = cc
                                    .getFieldCreator(field.name() + "_genericType", java.lang.reflect.Type.class)
                                    .setModifiers(Modifier.PRIVATE | Modifier.STATIC).getFieldDescriptor();
                            FieldDescriptor mediaTypeField = cc.getFieldCreator(field.name() + "_mediaType", MediaType.class)
                                    .setModifiers(Modifier.PRIVATE | Modifier.STATIC).getFieldDescriptor();

                            ResultHandle typeHandle = clinit.invokeStaticMethod(
                                    MethodDescriptor.ofMethod(DeploymentUtils.class, "loadClass", Class.class, String.class),
                                    clinit.load(fieldDotName.toString()));
                            clinit.writeStaticField(typeField, typeHandle);
                            if ((fieldType.kind() != Type.Kind.CLASS) && (fieldType.kind() != Type.Kind.PRIMITIVE)) {
                                // in order to pass the generic type we use the same trick with capturing and at runtime
                                // parsing the signature
                                TypeArgMapper typeArgMapper = new TypeArgMapper(field.declaringClass(), index);
                                ResultHandle genericTypeHandle = clinit.invokeStaticMethod(
                                        MethodDescriptor.ofMethod(TypeSignatureParser.class, "parse",
                                                java.lang.reflect.Type.class, String.class),
                                        clinit.load(AsmUtil.getSignature(fieldType, typeArgMapper)));
                                clinit.writeStaticField(genericTypeField, genericTypeHandle);
                            } else {
                                clinit.writeStaticField(genericTypeField, typeHandle);
                            }
                            clinit.writeStaticField(mediaTypeField, clinit.invokeStaticMethod(
                                    MethodDescriptor.ofMethod(MediaType.class, "valueOf", MediaType.class, String.class),
                                    clinit.load(partType)));

                            ResultHandle formStrValueHandle = populate.invokeVirtualMethod(
                                    MethodDescriptor.ofMethod(ResteasyReactiveRequestContext.class,
                                            "getFormParameter", Object.class, String.class, boolean.class, boolean.class),
                                    rrCtxHandle,
                                    formAttrNameHandle, populate.load(true), populate.load(false));

                            populate.assign(resultHandle, populate.invokeStaticMethod(
                                    MethodDescriptor.ofMethod(MultipartSupport.class, "convertFormAttribute", Object.class,
                                            String.class,
                                            Class.class,
                                            java.lang.reflect.Type.class, MediaType.class,
                                            ResteasyReactiveRequestContext.class, String.class),
                                    formStrValueHandle, populate.readStaticField(typeField),
                                    populate.readStaticField(genericTypeField),
                                    populate.readStaticField(mediaTypeField),
                                    rrCtxHandle, formAttrNameHandle));
                        }
                    }

                    if (useFieldAccess) {
                        BytecodeCreator bc = populate;
                        if (fieldType.kind() == Type.Kind.PRIMITIVE) {
                            bc = populate.ifNull(resultHandle).falseBranch();
                        }
                        bc.writeInstanceField(FieldDescriptor.of(currentClassInHierarchy.name().toString(), field.name(),
                                fieldDotName.toString()), instanceHandle, resultHandle);
                    } else {
                        BytecodeCreator bc = populate;
                        if (fieldType.kind() == Type.Kind.PRIMITIVE) {
                            bc = populate.ifNull(resultHandle).falseBranch();
                        }
                        bc.invokeVirtualMethod(MethodDescriptor.ofMethod(currentClassInHierarchy.name().toString(),
                                setterName, void.class, fieldDotName.toString()), instanceHandle, resultHandle);
                    }
                }

                DotName superClassDotName = currentClassInHierarchy.superName();
                if (superClassDotName.equals(DotNames.OBJECT_NAME)) {
                    break;
                }
                ClassInfo newCurrentClassInHierarchy = index.getClassByName(superClassDotName);
                if (newCurrentClassInHierarchy == null) {
                    if (!superClassDotName.toString().startsWith("java.")) {
                        LOGGER.warn("Class '" + superClassDotName + "' which is a parent class of '"
                                + currentClassInHierarchy.name()
                                + "' is not part of the Jandex index so its fields will be ignored. If you intended to include these fields, consider making the dependency part of the Jandex index by following the advice at: https://quarkus.io/guides/cdi-reference#bean_discovery");
                    }
                    break;
                }
                currentClassInHierarchy = newCurrentClassInHierarchy;
            }
            clinit.returnValue(null);
            populate.returnValue(null);
        }
        return generateClassName;
    }

    private static boolean isFileRelatedType(DotName type) {
        return type.equals(DotNames.FIELD_UPLOAD_NAME) || type.equals(DotNames.PATH_NAME) || type.equals(DotNames.FILE_NAME);
    }

    private static boolean isListOfFileUpload(Type fieldType) {
        if ((fieldType.kind() == Type.Kind.PARAMETERIZED_TYPE) && fieldType.name().equals(ResteasyReactiveDotNames.LIST)) {
            ParameterizedType parameterizedType = fieldType.asParameterizedType();
            if (!parameterizedType.arguments().isEmpty()) {
                DotName argTypeDotName = parameterizedType.arguments().get(0).name();
                return isFileRelatedType(argTypeDotName);
            }
        }
        return false;
    }

    private static void failIfFileTypeUsedAsGenericType(FieldInfo field, Type fieldType, DotName fieldDotName) {
        if (fieldType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType parameterizedType = fieldType.asParameterizedType();
            if (!parameterizedType.arguments().isEmpty()) {
                DotName argTypeDotName = parameterizedType.arguments().get(0).name();
                if (isFileRelatedType(argTypeDotName)) {
                    throw new IllegalArgumentException("Type '" + argTypeDotName.withoutPackagePrefix()
                            + "' cannot be used as a generic type of '"
                            + fieldDotName.withoutPackagePrefix() + "'. Offending field is '" + field.name() + "' of class '"
                            + field.declaringClass().name() + "'");
                }
            }
        }
    }
}
