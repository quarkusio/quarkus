package io.quarkus.panache.common.deployment.visitors;

import static io.quarkus.panache.common.deployment.PanacheEntityEnhancer.JSON_IGNORE_DOT_NAME;
import static io.quarkus.panache.common.deployment.PanacheEntityEnhancer.JSON_PROPERTY_SIGNATURE;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
import io.quarkus.gizmo.Gizmo;
import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.EntityField.EntityFieldAnnotation;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.MetamodelInfo;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.PanacheFieldAccessMethodVisitor;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizerVisitor;
import io.quarkus.panache.common.deployment.PanacheMovingAnnotationVisitor;

public abstract class PanacheEntityClassVisitor<EntityFieldType extends EntityField> extends ClassVisitor {

    protected Type thisClass;
    protected final Map<String, ? extends EntityFieldType> fields;
    private final Set<String> userMethods = new HashSet<>();
    private final MetamodelInfo<?> modelInfo;
    protected final ClassInfo panacheEntityBaseClassInfo;
    protected ClassInfo entityInfo;
    protected List<PanacheMethodCustomizer> methodCustomizers;

    public PanacheEntityClassVisitor(String className, ClassVisitor outputClassVisitor,
            MetamodelInfo<? extends EntityModel<? extends EntityFieldType>> modelInfo,
            ClassInfo panacheEntityBaseClassInfo, ClassInfo entityInfo,
            List<PanacheMethodCustomizer> methodCustomizers) {
        super(Gizmo.ASM_API_VERSION, outputClassVisitor);

        thisClass = Type.getType("L" + className.replace('.', '/') + ";");
        this.modelInfo = modelInfo;
        EntityModel<? extends EntityFieldType> entityModel = modelInfo.getEntityModel(className);
        fields = entityModel != null ? entityModel.fields : null;
        this.panacheEntityBaseClassInfo = panacheEntityBaseClassInfo;
        this.entityInfo = entityInfo;
        this.methodCustomizers = methodCustomizers;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        EntityField ef = fields.get(name);
        if (ef == null) {
            return super.visitField(access, name, descriptor, signature, value);
        }
        //we make the fields protected
        //so any errors are visible immediately, rather than data just being lost

        FieldVisitor superVisitor;
        if (name.equals("id")) {
            superVisitor = super.visitField(access, name, descriptor, signature, value);
        } else {
            superVisitor = super.visitField((access | Modifier.PROTECTED) & ~(Modifier.PRIVATE | Modifier.PUBLIC),
                    name, descriptor, signature, value);
        }
        ef.signature = signature;
        // if we have a mapped field, let's add some annotations
        return new FieldVisitor(Gizmo.ASM_API_VERSION, superVisitor) {
            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (!descriptor.startsWith(PanacheEntityEnhancer.JAXB_ANNOTATION_PREFIX)) {
                    return super.visitAnnotation(descriptor, visible);
                } else {
                    // Save off JAX-B annotations on the field so they can be applied to the generated getter later
                    EntityFieldAnnotation efAnno = new EntityFieldAnnotation(descriptor);
                    ef.annotations.add(efAnno);
                    return new PanacheMovingAnnotationVisitor(efAnno);
                }
            }

            @Override
            public void visitEnd() {
                // Add the @JaxbTransient property to the field so that JAXB prefers the generated getter (otherwise JAXB complains about
                // having a field and property both with the same name)
                // JSONB will already use the getter so we're good
                // Note: we don't need to check if we already have @XmlTransient in the descriptors because if we did, we moved it to the getter
                // so we can't have any duplicate
                super.visitAnnotation(PanacheEntityEnhancer.JAXB_TRANSIENT_SIGNATURE, true);
                super.visitEnd();
            }
        };
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature,
            String[] exceptions) {
        userMethods.add(methodName + "/" + descriptor);
        MethodVisitor superVisitor = super.visitMethod(access, methodName, descriptor, signature, exceptions);
        if (Modifier.isStatic(access)
                && Modifier.isPublic(access)
                && (access & Opcodes.ACC_SYNTHETIC) == 0
                && !methodCustomizers.isEmpty()) {
            org.jboss.jandex.Type[] argTypes = AsmUtil.getParameterTypes(descriptor);
            MethodInfo method = this.entityInfo.method(methodName, argTypes);
            if (method == null) {
                throw new IllegalStateException(
                        "Could not find indexed method: " + thisClass + "." + methodName + " with descriptor " + descriptor
                                + " and arg types " + Arrays.toString(argTypes));
            }
            superVisitor = new PanacheMethodCustomizerVisitor(superVisitor, method, thisClass, methodCustomizers);
        }
        return new PanacheFieldAccessMethodVisitor(superVisitor, thisClass.getInternalName(), methodName, descriptor,
                modelInfo);
    }

    @Override
    public void visitEnd() {
        // FIXME: generate default constructor

        for (MethodInfo method : panacheEntityBaseClassInfo.methods()) {
            // Do not generate a method that already exists
            String descriptor = AsmUtil.getDescriptor(method, name -> null);
            if (!userMethods.contains(method.name() + "/" + descriptor)) {
                AnnotationInstance bridge = method.annotation(PanacheEntityEnhancer.DOTNAME_GENERATE_BRIDGE);
                if (bridge != null) {
                    generateMethod(method, bridge.value("targetReturnTypeErased"));
                }
            }
        }

        generateAccessors();

        super.visitEnd();
    }

    protected void generateMethod(MethodInfo method, AnnotationValue targetReturnTypeErased) {
        String descriptor = AsmUtil.getDescriptor(method, name -> null);
        String signature = AsmUtil.getSignature(method, name -> null);
        List<org.jboss.jandex.Type> parameters = method.parameters();
        String castTo = null;
        if (targetReturnTypeErased != null && targetReturnTypeErased.asBoolean()) {
            castTo = method.returnType().name().toString('/');
        }

        MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                method.name(),
                descriptor,
                signature,
                null);
        AsmUtil.copyParameterNames(mv, method);
        mv.visitCode();
        for (PanacheMethodCustomizer customizer : methodCustomizers) {
            customizer.customize(thisClass, method, mv);
        }
        // inject model
        injectModel(mv);
        for (int i = 0; i < parameters.size(); i++) {
            mv.visitIntInsn(Opcodes.ALOAD, i);
        }
        // inject Class
        String forwardingDescriptor = "(" + getModelDescriptor() + descriptor.substring(1);
        if (castTo != null) {
            // return type is erased to Object
            int lastParen = forwardingDescriptor.lastIndexOf(')');
            forwardingDescriptor = forwardingDescriptor.substring(0, lastParen + 1) + "Ljava/lang/Object;";
        }
        invokeOperation(method, mv, forwardingDescriptor);
        if (castTo != null)
            mv.visitTypeInsn(Opcodes.CHECKCAST, castTo);
        String returnTypeDescriptor = descriptor.substring(descriptor.lastIndexOf(")") + 1);
        mv.visitInsn(AsmUtil.getReturnInstruction(returnTypeDescriptor));
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    protected void invokeOperation(MethodInfo method, MethodVisitor mv, String forwardingDescriptor) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                getPanacheOperationsInternalName(),
                method.name(),
                forwardingDescriptor, false);
    }

    protected String getModelDescriptor() {
        return "Ljava/lang/Class;";
    }

    protected abstract String getPanacheOperationsInternalName();

    protected void injectModel(MethodVisitor mv) {
        mv.visitLdcInsn(thisClass);
    }

    protected void generateAccessors() {
        if (fields == null)
            return;
        for (EntityField field : fields.values()) {
            // Getter
            String getterName = field.getGetterName();
            String getterDescriptor = "()" + field.descriptor;
            if (!userMethods.contains(getterName + "/" + getterDescriptor)) {
                MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC,
                        getterName, getterDescriptor, field.signature == null ? null : "()" + field.signature, null);
                mv.visitCode();
                mv.visitIntInsn(Opcodes.ALOAD, 0);
                generateAccessorGetField(mv, field);
                int returnCode = AsmUtil.getReturnInstruction(field.descriptor);
                mv.visitInsn(returnCode);
                mv.visitMaxs(0, 0);
                // Apply JAX-B annotations that are being transferred from the field
                for (EntityFieldAnnotation anno : field.annotations) {
                    anno.writeToVisitor(mv);
                }
                // Add an explicit Jackson annotation so that the entire property is not ignored due to having @XmlTransient
                // on the field
                if (shouldAddJsonProperty(field)) {
                    mv.visitAnnotation(JSON_PROPERTY_SIGNATURE, true);
                }
                mv.visitEnd();
            }

            // Setter
            String setterName = field.getSetterName();
            String setterDescriptor = "(" + field.descriptor + ")V";
            if (!userMethods.contains(setterName + "/" + setterDescriptor)) {
                MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC,
                        setterName, setterDescriptor, field.signature == null ? null : "(" + field.signature + ")V", null);
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
                generateAccessorSetField(mv, field);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
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

    protected abstract void generateAccessorSetField(MethodVisitor mv, EntityField field);

    protected abstract void generateAccessorGetField(MethodVisitor mv, EntityField field);
}
