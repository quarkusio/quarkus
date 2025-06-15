package io.quarkus.panache.common.deployment.visitors;

import static io.quarkus.panache.common.deployment.PanacheConstants.JAXB_ANNOTATION_PREFIX;
import static io.quarkus.panache.common.deployment.PanacheConstants.JAXB_TRANSIENT_SIGNATURE;
import static io.quarkus.panache.common.deployment.PanacheConstants.JSON_IGNORE_DOT_NAME;
import static io.quarkus.panache.common.deployment.PanacheConstants.JSON_PROPERTY_ACCESS;
import static io.quarkus.panache.common.deployment.PanacheConstants.JSON_PROPERTY_ACCESS_SIGNATURE;
import static io.quarkus.panache.common.deployment.PanacheConstants.JSON_PROPERTY_DEFAULT_VALUE;
import static io.quarkus.panache.common.deployment.PanacheConstants.JSON_PROPERTY_DOT_NAME;
import static io.quarkus.panache.common.deployment.PanacheConstants.JSON_PROPERTY_INDEX;
import static io.quarkus.panache.common.deployment.PanacheConstants.JSON_PROPERTY_NAMESPACE;
import static io.quarkus.panache.common.deployment.PanacheConstants.JSON_PROPERTY_REQUIRED;
import static io.quarkus.panache.common.deployment.PanacheConstants.JSON_PROPERTY_SIGNATURE;
import static io.quarkus.panache.common.deployment.PanacheConstants.JSON_PROPERTY_VALUE;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.EntityField.EntityFieldAnnotation;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.PanacheMovingAnnotationVisitor;

/**
 * A visitor that replaces public fields in an entity with a protected field + accessors, so that the accessors can be
 * called in {@link io.quarkus.panache.common.deployment.PanacheEntityEnhancer}.
 */
public final class PanacheEntityClassAccessorGenerationVisitor extends ClassVisitor {

    protected final Type thisClass;
    private final Map<String, ? extends EntityField> fields;
    private final Set<String> userMethods = new HashSet<>();
    private final ClassInfo entityInfo;

    public PanacheEntityClassAccessorGenerationVisitor(ClassVisitor outputClassVisitor, ClassInfo entityInfo,
            EntityModel entityModel) {
        super(Gizmo.ASM_API_VERSION, outputClassVisitor);

        String className = entityInfo.name().toString();
        thisClass = Type.getType("L" + className.replace('.', '/') + ";");
        fields = entityModel != null ? entityModel.fields : null;
        this.entityInfo = entityInfo;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        EntityField entityField = fields == null ? null : fields.get(name);
        if (entityField == null || !EntityField.Visibility.PUBLIC.equals(entityField.visibility)
                || hasGetterForField(entityField)) {
            // If the field does not exist or is non-public,
            // or if the getter for this entity field already exists,
            // we won't alter it in any way.
            return super.visitField(access, name, descriptor, signature, value);
        }

        // We're going to generate a getter for this field;
        // let's alter the field accordingly.

        // we make the fields protected
        // so any errors are visible immediately, rather than data just being lost
        FieldVisitor superVisitor;
        if (name.equals("id")) {
            superVisitor = super.visitField(access, name, descriptor, signature, value);
        } else {
            superVisitor = super.visitField((access | Modifier.PROTECTED) & ~(Modifier.PRIVATE | Modifier.PUBLIC), name,
                    descriptor, signature, value);
        }
        entityField.signature = signature;
        // if we have a mapped field, let's add some annotations
        return new FieldVisitor(Gizmo.ASM_API_VERSION, superVisitor) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (!descriptor.startsWith(JAXB_ANNOTATION_PREFIX)) {
                    return super.visitAnnotation(descriptor, visible);
                } else {
                    // Save off JAX-B annotations on the field so they can be applied to the generated getter later
                    EntityFieldAnnotation efAnno = new EntityFieldAnnotation(descriptor);
                    entityField.annotations.add(efAnno);
                    return new PanacheMovingAnnotationVisitor(efAnno);
                }
            }

            @Override
            public void visitEnd() {
                // Add the @JaxbTransient property to the field so that JAXB prefers the generated getter (otherwise
                // JAXB complains about
                // having a field and property both with the same name)
                // JSONB will already use the getter, so we're good
                // Note: we don't need to check if we already have @XmlTransient in the descriptors because if we did,
                // we moved it to the getter
                // so we can't have any duplicate
                super.visitAnnotation(JAXB_TRANSIENT_SIGNATURE, true);
                super.visitEnd();
            }
        };
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature,
            String[] exceptions) {
        userMethods.add(methodName + "/" + descriptor);
        return super.visitMethod(access, methodName, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        // FIXME: generate default constructor

        generateAccessors();

        super.visitEnd();
    }

    private void generateAccessors() {
        if (fields == null)
            return;
        for (EntityField field : fields.values()) {
            if (!EntityField.Visibility.PUBLIC.equals(field.visibility)) {
                // We don't generate accessors for non-public fields
                // (but may rely on library-specific accessors in other places)
                continue;
            }
            // Getter
            String getterName = field.getGetterName();
            String getterDescriptor = "()" + field.descriptor;
            if (!userMethods.contains(getterName + "/" + getterDescriptor)) {
                MethodVisitor mv = super.visitMethod(field.visibility.accessOpCode, getterName, getterDescriptor,
                        field.signature == null ? null : "()" + field.signature, null);
                mv.visitCode();
                mv.visitIntInsn(Opcodes.ALOAD, 0);
                mv.visitFieldInsn(Opcodes.GETFIELD, thisClass.getInternalName(), field.name, field.descriptor);
                int returnCode = AsmUtil.getReturnInstruction(field.descriptor);
                mv.visitInsn(returnCode);
                mv.visitMaxs(0, 0);
                // Apply JAX-B annotations that are being transferred from the field
                for (EntityFieldAnnotation anno : field.annotations) {
                    anno.writeToVisitor(mv);
                }
                // Add an explicit JAXB annotation so that the entire property is not ignored due to having
                // @XmlTransient
                // on the field
                if (shouldAddJsonProperty(field)) {
                    AnnotationVisitor visitor = mv.visitAnnotation(JSON_PROPERTY_SIGNATURE, true);
                    FieldInfo fieldInfo = entityInfo.field(field.name);
                    if (fieldInfo != null) {
                        AnnotationInstance jsonPropertyInstance = fieldInfo.annotation(JSON_PROPERTY_DOT_NAME);
                        // propagate the value of @JsonProperty field annotation to the newly added method annotation
                        if (jsonPropertyInstance != null) {
                            propagateJsonPropertyValues(jsonPropertyInstance, visitor);
                        }
                    }
                    visitor.visitEnd();
                }
                mv.visitEnd();
            }

            // Setter
            String setterName = field.getSetterName();
            String setterDescriptor = "(" + field.descriptor + ")V";
            if (!userMethods.contains(setterName + "/" + setterDescriptor)) {
                MethodVisitor mv = super.visitMethod(field.visibility.accessOpCode, setterName, setterDescriptor,
                        field.signature == null ? null : "(" + field.signature + ")V", null);
                mv.visitCode();
                mv.visitIntInsn(Opcodes.ALOAD, 0);
                int loadCode;
                switch (field.descriptor) {
                    case "Z":
                    case "B":
                    case "C":
                    case "S":
                    case "I":
                        loadCode = Opcodes.ILOAD;
                        break;
                    case "J":
                        loadCode = Opcodes.LLOAD;
                        break;
                    case "F":
                        loadCode = Opcodes.FLOAD;
                        break;
                    case "D":
                        loadCode = Opcodes.DLOAD;
                        break;
                    default:
                        loadCode = Opcodes.ALOAD;
                        break;
                }
                mv.visitIntInsn(loadCode, 1);
                mv.visitFieldInsn(Opcodes.PUTFIELD, thisClass.getInternalName(), field.name, field.descriptor);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
        }
    }

    private void propagateJsonPropertyValues(AnnotationInstance from, AnnotationVisitor to) {
        copyAnnotationStringValue(from, to, JSON_PROPERTY_VALUE);
        copyAnnotationStringValue(from, to, JSON_PROPERTY_NAMESPACE);
        copyAnnotationStringValue(from, to, JSON_PROPERTY_DEFAULT_VALUE);

        AnnotationValue jsonPropertyRequired = from.value(JSON_PROPERTY_REQUIRED);
        if (jsonPropertyRequired != null) {
            to.visit(JSON_PROPERTY_REQUIRED, jsonPropertyRequired.asBoolean());
        }

        AnnotationValue jsonPropertyIndex = from.value(JSON_PROPERTY_INDEX);
        if (jsonPropertyIndex != null) {
            to.visit(JSON_PROPERTY_INDEX, jsonPropertyIndex.asInt());
        }

        AnnotationValue jsonPropertyAccess = from.value(JSON_PROPERTY_ACCESS);
        if (jsonPropertyAccess != null) {
            to.visitEnum(JSON_PROPERTY_ACCESS, JSON_PROPERTY_ACCESS_SIGNATURE, jsonPropertyAccess.asString());
        }

    }

    private void copyAnnotationStringValue(AnnotationInstance from, AnnotationVisitor to, String property) {
        AnnotationValue value = from.value(property);
        if ((value != null) && !value.asString().isEmpty()) {
            to.visit(property, value.asString());
        }
    }

    private boolean shouldAddJsonProperty(EntityField entityField) {
        if (isAnnotatedWithJsonIgnore(entityField)) {
            return false;
        }
        return !entityField.hasAnnotation(JSON_PROPERTY_SIGNATURE);
    }

    private boolean isAnnotatedWithJsonIgnore(EntityField entityField) {
        FieldInfo field = entityInfo.field(entityField.name);
        if (field != null) {
            return field.hasAnnotation(JSON_IGNORE_DOT_NAME);
        }

        return false;
    }

    private boolean hasGetterForField(EntityField entityField) {
        MethodInfo methodInfo = entityInfo.method(entityField.getGetterName());
        return methodInfo != null
                && entityField.descriptor.equals(DescriptorUtils.typeToString(methodInfo.returnType()));
    }
}
