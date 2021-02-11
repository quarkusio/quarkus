package io.quarkus.mongodb.panache.deployment;

import static io.quarkus.mongodb.panache.deployment.BasePanacheMongoResourceProcessor.BSON_IGNORE;
import static org.jboss.jandex.DotName.createSimple;

import java.lang.reflect.Modifier;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.gizmo.DescriptorUtils;
import io.quarkus.mongodb.panache.deployment.visitors.PanacheMongoEntityClassVisitor;
import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.MetamodelInfo;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.TypeBundle;

public class PanacheMongoEntityEnhancer extends PanacheEntityEnhancer<MetamodelInfo<EntityModel<EntityField>>> {

    private final TypeBundle typeBundle;

    public PanacheMongoEntityEnhancer(IndexView index, List<PanacheMethodCustomizer> methodCustomizers,
            TypeBundle typeBundle) {
        super(index, methodCustomizers);
        this.typeBundle = typeBundle;
        modelInfo = new MetamodelInfo<>();
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new PanacheMongoEntityClassVisitor(outputClassVisitor, modelInfo,
                indexView.getClassByName(createSimple(className)), methodCustomizers, typeBundle, indexView);
    }

    @Override
    public void collectFields(ClassInfo classInfo) {
        EntityModel<EntityField> entityModel = new EntityModel<>(classInfo);
        for (FieldInfo fieldInfo : classInfo.fields()) {
            String name = fieldInfo.name();
            if (Modifier.isPublic(fieldInfo.flags()) && !fieldInfo.hasAnnotation(BSON_IGNORE)) {
                entityModel.addField(new EntityField(name, DescriptorUtils.typeToString(fieldInfo.type())));
            }
        }
        modelInfo.addEntityModel(entityModel);
    }
}
