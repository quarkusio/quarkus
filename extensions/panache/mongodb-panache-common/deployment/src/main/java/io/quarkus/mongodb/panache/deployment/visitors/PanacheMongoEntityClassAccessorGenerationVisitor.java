package io.quarkus.mongodb.panache.deployment.visitors;

import org.jboss.jandex.ClassInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.visitors.PanacheEntityClassAccessorGenerationVisitor;

public class PanacheMongoEntityClassAccessorGenerationVisitor extends PanacheEntityClassAccessorGenerationVisitor {

    public PanacheMongoEntityClassAccessorGenerationVisitor(ClassVisitor outputClassVisitor,
            ClassInfo entityInfo, EntityModel entityModel) {
        super(outputClassVisitor, entityInfo, entityModel);
    }

    @Override
    protected void generateAccessorSetField(MethodVisitor mv, EntityField field) {
        mv.visitFieldInsn(Opcodes.PUTFIELD, thisClass.getInternalName(), field.name, field.descriptor);
    }

    @Override
    protected void generateAccessorGetField(MethodVisitor mv, EntityField field) {
        mv.visitFieldInsn(Opcodes.GETFIELD, thisClass.getInternalName(), field.name, field.descriptor);
    }
}
