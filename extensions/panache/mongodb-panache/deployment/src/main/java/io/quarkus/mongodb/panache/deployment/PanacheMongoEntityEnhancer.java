package io.quarkus.mongodb.panache.deployment;

import static io.quarkus.mongodb.panache.deployment.BasePanacheMongoResourceProcessor.BSON_IGNORE;

import java.lang.reflect.Modifier;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.mongodb.panache.deployment.visitors.PanacheMongoEntityClassAccessorGenerationVisitor;
import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.MetamodelInfo;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.TypeBundle;
import io.quarkus.panache.common.deployment.visitors.PanacheEntityClassOperationGenerationVisitor;

public class PanacheMongoEntityEnhancer extends PanacheEntityEnhancer {

    private final TypeBundle typeBundle;

    public PanacheMongoEntityEnhancer(IndexView index, List<PanacheMethodCustomizer> methodCustomizers,
            TypeBundle typeBundle) {
        super(index, methodCustomizers);
        this.typeBundle = typeBundle;
        modelInfo = new MetamodelInfo();
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        ClassInfo entityInfo = indexView.getClassByName(DotName.createSimple(className));
        EntityModel entityModel = modelInfo.getEntityModel(className);
        outputClassVisitor = new PanacheMongoEntityClassAccessorGenerationVisitor(outputClassVisitor, entityInfo, entityModel);
        return new PanacheEntityClassOperationGenerationVisitor(outputClassVisitor, typeBundle,
                entityInfo, methodCustomizers, indexView);
    }

    @Override
    public void collectFields(ClassInfo classInfo) {
        EntityModel entityModel = new EntityModel(classInfo);
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
