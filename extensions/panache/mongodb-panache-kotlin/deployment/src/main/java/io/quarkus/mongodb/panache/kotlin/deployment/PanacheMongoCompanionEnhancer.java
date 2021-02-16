package io.quarkus.mongodb.panache.kotlin.deployment;

import static java.util.Collections.emptyList;

import java.util.List;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.panache.common.deployment.PanacheCompanionEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.TypeBundle;
import io.quarkus.panache.common.deployment.visitors.KotlinPanacheClassVisitor;

public class PanacheMongoCompanionEnhancer extends PanacheCompanionEnhancer {
    private final TypeBundle types;

    public PanacheMongoCompanionEnhancer(IndexView index, List<PanacheMethodCustomizer> methodCustomizers,
            TypeBundle types) {
        super(index, methodCustomizers);
        this.types = types;
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new KotlinPanacheClassVisitor(outputClassVisitor,
                indexView.getClassByName(DotName.createSimple(className)), indexView, types,
                types.entityCompanionBase(), emptyList());
    }

}
