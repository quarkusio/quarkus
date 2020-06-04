package io.quarkus.hibernate.orm.panache.kotlin.deployment;

import java.util.List;
import java.util.StringJoiner;

import org.jboss.jandex.ClassInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.MetamodelInfo;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;

/**
 * This visitor process kotlin entities and removes the final modifier from the compiler generated getters and setters.
 * Unlike Java entities, we don't need to generate the getters and setters because kotlinc does that for us but kotlin,
 * by default, is final so we need to open those up for hibernate to add its hooks.
 */
class KotlinPanacheEntityClassVisitor extends PanacheEntityEnhancer.PanacheEntityClassVisitor<EntityField> {

    private String entityBinaryType;
    private org.objectweb.asm.Type entityType;

    public KotlinPanacheEntityClassVisitor(String className, ClassVisitor outputClassVisitor,
            MetamodelInfo<EntityModel<EntityField>> modelInfo,
            ClassInfo panacheEntityBaseClassInfo,
            ClassInfo entityInfo,
            List<PanacheMethodCustomizer> methodCustomizers) {
        super(className, outputClassVisitor, modelInfo, panacheEntityBaseClassInfo, entityInfo, methodCustomizers);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);

        entityBinaryType = name.replace('.', '/');
        entityType = org.objectweb.asm.Type.getType("L" + entityBinaryType + ";");
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        int updated = access;
        if (name.startsWith("get") || name.startsWith("set")) {
            updated &= ~Opcodes.ACC_FINAL;
        }
        return super.visitMethod(updated, name, descriptor, signature, exceptions);
    }

    @Override
    protected String getModelDescriptor() {
        return "Ljava/lang/Class;";
    }

    @Override
    protected String getPanacheOperationsBinaryName() {
        return KotlinPanacheEntityEnhancer.JPA_OPERATIONS_BINARY_NAME;
    }

    @Override
    protected void injectModel(MethodVisitor mv) {
        mv.visitLdcInsn(entityType);
    }

    @Override
    protected void generateAccessors() {
    }

    @Override
    protected void generateAccessorSetField(MethodVisitor mv, EntityField field) {
        throw new UnsupportedOperationException("generateAccessorSetField has not yet been implemented.");
    }

    @Override
    protected void generateAccessorGetField(MethodVisitor mv, EntityField field) {
        throw new UnsupportedOperationException("generateAccessorGetField has not yet been implemented.");
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", KotlinPanacheEntityClassVisitor.class.getSimpleName() + "[", "]")
                .add(entityBinaryType)
                .toString();
    }
}
