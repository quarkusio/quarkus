package io.quarkus.hibernate.orm.panache.kotlin.deployment;

import static io.quarkus.hibernate.orm.panache.kotlin.deployment.KotlinJpaTypeBundle.BUNDLE;

import java.lang.reflect.Modifier;
import java.util.List;

import javax.persistence.Transient;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.MetamodelInfo;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.visitors.KotlinPanacheClassVisitor;

public class KotlinPanacheCompanionEnhancer extends PanacheEntityEnhancer<MetamodelInfo<EntityModel<EntityField>>> {

    private static final DotName DOTNAME_TRANSIENT = DotName.createSimple(Transient.class.getName());

    public KotlinPanacheCompanionEnhancer(IndexView index, List<PanacheMethodCustomizer> methodCustomizers) {
        super(index, methodCustomizers);
        modelInfo = new MetamodelInfo<>();
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new KotlinPanacheClassVisitor(outputClassVisitor,
                indexView.getClassByName(DotName.createSimple(className)), indexView, BUNDLE,
                BUNDLE.entityCompanionBase(), methodCustomizers);
    }

    @Override
    public void collectFields(ClassInfo classInfo) {
        EntityModel<EntityField> entityModel = new EntityModel<>(classInfo);
        for (FieldInfo fieldInfo : classInfo.fields()) {
            String name = fieldInfo.name();
            if (Modifier.isPublic(fieldInfo.flags())
                    && !Modifier.isStatic(fieldInfo.flags())
                    && !fieldInfo.hasAnnotation(DOTNAME_TRANSIENT)) {
                entityModel.addField(new EntityField(name, DescriptorUtils.typeToString(fieldInfo.type())));
            }
        }
        modelInfo.addEntityModel(entityModel);
    }
}
