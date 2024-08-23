package org.jboss.resteasy.reactive.server.processor.generation.multipart;

import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.FORM_PARAM;
import static org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames.REST_FORM_PARAM;

import java.lang.reflect.Modifier;
import java.util.List;

import jakarta.ws.rs.core.MediaType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.processor.JavaBeanUtil;
import org.jboss.resteasy.reactive.common.processor.ResteasyReactiveDotNames;
import org.jboss.resteasy.reactive.server.core.multipart.MultipartMessageBodyWriter;
import org.jboss.resteasy.reactive.server.core.multipart.MultipartOutputInjectionTarget;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataOutput;
import org.jboss.resteasy.reactive.server.multipart.PartItem;

import io.quarkus.gizmo.AssignableResultHandle;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

public final class FormDataOutputMapperGenerator {

    private static final Logger LOGGER = Logger.getLogger(FormDataOutputMapperGenerator.class);

    private static final String TRANSFORM_METHOD_NAME = "mapFrom";
    private static final String ADD_FORM_DATA_METHOD_NAME = "addFormData";

    private FormDataOutputMapperGenerator() {
    }

    /**
     * Returns true whether the returning type uses either {@link org.jboss.resteasy.reactive.RestForm}
     * or {@link org.jboss.resteasy.reactive.server.core.multipart.FormData} annotations.
     */
    public static boolean isReturnTypeCompatible(ClassInfo returnTypeClassInfo, IndexView index) {
        // go up the class hierarchy until we reach Object
        ClassInfo currentClassInHierarchy = returnTypeClassInfo;
        while (true) {
            List<FieldInfo> fields = currentClassInHierarchy.fields();
            for (FieldInfo field : fields) {
                if (Modifier.isStatic(field.flags())) { // nothing we need to do about static fields
                    continue;
                }

                if (field.annotation(REST_FORM_PARAM) != null || field.annotation(FORM_PARAM) != null) {
                    // Found either @RestForm or @FormParam in returning class, it's compatible.
                    return true;
                }
            }

            DotName superClassDotName = currentClassInHierarchy.superName();
            if (superClassDotName.equals(ResteasyReactiveDotNames.OBJECT_NAME)) {
                break;
            }
            ClassInfo newCurrentClassInHierarchy = index.getClassByName(superClassDotName);
            if (newCurrentClassInHierarchy == null) {
                printWarningMessageForMissingJandexIndex(currentClassInHierarchy, superClassDotName);
                break;
            }

            currentClassInHierarchy = newCurrentClassInHierarchy;
        }

        // if we reach this point then the returning type is not compatible.
        return false;
    }

    /**
     * Generates a class that map a Pojo into {@link PartItem} that is then used by {@link MultipartMessageBodyWriter}.
     *
     * <p>
     * For example for a pojo like:
     *
     * <pre>
     * public class FormData {
     *
     *     &#64;RestForm
     *     &#64;PartType(MediaType.TEXT_PLAIN)
     *     private String text;
     *
     *     &#64;RestForm
     *     &#64;PartType(MediaType.APPLICATION_OCTET_STREAM)
     *     public File file;
     *
     *     public String getText() {
     *         return text;
     *     }
     *
     *     public void setText(String text) {
     *         this.text = text;
     *     }
     *
     *     public File getFile() {
     *         return file;
     *     }
     *
     *     public void setFile(File file) {
     *         this.file = file;
     *     }
     * }
     * </pre>
     *
     * <p>
     *
     * The generated mapper would look like:
     *
     * <pre>
     * public class FormData_generated_mapper implements MultipartOutputInjectionTarget {
     *
     *     public MultipartFormDataOutput mapFrom(Object var1) {
     *         MultipartFormDataOutput var2 = new MultipartFormDataOutput();
     *         FormData var4 = (FormData) var1;
     *         File var3 = var4.data;
     *         var2.addFormData("file", var3, "application/octet-stream");
     *         File var5 = var4.text;
     *         var2.addFormData("text", var5, "text/plain");
     *         return var2;
     *     }
     * }
     * </pre>
     */
    public static String generate(ClassInfo returnTypeClassInfo, ClassOutput classOutput, IndexView index) {
        String returnClassName = returnTypeClassInfo.name().toString();
        String generateClassName = MultipartMessageBodyWriter.getGeneratedMapperClassNameFor(returnClassName);
        String interfaceClassName = MultipartOutputInjectionTarget.class.getName();
        try (ClassCreator cc = new ClassCreator(classOutput, generateClassName, null, Object.class.getName(),
                interfaceClassName)) {
            MethodCreator populate = cc.getMethodCreator(TRANSFORM_METHOD_NAME, MultipartFormDataOutput.class.getName(),
                    Object.class);
            populate.setModifiers(Modifier.PUBLIC);

            ResultHandle formDataInstanceHandle = populate.newInstance(MethodDescriptor
                    .ofConstructor(MultipartFormDataOutput.class));
            ResultHandle inputInstanceHandle = populate.checkCast(populate.getMethodParam(0), returnClassName);

            // go up the class hierarchy until we reach Object
            ClassInfo currentClassInHierarchy = returnTypeClassInfo;
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
                    String getterName = JavaBeanUtil.getGetterName(field.name(), field.type().name());
                    Type fieldType = field.type();
                    DotName fieldDotName = fieldType.name();
                    MethodInfo getter = currentClassInHierarchy.method(getterName);
                    if (getter == null) {
                        // even if the field is private, it will be transformed to be made public
                        useFieldAccess = true;
                    }
                    if (!useFieldAccess && !Modifier.isPublic(getter.flags())) {
                        throw new IllegalArgumentException(
                                "Getter '" + getterName + "' of class '" + returnTypeClassInfo + "' must be public");
                    }

                    String formAttrName = field.name();
                    AnnotationValue formParamValue = formParamInstance.value();
                    if (formParamValue != null) {
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

                    // Cast part type to MediaType.
                    AssignableResultHandle partTypeHandle = populate.createVariable(MediaType.class);
                    populate.assign(partTypeHandle,
                            populate.invokeStaticMethod(
                                    MethodDescriptor.ofMethod(MediaType.class, "valueOf", MediaType.class, String.class),
                                    populate.load(partType)));

                    // Continue with the value
                    AssignableResultHandle resultHandle = populate.createVariable(Object.class);

                    if (useFieldAccess) {
                        populate.assign(resultHandle,
                                populate.readInstanceField(
                                        FieldDescriptor.of(currentClassInHierarchy.name().toString(), field.name(),
                                                fieldDotName.toString()),
                                        inputInstanceHandle));
                    } else {
                        populate.assign(resultHandle,
                                populate.invokeVirtualMethod(
                                        MethodDescriptor.ofMethod(currentClassInHierarchy.name().toString(),
                                                getterName, fieldDotName.toString()),
                                        inputInstanceHandle));
                    }

                    // Get parameterized type if field type is a parameterized class
                    String genericType = "";
                    if (fieldType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                        List<Type> argumentTypes = fieldType.asParameterizedType().arguments();
                        if (argumentTypes.size() > 0) {
                            genericType = argumentTypes.get(0).name().toString();
                        }
                    }

                    // Add it to the form data object
                    populate.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(MultipartFormDataOutput.class, ADD_FORM_DATA_METHOD_NAME, PartItem.class,
                                    String.class, Object.class, String.class, MediaType.class),
                            formDataInstanceHandle,
                            populate.load(formAttrName), resultHandle, populate.load(genericType), partTypeHandle);
                }

                DotName superClassDotName = currentClassInHierarchy.superName();
                if (superClassDotName.equals(ResteasyReactiveDotNames.OBJECT_NAME)) {
                    break;
                }
                ClassInfo newCurrentClassInHierarchy = index.getClassByName(superClassDotName);
                if (newCurrentClassInHierarchy == null) {
                    printWarningMessageForMissingJandexIndex(currentClassInHierarchy, superClassDotName);
                    break;
                }
                currentClassInHierarchy = newCurrentClassInHierarchy;
            }

            populate.returnValue(formDataInstanceHandle);
        }
        return generateClassName;
    }

    private static void printWarningMessageForMissingJandexIndex(ClassInfo currentClassInHierarchy, DotName superClassDotName) {
        if (!superClassDotName.toString().startsWith("java.")) {
            LOGGER.warn("Class '" + superClassDotName + "' which is a parent class of '"
                    + currentClassInHierarchy.name()
                    + "' is not part of the Jandex index so its fields will be ignored. If you intended to include these fields, consider making the dependency part of the Jandex index by following the advice at: https://quarkus.io/guides/cdi-reference#bean_discovery");
        }
    }
}
