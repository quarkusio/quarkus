package io.quarkus.mongodb.panache.kotlin.deployment;

import java.util.Collections;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;
import io.quarkus.panache.common.deployment.TypeBundle;
import io.quarkus.panache.common.deployment.visitors.KotlinPanacheClassVisitor;

public class KotlinPanacheMongoRepositoryEnhancer extends PanacheRepositoryEnhancer {
    private final TypeBundle types;

    public KotlinPanacheMongoRepositoryEnhancer(IndexView index, TypeBundle types) {
        super(index);
        this.types = types;
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new KotlinPanacheClassVisitor(outputClassVisitor,
                indexView.getClassByName(DotName.createSimple(className)), indexView, types,
                types.repositoryBase(), Collections.emptyList());
    }
}
