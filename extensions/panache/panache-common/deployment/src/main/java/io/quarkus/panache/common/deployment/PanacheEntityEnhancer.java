package io.quarkus.panache.common.deployment;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkus.gizmo.Gizmo;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.deployment.EntityField.EntityFieldAnnotation;

public abstract class PanacheEntityEnhancer<MetamodelType extends MetamodelInfo<?>>
        implements BiFunction<String, ClassVisitor, ClassVisitor> {

    public final static String SORT_NAME = Sort.class.getName();
    public final static String SORT_BINARY_NAME = SORT_NAME.replace('.', '/');
    public final static String SORT_SIGNATURE = "L" + SORT_BINARY_NAME + ";";

    public final static String PARAMETERS_NAME = Parameters.class.getName();
    public final static String PARAMETERS_BINARY_NAME = PARAMETERS_NAME.replace('.', '/');
    public final static String PARAMETERS_SIGNATURE = "L" + PARAMETERS_BINARY_NAME + ";";

    private static final String JAXB_ANNOTATION_PREFIX = "Ljavax/xml/bind/annotation/";
    private static final String JAXB_TRANSIENT_BINARY_NAME = "javax/xml/bind/annotation/XmlTransient";
    private static final String JAXB_TRANSIENT_SIGNATURE = "L" + JAXB_TRANSIENT_BINARY_NAME + ";";

    protected MetamodelType modelInfo;
    protected final ClassInfo panacheEntityBaseClassInfo;
    protected final IndexView indexView;

    public PanacheEntityEnhancer(IndexView index, DotName panacheEntityBaseName) {
        this.panacheEntityBaseClassInfo = index.getClassByName(panacheEntityBaseName);
        this.indexView = index;
    }

    @Override
    public abstract ClassVisitor apply(String className, ClassVisitor outputClassVisitor);

    protected abstract static class PanacheEntityClassVisitor<EntityFieldType extends EntityField> extends ClassVisitor {

        protected Type thisClass;
        protected Map<String, ? extends EntityFieldType> fields;
        // set of name + "/" + descriptor (only for suspected accessor names)
        private Set<String> methods = new HashSet<>();
        private MetamodelInfo<?> modelInfo;
        private ClassInfo panacheEntityBaseClassInfo;
        protected ClassInfo entityInfo;

        public PanacheEntityClassVisitor(String className, ClassVisitor outputClassVisitor,
                MetamodelInfo<? extends EntityModel<? extends EntityFieldType>> modelInfo,
                ClassInfo panacheEntityBaseClassInfo,
                ClassInfo entityInfo) {
            super(Gizmo.ASM_API_VERSION, outputClassVisitor);
            thisClass = Type.getType("L" + className.replace('.', '/') + ";");
            this.modelInfo = modelInfo;
            EntityModel<? extends EntityFieldType> entityModel = modelInfo.getEntityModel(className);
            fields = entityModel != null ? entityModel.fields : null;
            this.panacheEntityBaseClassInfo = panacheEntityBaseClassInfo;
            this.entityInfo = entityInfo;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            FieldVisitor superVisitor = super.visitField(access, name, descriptor, signature, value);
            EntityField ef = fields.get(name);
            if (ef == null) {
                return superVisitor;
            }
            ef.signature = signature;
            // if we have a mapped field, let's add some annotations
            return new FieldVisitor(Gizmo.ASM_API_VERSION, superVisitor) {
                private Set<String> descriptors = new HashSet<>();

                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    descriptors.add(descriptor);
                    if (!descriptor.startsWith(JAXB_ANNOTATION_PREFIX)) {
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
                    // add the @JaxbTransient property to the field so that Jackson prefers the generated getter
                    // jsonb will already use the getter so we're good
                    super.visitAnnotation(JAXB_TRANSIENT_SIGNATURE, true);
                    super.visitEnd();
                }
            };
        }

        @Override
        public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature,
                String[] exceptions) {
            if (methodName.startsWith("get")
                    || methodName.startsWith("set")
                    || methodName.startsWith("is")) {
                methods.add(methodName + "/" + descriptor);
            }
            MethodVisitor superVisitor = super.visitMethod(access, methodName, descriptor, signature, exceptions);
            return new PanacheFieldAccessMethodVisitor(superVisitor, thisClass.getInternalName(), methodName, descriptor,
                    modelInfo);
        }

        @Override
        public void visitEnd() {
            // FIXME: generate default constructor

            for (MethodInfo method : panacheEntityBaseClassInfo.methods()) {
                // Do not generate a method that already exists
                if (!JandexUtil.containsMethod(entityInfo, method)) {
                    AnnotationInstance bridge = method.annotation(JandexUtil.DOTNAME_GENERATE_BRIDGE);
                    if (bridge != null) {
                        generateMethod(method, bridge.value("targetReturnTypeErased"));
                    }
                }
            }

            generateAccessors();

            super.visitEnd();
        }

        private void generateMethod(MethodInfo method, AnnotationValue targetReturnTypeErased) {
            String descriptor = JandexUtil.getDescriptor(method, name -> null);
            String signature = JandexUtil.getSignature(method, name -> null);
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
            for (int i = 0; i < parameters.size(); i++) {
                mv.visitParameter(method.parameterName(i), 0 /* modifiers */);
            }
            mv.visitCode();
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
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    getPanacheOperationsBinaryName(),
                    method.name(),
                    forwardingDescriptor, false);
            if (castTo != null)
                mv.visitTypeInsn(Opcodes.CHECKCAST, castTo);
            String returnTypeDescriptor = descriptor.substring(descriptor.lastIndexOf(")") + 1);
            mv.visitInsn(JandexUtil.getReturnInstruction(returnTypeDescriptor));
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        protected abstract String getModelDescriptor();

        protected abstract String getPanacheOperationsBinaryName();

        protected abstract void injectModel(MethodVisitor mv);

        private void generateAccessors() {
            if (fields == null)
                return;
            for (EntityField field : fields.values()) {
                // Getter
                String getterName = field.getGetterName();
                String getterDescriptor = "()" + field.descriptor;
                if (!methods.contains(getterName + "/" + getterDescriptor)) {
                    MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC,
                            getterName, getterDescriptor, field.signature == null ? null : "()" + field.signature, null);
                    mv.visitCode();
                    mv.visitIntInsn(Opcodes.ALOAD, 0);
                    generateAccessorGetField(mv, field);
                    int returnCode = JandexUtil.getReturnInstruction(field.descriptor);
                    mv.visitInsn(returnCode);
                    mv.visitMaxs(0, 0);
                    // Apply JAX-B annotations that are being transferred from the field
                    for (EntityFieldAnnotation anno : field.annotations) {
                        anno.writeToVisitor(mv);
                    }
                    mv.visitEnd();
                }

                // Setter
                String setterName = field.getSetterName();
                String setterDescriptor = "(" + field.descriptor + ")V";
                if (!methods.contains(setterName + "/" + setterDescriptor)) {
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

        protected abstract void generateAccessorSetField(MethodVisitor mv, EntityField field);

        protected abstract void generateAccessorGetField(MethodVisitor mv, EntityField field);
    }

    public abstract void collectFields(ClassInfo classInfo);

    public MetamodelType getModelInfo() {
        return modelInfo;
    }
}
