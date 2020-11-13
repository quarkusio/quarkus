package io.quarkus.panache.common.deployment.visitors;

import static io.quarkus.deployment.util.AsmUtil.getDescriptor;
import static io.quarkus.panache.common.deployment.PanacheEntityEnhancer.JSON_IGNORE_DOT_NAME;
import static io.quarkus.panache.common.deployment.PanacheEntityEnhancer.JSON_PROPERTY_DOT_NAME;
import static io.quarkus.panache.common.deployment.PanacheEntityEnhancer.JSON_PROPERTY_SIGNATURE;
import static io.quarkus.panache.common.deployment.visitors.KotlinPanacheClassVisitor.OBJECT;
import static io.quarkus.panache.common.deployment.visitors.KotlinPanacheClassVisitor.recursivelyFindEntityTypeArguments;
import static io.quarkus.panache.common.deployment.visitors.PanacheRepositoryClassVisitor.CLASS;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.TypeVariable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkus.deployment.util.AsmUtil;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.panache.common.deployment.ByteCodeType;
import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.EntityField.EntityFieldAnnotation;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.MetamodelInfo;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.PanacheFieldAccessMethodVisitor;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizerVisitor;
import io.quarkus.panache.common.deployment.PanacheMovingAnnotationVisitor;
import io.quarkus.panache.common.deployment.TypeBundle;

public abstract class PanacheEntityClassVisitor<EntityFieldType extends EntityField> extends ClassVisitor {

    protected Type thisClass;
    protected final Map<String, ? extends EntityFieldType> fields;
    private final Set<String> userMethods = new HashSet<>();
    private final MetamodelInfo<?> modelInfo;
    protected TypeBundle typeBundle;
    protected final ClassInfo panacheEntityBaseClassInfo;
    protected ClassInfo entityInfo;
    protected List<PanacheMethodCustomizer> methodCustomizers;
    protected final Map<String, ByteCodeType> typeArguments = new HashMap<>();
    protected final Function<String, String> argMapper;
    protected final ByteCodeType entityUpperBound;
    private final Map<String, String> erasures = new HashMap<>();

    public PanacheEntityClassVisitor(ClassVisitor outputClassVisitor,
            MetamodelInfo<? extends EntityModel<? extends EntityFieldType>> modelInfo,
            TypeBundle typeBundle,
            ClassInfo entityInfo,
            List<PanacheMethodCustomizer> methodCustomizers, IndexView indexView) {
        super(Gizmo.ASM_API_VERSION, outputClassVisitor);

        String className = entityInfo.name().toString();
        thisClass = Type.getType("L" + className.replace('.', '/') + ";");
        this.modelInfo = modelInfo;
        this.typeBundle = typeBundle;
        EntityModel<? extends EntityFieldType> entityModel = modelInfo.getEntityModel(className);
        fields = entityModel != null ? entityModel.fields : null;
        this.panacheEntityBaseClassInfo = indexView.getClassByName(typeBundle.entityBase().dotName());
        this.entityInfo = entityInfo;
        this.methodCustomizers = methodCustomizers;

        ByteCodeType baseType = typeBundle.entityBase();
        List<TypeVariable> typeVariables = indexView.getClassByName(baseType.dotName()).typeParameters();
        if (!typeVariables.isEmpty()) {
            entityUpperBound = new ByteCodeType(typeVariables.get(0).bounds().get(0));
        } else {
            entityUpperBound = null;
        }

        discoverTypeParameters(entityInfo, indexView, typeBundle, baseType);
        argMapper = type -> {
            ByteCodeType byteCodeType = typeArguments.get(type);
            return byteCodeType != null
                    ? byteCodeType.descriptor()
                    : OBJECT.descriptor();
        };
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        if (fields == null || fields.isEmpty()) {
            return super.visitField(access, name, descriptor, signature, value);
        }
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

    protected void discoverTypeParameters(ClassInfo classInfo, IndexView indexView, TypeBundle types, ByteCodeType baseType) {
        List<ByteCodeType> foundTypeArguments = recursivelyFindEntityTypeArguments(indexView,
                classInfo.name(), baseType.dotName());

        ByteCodeType entityType = (foundTypeArguments.size() > 0) ? foundTypeArguments.get(0) : OBJECT;
        ByteCodeType idType = (foundTypeArguments.size() > 1) ? foundTypeArguments.get(1) : OBJECT;

        typeArguments.put("Entity", entityType);
        typeArguments.put("Id", idType);
        typeArguments.keySet().stream()
                .filter(k -> !k.equals("Id"))
                .forEach(k -> erasures.put(k, OBJECT.descriptor()));
        try {
            ByteCodeType entity = typeArguments.get("Entity");
            if (entity != null) {
                erasures.put(entity.dotName().toString(), entity.descriptor());
            }
            erasures.put(types.queryType().dotName().toString(), OBJECT.descriptor());
            erasures.put(types.updateType().dotName().toString(), OBJECT.descriptor());
        } catch (UnsupportedOperationException ignored) {
        }
    }

    protected void generateMethod(MethodInfo method, AnnotationValue targetReturnTypeErased) {
        List<org.jboss.jandex.Type> parameters = method.parameters();

        MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                method.name(),
                AsmUtil.getDescriptor(method, name -> null),
                AsmUtil.getSignature(method, name1 -> null),
                null);
        AsmUtil.copyParameterNames(mv, method);
        mv.visitCode();
        for (PanacheMethodCustomizer customizer : methodCustomizers) {
            customizer.customize(thisClass, method, mv);
        }
        loadOperations(mv);
        loadArguments(mv, parameters);
        invokeOperations(mv, method);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void loadOperations(MethodVisitor mv) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, typeBundle.operations().internalName(), "INSTANCE",
                typeBundle.operations().descriptor());
    }

    private void loadArguments(MethodVisitor mv, List<org.jboss.jandex.Type> parameters) {
        // inject Class
        injectModel(mv);
        for (int i = 0; i < parameters.size(); i++) {
            mv.visitIntInsn(Opcodes.ALOAD, i);
        }
    }

    private void invokeOperations(MethodVisitor mv, MethodInfo method) {
        String operationDescriptor;

        StringJoiner joiner = new StringJoiner("", "(", ")");
        joiner.add(CLASS.descriptor());
        descriptors(method, joiner);

        org.jboss.jandex.Type returnType = method.returnType();
        String descriptor = getDescriptor(returnType, argMapper);
        String key = returnType.kind() == org.jboss.jandex.Type.Kind.TYPE_VARIABLE
                ? returnType.asTypeVariable().identifier()
                : returnType.name().toString();
        operationDescriptor = joiner + erasures.getOrDefault(key, descriptor);

        mv.visitMethodInsn(INVOKEVIRTUAL, typeBundle.operations().internalName(), method.name(),
                operationDescriptor, false);
        if (returnType.kind() != org.jboss.jandex.Type.Kind.PRIMITIVE) {
            String cast;
            if (returnType.kind() == org.jboss.jandex.Type.Kind.TYPE_VARIABLE) {
                TypeVariable typeVariable = returnType.asTypeVariable();
                ByteCodeType type = typeArguments.get(typeVariable.identifier());
                if (type == null && typeVariable.bounds().size() != 1) {
                    type = OBJECT;
                } else {
                    type = new ByteCodeType(typeVariable.bounds().get(0));
                }
                cast = type.internalName();
            } else {
                cast = returnType.name().toString().replace('.', '/');
            }
            mv.visitTypeInsn(CHECKCAST, cast);
        }
        mv.visitInsn(AsmUtil.getReturnInstruction(returnType));
    }

    private void descriptors(MethodInfo method, StringJoiner joiner) {
        for (org.jboss.jandex.Type parameter : method.parameters()) {
            if (parameter.kind() == org.jboss.jandex.Type.Kind.TYPE_VARIABLE
                    || method.name().endsWith("ById")
                            && parameter.name().equals(typeArguments.get("Id").dotName())) {
                joiner.add(OBJECT.descriptor());
            } else {
                joiner.add(mapType(parameter));
            }
        }
    }

    private String mapType(org.jboss.jandex.Type parameter) {
        String descriptor;
        switch (parameter.kind()) {
            case PRIMITIVE:
            case TYPE_VARIABLE:
                descriptor = OBJECT.descriptor();
                break;
            default:
                String value = getDescriptor(parameter, argMapper);
                descriptor = erasures.getOrDefault(value, value);
        }
        return descriptor;
    }

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
                    AnnotationVisitor visitor = mv.visitAnnotation(JSON_PROPERTY_SIGNATURE, true);
                    FieldInfo fieldInfo = entityInfo.field(field.name);
                    if (fieldInfo != null) {
                        AnnotationInstance jsonPropertyInstance = fieldInfo.annotation(JSON_PROPERTY_DOT_NAME);
                        // propagate the value of @JsonProperty field annotation to the newly added method annotation
                        if (jsonPropertyInstance != null) {
                            AnnotationValue jsonPropertyValue = jsonPropertyInstance.value();
                            if ((jsonPropertyValue != null) && !jsonPropertyValue.asString().isEmpty()) {
                                visitor.visit("value", jsonPropertyValue.asString());
                            }
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
