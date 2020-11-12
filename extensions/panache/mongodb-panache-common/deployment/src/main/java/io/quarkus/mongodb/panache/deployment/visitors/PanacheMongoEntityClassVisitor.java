package io.quarkus.mongodb.panache.deployment.visitors;

import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.MetamodelInfo;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.TypeBundle;
import io.quarkus.panache.common.deployment.visitors.PanacheEntityClassVisitor;

public class PanacheMongoEntityClassVisitor extends PanacheEntityClassVisitor<EntityField> {

    public PanacheMongoEntityClassVisitor(ClassVisitor outputClassVisitor,
            MetamodelInfo<EntityModel<EntityField>> modelInfo,
            ClassInfo entityInfo,
            List<PanacheMethodCustomizer> methodCustomizers,
            TypeBundle typeBundle, IndexView indexView) {
        super(outputClassVisitor, modelInfo, typeBundle, entityInfo, methodCustomizers, indexView);
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
