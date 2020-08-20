package io.quarkus.mongodb.panache.kotlin.deployment;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.mongodb.panache.deployment.TypeBundle;
import io.quarkus.mongodb.panache.kotlin.deployment.visitors.KotlinPanacheMongoRepositoryClassVisitor;
import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;

public class KotlinPanacheMongoRepositoryEnhancer extends PanacheRepositoryEnhancer {
    private TypeBundle types;

    public KotlinPanacheMongoRepositoryEnhancer(IndexView index, TypeBundle types) {
        super(index, types.repositoryBase().dotName());
        this.types = types;
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new KotlinPanacheMongoRepositoryClassVisitor(this.indexView, outputClassVisitor, className, types);
    }

    @Override
    public boolean skipRepository(ClassInfo classInfo) {
        return false;
    }
}
