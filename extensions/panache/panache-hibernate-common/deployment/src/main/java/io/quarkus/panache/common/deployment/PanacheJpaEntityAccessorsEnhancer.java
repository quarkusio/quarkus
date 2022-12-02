package io.quarkus.panache.common.deployment;

import java.util.function.BiFunction;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.panache.common.deployment.visitors.PanacheEntityClassAccessorGenerationVisitor;

public class PanacheJpaEntityAccessorsEnhancer
        implements BiFunction<String, ClassVisitor, ClassVisitor> {

    private final IndexView indexView;
    private final MetamodelInfo modelInfo;

    public PanacheJpaEntityAccessorsEnhancer(IndexView index, MetamodelInfo modelInfo) {
        this.indexView = index;
        this.modelInfo = modelInfo;
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        ClassInfo entityInfo = indexView.getClassByName(DotName.createSimple(className));
        EntityModel entityModel = modelInfo.getEntityModel(className);
        return new PanacheEntityClassAccessorGenerationVisitor(outputClassVisitor, entityInfo, entityModel);
    }
}
