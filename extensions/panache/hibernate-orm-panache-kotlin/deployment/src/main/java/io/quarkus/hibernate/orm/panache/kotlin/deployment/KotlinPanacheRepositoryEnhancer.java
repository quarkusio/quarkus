package io.quarkus.hibernate.orm.panache.kotlin.deployment;

import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;

public class KotlinPanacheRepositoryEnhancer extends PanacheRepositoryEnhancer {

    public KotlinPanacheRepositoryEnhancer(IndexView index) {
        super(index, KotlinPanacheResourceProcessor.PANACHE_REPOSITORY_BASE);
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new KotlinPanacheRepositoryClassVisitor(className, outputClassVisitor,
                this.indexView);
    }
}
