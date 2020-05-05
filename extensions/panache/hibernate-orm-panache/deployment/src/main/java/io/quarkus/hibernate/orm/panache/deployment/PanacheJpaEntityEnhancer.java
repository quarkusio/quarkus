package io.quarkus.hibernate.orm.panache.deployment;

import java.lang.reflect.Modifier;
import java.util.List;

import javax.persistence.Transient;

import org.hibernate.bytecode.enhance.spi.EnhancerConstants;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.MetamodelInfo;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;

public class PanacheJpaEntityEnhancer extends PanacheEntityEnhancer<MetamodelInfo<EntityModel<EntityField>>> {

    public static final String ENTITY_BASE_NAME = PanacheEntityBase.class.getName();
    public static final String ENTITY_BASE_BINARY_NAME = ENTITY_BASE_NAME.replace('.', '/');
    public static final String ENTITY_BASE_SIGNATURE = "L" + ENTITY_BASE_BINARY_NAME + ";";

    public static final String QUERY_NAME = PanacheQuery.class.getName();
    public static final String QUERY_BINARY_NAME = QUERY_NAME.replace('.', '/');
    public static final String QUERY_SIGNATURE = "L" + QUERY_BINARY_NAME + ";";

    public static final String JPA_OPERATIONS_NAME = JpaOperations.class.getName();
    public static final String JPA_OPERATIONS_BINARY_NAME = JPA_OPERATIONS_NAME.replace('.', '/');

    private static final DotName DOTNAME_TRANSIENT = DotName.createSimple(Transient.class.getName());

    public PanacheJpaEntityEnhancer(IndexView index, List<PanacheMethodCustomizer> methodCustomizers) {
        super(index, PanacheResourceProcessor.DOTNAME_PANACHE_ENTITY_BASE, methodCustomizers);
        modelInfo = new MetamodelInfo<>();
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new PanacheJpaEntityClassVisitor(className, outputClassVisitor, modelInfo, panacheEntityBaseClassInfo,
                indexView.getClassByName(DotName.createSimple(className)), methodCustomizers);
    }

    static class PanacheJpaEntityClassVisitor extends PanacheEntityClassVisitor<EntityField> {

        public PanacheJpaEntityClassVisitor(String className, ClassVisitor outputClassVisitor,
                MetamodelInfo<EntityModel<EntityField>> modelInfo,
                ClassInfo panacheEntityBaseClassInfo,
                ClassInfo entityInfo,
                List<PanacheMethodCustomizer> methodCustomizers) {
            super(className, outputClassVisitor, modelInfo, panacheEntityBaseClassInfo, entityInfo, methodCustomizers);
        }

        @Override
        protected void injectModel(MethodVisitor mv) {
            mv.visitLdcInsn(thisClass);
        }

        @Override
        protected String getModelDescriptor() {
            return "Ljava/lang/Class;";
        }

        @Override
        protected String getPanacheOperationsBinaryName() {
            return JPA_OPERATIONS_BINARY_NAME;
        }

        @Override
        protected void generateAccessorSetField(MethodVisitor mv, EntityField field) {
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
        }

        @Override
        protected void generateAccessorGetField(MethodVisitor mv, EntityField field) {
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
        }
    }

    public void collectFields(ClassInfo classInfo) {
        EntityModel<EntityField> entityModel = new EntityModel<>(classInfo);
        for (FieldInfo fieldInfo : classInfo.fields()) {
            String name = fieldInfo.name();
            if (Modifier.isPublic(fieldInfo.flags())
                    && !fieldInfo.hasAnnotation(DOTNAME_TRANSIENT)) {
                entityModel.addField(new EntityField(name, DescriptorUtils.typeToString(fieldInfo.type())));
            }
        }
        modelInfo.addEntityModel(entityModel);
    }
}
