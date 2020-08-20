package io.quarkus.mongodb.panache.deployment;

import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.mongodb.panache.deployment.visitors.PanacheMongoRepositoryClassVisitor;
import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;

public class PanacheMongoRepositoryEnhancer extends PanacheRepositoryEnhancer {
    private final TypeBundle typeBundle;

    public PanacheMongoRepositoryEnhancer(IndexView index, TypeBundle typeBundle) {
        super(index, typeBundle.repositoryBase().dotName());
        this.typeBundle = typeBundle;
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new PanacheMongoRepositoryClassVisitor(className, outputClassVisitor,
                this.indexView, typeBundle);
    }

}
