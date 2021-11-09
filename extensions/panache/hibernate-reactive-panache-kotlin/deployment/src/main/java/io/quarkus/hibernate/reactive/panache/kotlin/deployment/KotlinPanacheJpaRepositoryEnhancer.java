package io.quarkus.hibernate.reactive.panache.kotlin.deployment;

import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;
import io.quarkus.panache.common.deployment.visitors.PanacheRepositoryClassOperationGenerationVisitor;

public class KotlinPanacheJpaRepositoryEnhancer extends PanacheRepositoryEnhancer {

    public KotlinPanacheJpaRepositoryEnhancer(IndexView index) {
        super(index);
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new PanacheRepositoryClassOperationGenerationVisitor(className, outputClassVisitor,
                this.indexView, ReactiveJavaJpaTypeBundle.BUNDLE);
    }
}
