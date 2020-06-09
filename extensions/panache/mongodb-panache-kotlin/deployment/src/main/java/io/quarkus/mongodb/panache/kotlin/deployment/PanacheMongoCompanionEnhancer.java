package io.quarkus.mongodb.panache.kotlin.deployment;

import static io.quarkus.mongodb.panache.deployment.BasePanacheMongoResourceProcessor.BSON_IGNORE;

import java.lang.reflect.Modifier;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.mongodb.panache.deployment.TypeBundle;
import io.quarkus.mongodb.panache.kotlin.deployment.visitors.PanacheMongoCompanionClassVisitor;
import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.MetamodelInfo;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;

public class PanacheMongoCompanionEnhancer extends PanacheEntityEnhancer<MetamodelInfo<EntityModel<EntityField>>> {
    private TypeBundle types;

    public PanacheMongoCompanionEnhancer(IndexView index, List<PanacheMethodCustomizer> methodCustomizers,
            TypeBundle types) {
        super(index, methodCustomizers);
        this.types = types;
        modelInfo = new MetamodelInfo<>();
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new PanacheMongoCompanionClassVisitor(outputClassVisitor,
                indexView.getClassByName(DotName.createSimple(className)), indexView, types);
    }

    @Override
    public void collectFields(ClassInfo classInfo) {
        EntityModel<EntityField> entityModel = new EntityModel<>(classInfo);
        for (FieldInfo fieldInfo : classInfo.fields()) {
            String name = fieldInfo.name();
            if (Modifier.isPublic(fieldInfo.flags())
                    && !Modifier.isStatic(fieldInfo.flags())
                    && !fieldInfo.hasAnnotation(BSON_IGNORE)) {
                entityModel.addField(new EntityField(name, DescriptorUtils.typeToString(fieldInfo.type())));
            }
        }
        modelInfo.addEntityModel(entityModel);
    }

}
