package io.quarkus.hibernate.orm.panache.deployment;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import javax.persistence.Transient;

import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;

public class PanacheJpaEntityEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    public final static String ENTITY_BASE_NAME = PanacheEntityBase.class.getName();
    public final static String ENTITY_BASE_BINARY_NAME = ENTITY_BASE_NAME.replace('.', '/');
    public final static String ENTITY_BASE_SIGNATURE = "L" + ENTITY_BASE_BINARY_NAME + ";";

    public final static String SORT_NAME = Sort.class.getName();
    public final static String SORT_BINARY_NAME = SORT_NAME.replace('.', '/');
    public final static String SORT_SIGNATURE = "L" + SORT_BINARY_NAME + ";";

    public final static String PARAMETERS_NAME = Parameters.class.getName();
    public final static String PARAMETERS_BINARY_NAME = PARAMETERS_NAME.replace('.', '/');
    public final static String PARAMETERS_SIGNATURE = "L" + PARAMETERS_BINARY_NAME + ";";

    public final static String QUERY_NAME = PanacheQuery.class.getName();
    public final static String QUERY_BINARY_NAME = QUERY_NAME.replace('.', '/');
    public final static String QUERY_SIGNATURE = "L" + QUERY_BINARY_NAME + ";";

    public final static String JPA_OPERATIONS_NAME = JpaOperations.class.getName();
    public final static String JPA_OPERATIONS_BINARY_NAME = JPA_OPERATIONS_NAME.replace('.', '/');

    private static final String JAXB_TRANSIENT_BINARY_NAME = "javax/xml/bind/annotation/XmlTransient";
    private static final String JAXB_TRANSIENT_SIGNATURE = "L" + JAXB_TRANSIENT_BINARY_NAME + ";";

    private static final DotName DOTNAME_TRANSIENT = DotName.createSimple(Transient.class.getName());
    final Map<String, EntityModel> entities = new HashMap<>();

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new ModelEnhancingClassVisitor(className, outputClassVisitor, entities);
    }

    static class ModelEnhancingClassVisitor extends ClassVisitor {

        private Type thisClass;
        private Map<String, EntityField> fields;
        // set of name + "/" + descriptor (only for suspected accessor names)
        private Set<String> methods = new HashSet<>();
        private Map<String, EntityModel> entities;

        public ModelEnhancingClassVisitor(String className, ClassVisitor outputClassVisitor,
                Map<String, EntityModel> entities) {
            super(Opcodes.ASM6, outputClassVisitor);
            thisClass = Type.getType("L" + className.replace('.', '/') + ";");
            this.entities = entities;
            EntityModel entityModel = entities.get(className);
            fields = entityModel != null ? entityModel.fields : null;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            FieldVisitor superVisitor = super.visitField(access, name, descriptor, signature, value);
            if (fields == null || !fields.containsKey(name))
                return superVisitor;
            // if we have a mapped field, let's add some annotations
            return new FieldVisitor(Opcodes.ASM6, superVisitor) {
                private Set<String> descriptors = new HashSet<>();

                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    descriptors.add(descriptor);
                    return super.visitAnnotation(descriptor, visible);
                }

                @Override
                public void visitEnd() {
                    // add the @JaxbTransient property to the field so that Jackson prefers the generated getter
                    // jsonb will already use the getter so we're good
                    if (!descriptors.contains(JAXB_TRANSIENT_SIGNATURE))
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
                    || methodName.startsWith("is"))
                methods.add(methodName + "/" + descriptor);
            // FIXME: do not add method if already present
            MethodVisitor superVisitor = super.visitMethod(access, methodName, descriptor, signature, exceptions);
            return new PanacheFieldAccessMethodVisitor(superVisitor, thisClass.getInternalName(), methodName, descriptor,
                    entities);
        }

        @Override
        public void visitEnd() {
            // FIXME: generate default constructor

            generateMethod("findById",
                    "(Ljava/lang/Object;)" + ENTITY_BASE_SIGNATURE,
                    "<T:" + ENTITY_BASE_SIGNATURE + ">(Ljava/lang/Object;)TT;",
                    Opcodes.ARETURN, ENTITY_BASE_BINARY_NAME, "id");

            // find Sort? Map|Object[]|Parameters?

            generateMethod("find",
                    "(Ljava/lang/String;[Ljava/lang/Object;)" + QUERY_SIGNATURE,
                    "<T:" + ENTITY_BASE_SIGNATURE + ">(Ljava/lang/String;[Ljava/lang/Object;)L" + QUERY_BINARY_NAME + "<TT;>;",
                    Opcodes.ARETURN, null, "query", "params");

            generateMethod("find",
                    "(Ljava/lang/String;" + SORT_SIGNATURE + "[Ljava/lang/Object;)" + QUERY_SIGNATURE,
                    "<T:" + ENTITY_BASE_SIGNATURE + ">(Ljava/lang/String;" + SORT_SIGNATURE + "[Ljava/lang/Object;)L"
                            + QUERY_BINARY_NAME + "<TT;>;",
                    Opcodes.ARETURN, null, "query", "sort", "params");

            generateMethod("find",
                    "(Ljava/lang/String;Ljava/util/Map;)" + QUERY_SIGNATURE,
                    "<T:" + ENTITY_BASE_SIGNATURE
                            + ">(Ljava/lang/String;Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)L" + QUERY_BINARY_NAME
                            + "<TT;>;",
                    Opcodes.ARETURN, null, "query", "params");

            generateMethod("find",
                    "(Ljava/lang/String;" + SORT_SIGNATURE + "Ljava/util/Map;)" + QUERY_SIGNATURE,
                    "<T:" + ENTITY_BASE_SIGNATURE + ">(Ljava/lang/String;" + SORT_SIGNATURE
                            + "Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)L" + QUERY_BINARY_NAME + "<TT;>;",
                    Opcodes.ARETURN, null, "query", "sort", "params");

            generateMethod("find",
                    "(Ljava/lang/String;" + PARAMETERS_SIGNATURE + ")" + QUERY_SIGNATURE,
                    "<T:" + ENTITY_BASE_SIGNATURE + ">(Ljava/lang/String;" + PARAMETERS_SIGNATURE + ")L" + QUERY_BINARY_NAME
                            + "<TT;>;",
                    Opcodes.ARETURN, null, "query", "params");

            generateMethod("find",
                    "(Ljava/lang/String;" + SORT_SIGNATURE + PARAMETERS_SIGNATURE + ")" + QUERY_SIGNATURE,
                    "<T:" + ENTITY_BASE_SIGNATURE + ">(Ljava/lang/String;" + SORT_SIGNATURE + PARAMETERS_SIGNATURE + ")L"
                            + QUERY_BINARY_NAME + "<TT;>;",
                    Opcodes.ARETURN, null, "query", "sort", "params");

            // list Sort? Map|Object[]|Parameters?

            generateMethod("list",
                    "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/util/List;",
                    "<T:" + ENTITY_BASE_SIGNATURE + ">(Ljava/lang/String;[Ljava/lang/Object;)Ljava/util/List<TT;>;",
                    Opcodes.ARETURN, null, "query", "params");

            generateMethod("list",
                    "(Ljava/lang/String;" + SORT_SIGNATURE + "[Ljava/lang/Object;)Ljava/util/List;",
                    "<T:" + ENTITY_BASE_SIGNATURE + ">(Ljava/lang/String;" + SORT_SIGNATURE
                            + "[Ljava/lang/Object;)Ljava/util/List<TT;>;",
                    Opcodes.ARETURN, null, "query", "sort", "params");

            generateMethod("list",
                    "(Ljava/lang/String;Ljava/util/Map;)Ljava/util/List;",
                    "<T:" + ENTITY_BASE_SIGNATURE
                            + ">(Ljava/lang/String;Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)Ljava/util/List<TT;>;",
                    Opcodes.ARETURN, null, "query", "params");

            generateMethod("list",
                    "(Ljava/lang/String;" + SORT_SIGNATURE + "Ljava/util/Map;)Ljava/util/List;",
                    "<T:" + ENTITY_BASE_SIGNATURE + ">(Ljava/lang/String;" + SORT_SIGNATURE
                            + "Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)Ljava/util/List<TT;>;",
                    Opcodes.ARETURN, null, "query", "sort", "params");

            generateMethod("list",
                    "(Ljava/lang/String;" + PARAMETERS_SIGNATURE + ")Ljava/util/List;",
                    "<T:" + ENTITY_BASE_SIGNATURE + ">(Ljava/lang/String;" + PARAMETERS_SIGNATURE + ")Ljava/util/List<TT;>;",
                    Opcodes.ARETURN, null, "query", "params");

            generateMethod("list",
                    "(Ljava/lang/String;" + SORT_SIGNATURE + PARAMETERS_SIGNATURE + ")Ljava/util/List;",
                    "<T:" + ENTITY_BASE_SIGNATURE + ">(Ljava/lang/String;" + SORT_SIGNATURE + PARAMETERS_SIGNATURE
                            + ")Ljava/util/List<TT;>;",
                    Opcodes.ARETURN, null, "query", "sort", "params");

            // stream Sort? Map|Object[]|Parameters?

            generateMethod("stream",
                    "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/util/stream/Stream;",
                    "<T:" + ENTITY_BASE_SIGNATURE + ">(Ljava/lang/String;[Ljava/lang/Object;)Ljava/util/stream/Stream<TT;>;",
                    Opcodes.ARETURN, null, "query", "params");

            generateMethod("stream",
                    "(Ljava/lang/String;" + SORT_SIGNATURE + "[Ljava/lang/Object;)Ljava/util/stream/Stream;",
                    "<T:" + ENTITY_BASE_SIGNATURE + ">(Ljava/lang/String;" + SORT_SIGNATURE
                            + "[Ljava/lang/Object;)Ljava/util/stream/Stream<TT;>;",
                    Opcodes.ARETURN, null, "query", "sort", "params");

            generateMethod("stream",
                    "(Ljava/lang/String;Ljava/util/Map;)Ljava/util/stream/Stream;",
                    "<T:" + ENTITY_BASE_SIGNATURE
                            + ">(Ljava/lang/String;Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)Ljava/util/stream/Stream<TT;>;",
                    Opcodes.ARETURN, null, "query", "params");

            generateMethod("stream",
                    "(Ljava/lang/String;" + SORT_SIGNATURE + "Ljava/util/Map;)Ljava/util/stream/Stream;",
                    "<T:" + ENTITY_BASE_SIGNATURE + ">(Ljava/lang/String;" + SORT_SIGNATURE
                            + "Ljava/util/Map<Ljava/lang/String;Ljava/lang/Object;>;)Ljava/util/stream/Stream<TT;>;",
                    Opcodes.ARETURN, null, "query", "sort", "params");

            generateMethod("stream",
                    "(Ljava/lang/String;" + PARAMETERS_SIGNATURE + ")Ljava/util/stream/Stream;",
                    "<T:" + ENTITY_BASE_SIGNATURE + ">(Ljava/lang/String;" + PARAMETERS_SIGNATURE
                            + ")Ljava/util/stream/Stream<TT;>;",
                    Opcodes.ARETURN, null, "query", "params");

            generateMethod("stream",
                    "(Ljava/lang/String;" + SORT_SIGNATURE + PARAMETERS_SIGNATURE + ")Ljava/util/stream/Stream;",
                    "<T:" + ENTITY_BASE_SIGNATURE + ">(Ljava/lang/String;" + SORT_SIGNATURE + PARAMETERS_SIGNATURE
                            + ")Ljava/util/stream/Stream<TT;>;",
                    Opcodes.ARETURN, null, "query", "sort", "params");

            // findAll Sort?

            generateMethod("findAll",
                    "()" + QUERY_SIGNATURE,
                    "<T:" + ENTITY_BASE_SIGNATURE + ">()L" + QUERY_BINARY_NAME + "<TT;>;",
                    Opcodes.ARETURN, null);

            generateMethod("findAll",
                    "(" + SORT_SIGNATURE + ")" + QUERY_SIGNATURE,
                    "<T:" + ENTITY_BASE_SIGNATURE + ">(" + SORT_SIGNATURE + ")L" + QUERY_BINARY_NAME + "<TT;>;",
                    Opcodes.ARETURN, null, "sort");

            // listAll Sort?

            generateMethod("listAll",
                    "()Ljava/util/List;",
                    "<T:" + ENTITY_BASE_SIGNATURE + ">()Ljava/util/List<TT;>;",
                    Opcodes.ARETURN, null);

            generateMethod("listAll",
                    "(" + SORT_SIGNATURE + ")Ljava/util/List;",
                    "<T:" + ENTITY_BASE_SIGNATURE + ">(" + SORT_SIGNATURE + ")Ljava/util/List<TT;>;",
                    Opcodes.ARETURN, null, "sort");

            // streamAll Sort?

            generateMethod("streamAll",
                    "()Ljava/util/stream/Stream;",
                    "<T:" + ENTITY_BASE_SIGNATURE + ">()Ljava/util/stream/Stream<TT;>;",
                    Opcodes.ARETURN, null);

            generateMethod("streamAll",
                    "(" + SORT_SIGNATURE + ")Ljava/util/stream/Stream;",
                    "<T:" + ENTITY_BASE_SIGNATURE + ">(" + SORT_SIGNATURE + ")Ljava/util/stream/Stream<TT;>;",
                    Opcodes.ARETURN, null, "sort");

            // count [String, Map|Object[]|Parameters?]?

            generateMethod("count", "(Ljava/lang/String;[Ljava/lang/Object;)J", null, Opcodes.LRETURN, null, "query", "params");
            generateMethod("count", "(Ljava/lang/String;Ljava/util/Map;)J", null, Opcodes.LRETURN, null, "query", "params");
            generateMethod("count", "(Ljava/lang/String;" + PARAMETERS_SIGNATURE + ")J", null, Opcodes.LRETURN, null, "query",
                    "params");
            generateMethod("count", "()J", null, Opcodes.LRETURN, null);

            // delete [String, Map|Object[]|Parameters?]?

            generateMethod("delete", "(Ljava/lang/String;[Ljava/lang/Object;)J", null, Opcodes.LRETURN, null, "query",
                    "params");
            generateMethod("delete", "(Ljava/lang/String;Ljava/util/Map;)J", null, Opcodes.LRETURN, null, "query", "params");
            generateMethod("delete", "(Ljava/lang/String;" + PARAMETERS_SIGNATURE + ")J", null, Opcodes.LRETURN, null, "query",
                    "params");
            generateMethod("deleteAll", "()J", null, Opcodes.LRETURN, null);

            generateAccessors();

            super.visitEnd();
        }

        private void generateMethod(String name,
                String descriptor,
                String signature,
                int returnOperation,
                String castTo,
                String... parameters) {
            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    name,
                    descriptor,
                    signature,
                    null);
            for (int i = 0; i < parameters.length; i++) {
                mv.visitParameter(parameters[i], 0 /* modifiers */);
            }
            mv.visitCode();
            // inject Class
            mv.visitLdcInsn(thisClass);
            for (int i = 0; i < parameters.length; i++) {
                mv.visitIntInsn(Opcodes.ALOAD, i);
            }
            // inject Class
            String forwardingDescriptor = "(Ljava/lang/Class;" + descriptor.substring(1);
            if (castTo != null) {
                // return type is erased to Object
                int lastParen = forwardingDescriptor.lastIndexOf(')');
                forwardingDescriptor = forwardingDescriptor.substring(0, lastParen + 1) + "Ljava/lang/Object;";
            }
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    JPA_OPERATIONS_BINARY_NAME,
                    name,
                    forwardingDescriptor, false);
            if (castTo != null)
                mv.visitTypeInsn(Opcodes.CHECKCAST, castTo);
            mv.visitInsn(returnOperation);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }

        private void generateAccessors() {
            if (fields == null)
                return;
            for (EntityField field : fields.values()) {
                // Getter
                String getterName = field.getGetterName();
                String getterDescriptor = "()" + field.descriptor;
                if (!methods.contains(getterName + "/" + getterDescriptor)) {
                    MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                            getterName, getterDescriptor, null, null);
                    mv.visitCode();
                    mv.visitIntInsn(Opcodes.ALOAD, 0);
                    // Due to https://github.com/quarkusio/quarkus/issues/1376 we generate Hibernate read/write calls
                    // directly rather than rely on Hibernate to see our generated accessor because it does not
                    mv.visitMethodInsn(
                            Opcodes.INVOKEVIRTUAL,
                            thisClass.getInternalName(),
                            EnhancerConstants.PERSISTENT_FIELD_READER_PREFIX + field.name,
                            Type.getMethodDescriptor(Type.getType(field.descriptor)),
                            false);
                    // instead of:
                    //                    mv.visitFieldInsn(Opcodes.GETFIELD, thisClass.getInternalName(), field.name, field.descriptor);
                    int returnCode;
                    switch (field.descriptor) {
                        case "Z":
                        case "B":
                        case "C":
                        case "S":
                        case "I":
                            returnCode = Opcodes.IRETURN;
                            break;
                        case "J":
                            returnCode = Opcodes.LRETURN;
                            break;
                        case "F":
                            returnCode = Opcodes.FRETURN;
                            break;
                        case "D":
                            returnCode = Opcodes.DRETURN;
                            break;
                        default:
                            returnCode = Opcodes.ARETURN;
                            break;
                    }
                    mv.visitInsn(returnCode);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }

                // Setter
                String setterName = field.getSetterName();
                String setterDescriptor = "(" + field.descriptor + ")V";
                if (!methods.contains(setterName + "/" + setterDescriptor)) {
                    MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                            setterName, setterDescriptor, null, null);
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
                    // Due to https://github.com/quarkusio/quarkus/issues/1376 we generate Hibernate read/write calls
                    // directly rather than rely on Hibernate to see our generated accessor because it does not
                    mv.visitMethodInsn(
                            Opcodes.INVOKEVIRTUAL,
                            thisClass.getInternalName(),
                            EnhancerConstants.PERSISTENT_FIELD_WRITER_PREFIX + field.name,
                            Type.getMethodDescriptor(Type.getType(void.class), Type.getType(field.descriptor)),
                            false);
                    // instead of:
                    //                    mv.visitFieldInsn(Opcodes.PUTFIELD, thisClass.getInternalName(), field.name, field.descriptor);
                    mv.visitInsn(Opcodes.RETURN);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }
            }
        }
    }

    public void collectFields(ClassInfo classInfo) {
        Map<String, EntityField> fields = new HashMap<>();
        for (FieldInfo fieldInfo : classInfo.fields()) {
            String name = fieldInfo.name();
            if (Modifier.isPublic(fieldInfo.flags())
                    && !fieldInfo.hasAnnotation(DOTNAME_TRANSIENT)) {
                fields.put(name, new EntityField(name, DescriptorUtils.typeToString(fieldInfo.type())));
            }
        }
        entities.put(classInfo.name().toString(), new EntityModel(classInfo, fields));
    }
}
