package org.jboss.shamrock.panache;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import javax.persistence.Transient;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.panache.jpa.EntityBase;
import org.jboss.panache.jpa.JpaOperations;
import org.jboss.protean.gizmo.DescriptorUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class PanacheJpaModelEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    public final static String ENTITY_BASE_NAME = EntityBase.class.getName();
    public final static String ENTITY_BASE_BINARY_NAME = ENTITY_BASE_NAME.replace('.', '/');
    public final static String ENTITY_BASE_SIGNATURE = "L"+ENTITY_BASE_BINARY_NAME+";";

    public final static String JPA_OPERATIONS_NAME = JpaOperations.class.getName();
    public final static String JPA_OPERATIONS_BINARY_NAME = JPA_OPERATIONS_NAME.replace('.', '/');
    public final static String JPA_OPERATIONS_SIGNATURE = "L"+JPA_OPERATIONS_BINARY_NAME+";";
    
    private static final DotName DOTNAME_TRANSIENT = DotName.createSimple(Transient.class.getName());
    final Map<String, EntityModel> entities = new HashMap<>();

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new ModelEnhancingClassVisitor(className, outputClassVisitor, entities);
    }

    static class ModelEnhancingClassVisitor extends ClassVisitor {

        private Type thisClass;
        private Map<String,EntityField> fields;
        // set of name + "/" + descriptor (only for suspected accessor names)
        private Set<String> methods = new HashSet<>();
        private Map<String, EntityModel> entities;

        public ModelEnhancingClassVisitor(String className, ClassVisitor outputClassVisitor, Map<String, EntityModel> entities) {
            super(Opcodes.ASM6, outputClassVisitor);
            thisClass = Type.getType("L"+className.replace('.', '/')+";");
            this.entities = entities;
            EntityModel entityModel = entities.get(className);
            fields = entityModel != null ? entityModel.fields : null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature, String[] exceptions) {
            if(methodName.startsWith("get")
                    || methodName.startsWith("set")
                    || methodName.startsWith("is"))
                methods.add(methodName + "/" + descriptor);
            // FIXME: do not add method if already present 
            MethodVisitor superVisitor = super.visitMethod(access, methodName, descriptor, signature, exceptions);
            return new PanacheFieldAccessMethodVisitor(superVisitor, thisClass.getInternalName(), methodName, descriptor, entities);
        }

        @Override
        public void visitEnd() {
            // FIXME: generate default constructor
            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, 
                    "findById", 
                    "(Ljava/lang/Object;)"+ENTITY_BASE_SIGNATURE+"", 
                    "<T:"+ENTITY_BASE_SIGNATURE+">(Ljava/lang/Object;)TT;", 
                    null);
            mv.visitParameter("id", 0);
            mv.visitCode();
            mv.visitLdcInsn(thisClass);
            mv.visitIntInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    JPA_OPERATIONS_BINARY_NAME,
                    "findById",
                    "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/Object;", false);
            mv.visitTypeInsn(Opcodes.CHECKCAST, ENTITY_BASE_BINARY_NAME);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    "find",
                    "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/util/List;",
                    "<T:"+ENTITY_BASE_SIGNATURE+">(Ljava/lang/String;[Ljava/lang/Object;)Ljava/util/List<TT;>;",
                    null);
            mv.visitParameter("query", 0);
            mv.visitParameter("params", 0);
            mv.visitCode();
            mv.visitLdcInsn(thisClass);
            mv.visitIntInsn(Opcodes.ALOAD, 0);
            mv.visitIntInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    JPA_OPERATIONS_BINARY_NAME,
                    "find",
                    "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Object;)Ljava/util/List;", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    "findAll",
                    "()Ljava/util/List;",
                    "<T:"+ENTITY_BASE_SIGNATURE+">()Ljava/util/List<TT;>;",
                    null);
            mv.visitCode();
            mv.visitLdcInsn(thisClass);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    JPA_OPERATIONS_BINARY_NAME,
                    "findAll",
                    "(Ljava/lang/Class;)Ljava/util/List;", false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    "count",
                    "()J",
                    null,
                    null);
            mv.visitCode();
            mv.visitLdcInsn(thisClass);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    JPA_OPERATIONS_BINARY_NAME,
                    "count",
                    "(Ljava/lang/Class;)J", false);
            mv.visitInsn(Opcodes.LRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    "count",
                    "(Ljava/lang/String;[Ljava/lang/Object;)J",
                    null,
                    null);
            mv.visitParameter("query", 0);
            mv.visitParameter("params", 0);
            mv.visitCode();
            mv.visitLdcInsn(thisClass);
            mv.visitIntInsn(Opcodes.ALOAD, 0);
            mv.visitIntInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    JPA_OPERATIONS_BINARY_NAME,
                    "count",
                    "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Object;)J", false);
            mv.visitInsn(Opcodes.LRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    "delete",
                    "(Ljava/lang/String;[Ljava/lang/Object;)J",
                    null,
                    null);
            mv.visitParameter("query", 0);
            mv.visitParameter("params", 0);
            mv.visitCode();
            mv.visitLdcInsn(thisClass);
            mv.visitIntInsn(Opcodes.ALOAD, 0);
            mv.visitIntInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    JPA_OPERATIONS_BINARY_NAME,
                    "delete",
                    "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Object;)J", false);
            mv.visitInsn(Opcodes.LRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC,
                    "deleteAll",
                    "()J",
                    null,
                    null);
            mv.visitCode();
            mv.visitLdcInsn(thisClass);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    JPA_OPERATIONS_BINARY_NAME,
                    "deleteAll",
                    "(Ljava/lang/Class;)J", false);
            mv.visitInsn(Opcodes.LRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();

            generateAccessors();
            
            super.visitEnd();
        }

        private void generateAccessors() {
            if(fields == null)
                return;
            for (EntityField field : fields.values()) {
                // Getter
                String getterName = field.getGetterName();
                String getterDescriptor = "()"+field.descriptor;
                if(!methods.contains(getterName + "/" + getterDescriptor)) {
                    MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, 
                                                         getterName, getterDescriptor, null, null);
                    mv.visitCode();
                    mv.visitIntInsn(Opcodes.ALOAD, 0);
                    mv.visitFieldInsn(Opcodes.GETFIELD, thisClass.getInternalName(), field.name, field.descriptor);
                    int returnCode;
                    switch(field.descriptor) {
                    case "Z":
                    case "B":
                    case "C":
                    case "S":
                    case "I":
                        returnCode = Opcodes.IRETURN; break;
                    case "J": returnCode = Opcodes.LRETURN; break;
                    case "F": returnCode = Opcodes.FRETURN; break;
                    case "D": returnCode = Opcodes.DRETURN; break;
                    default: returnCode = Opcodes.ARETURN; break;
                    }
                    mv.visitInsn(returnCode);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }
                
                // Setter
                String setterName = field.getSetterName();
                String setterDescriptor = "("+field.descriptor+")V";
                if(!methods.contains(setterName + "/" + setterDescriptor)) {
                    MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, 
                                                         setterName, setterDescriptor, null, null);
                    mv.visitCode();
                    mv.visitIntInsn(Opcodes.ALOAD, 0);
                    int loadCode;
                    switch(field.descriptor) {
                    case "Z":
                    case "B":
                    case "C":
                    case "S":
                    case "I":
                        loadCode = Opcodes.ILOAD; break;
                    case "J": loadCode = Opcodes.LLOAD; break;
                    case "F": loadCode = Opcodes.FLOAD; break;
                    case "D": loadCode = Opcodes.DLOAD; break;
                    default: loadCode = Opcodes.ALOAD; break;
                    }
                    mv.visitIntInsn(loadCode, 1);
                    mv.visitFieldInsn(Opcodes.PUTFIELD, thisClass.getInternalName(), field.name, field.descriptor);
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
            if(Modifier.isPublic(fieldInfo.flags())
                    && !fieldInfo.hasAnnotation(DOTNAME_TRANSIENT)) {
                fields.put(name, new EntityField(name, DescriptorUtils.typeToString(fieldInfo.type())));
            }
        }
        entities.put(classInfo.name().toString(), new EntityModel(classInfo, fields));
    }
}
