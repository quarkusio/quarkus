package io.quarkus.mongodb.panache.deployment;

import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.MetamodelInfo;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.TypeBundle;
import io.quarkus.panache.common.deployment.visitors.PanacheEntityClassAccessorGenerationVisitor;
import io.quarkus.panache.common.deployment.visitors.PanacheEntityClassOperationGenerationVisitor;

public class PanacheMongoEntityEnhancer extends PanacheEntityEnhancer {

    private final TypeBundle typeBundle;
    private final MetamodelInfo modelInfo;

    public PanacheMongoEntityEnhancer(IndexView index, List<PanacheMethodCustomizer> methodCustomizers,
            TypeBundle typeBundle, MetamodelInfo modelInfo) {
        super(index, methodCustomizers);
        this.typeBundle = typeBundle;
        this.modelInfo = modelInfo;
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        ClassInfo entityInfo = indexView.getClassByName(DotName.createSimple(className));
        EntityModel entityModel = modelInfo.getEntityModel(className);
        outputClassVisitor = new PanacheEntityClassAccessorGenerationVisitor(outputClassVisitor, entityInfo, entityModel);
        return new PanacheEntityClassOperationGenerationVisitor(outputClassVisitor, typeBundle,
                entityInfo, methodCustomizers, indexView);
    }
}
