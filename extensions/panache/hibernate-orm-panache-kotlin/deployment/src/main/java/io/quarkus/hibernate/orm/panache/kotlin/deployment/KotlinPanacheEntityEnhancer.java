package io.quarkus.hibernate.orm.panache.kotlin.deployment;

import static io.quarkus.hibernate.orm.panache.kotlin.deployment.KotlinPanacheResourceProcessor.PANACHE_ENTITY_BASE;
import static io.quarkus.hibernate.orm.panache.kotlin.deployment.KotlinPanacheResourceProcessor.TRANSIENT;

import java.lang.reflect.Modifier;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.hibernate.orm.panache.kotlin.runtime.JpaOperations;
import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.MetamodelInfo;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;

public class KotlinPanacheEntityEnhancer extends PanacheEntityEnhancer<MetamodelInfo<EntityModel<EntityField>>> {

    private final static String JPA_OPERATIONS_NAME = JpaOperations.class.getName();
    public final static String JPA_OPERATIONS_BINARY_NAME = JPA_OPERATIONS_NAME.replace('.', '/');

    public KotlinPanacheEntityEnhancer(IndexView index, List<PanacheMethodCustomizer> methodCustomizers) {
        super(index, methodCustomizers);
        modelInfo = new MetamodelInfo<>();
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new KotlinPanacheEntityClassVisitor(className, outputClassVisitor, modelInfo,
                indexView.getClassByName(PANACHE_ENTITY_BASE),
                indexView.getClassByName(DotName.createSimple(className)), methodCustomizers);
    }

    public void collectFields(ClassInfo classInfo) {
        EntityModel<EntityField> entityModel = new EntityModel<>(classInfo);
        for (FieldInfo fieldInfo : classInfo.fields()) {
            String name = fieldInfo.name();
            if (Modifier.isPublic(fieldInfo.flags())
                    && !Modifier.isStatic(fieldInfo.flags())
                    && !fieldInfo.hasAnnotation(TRANSIENT)) {
                entityModel.addField(new EntityField(name, DescriptorUtils.typeToString(fieldInfo.type())));
            }
        }
        modelInfo.addEntityModel(entityModel);
    }
}
