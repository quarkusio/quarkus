package io.quarkus.hibernate.reactive.panache.deployment;

import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;
import io.quarkus.panache.common.deployment.visitors.PanacheRepositoryClassVisitor;

public class PanacheJpaRepositoryEnhancer extends PanacheRepositoryEnhancer {

    public PanacheJpaRepositoryEnhancer(IndexView index) {
        super(index);
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new PanacheRepositoryClassVisitor(className, outputClassVisitor,
                this.indexView, ReactiveJavaJpaTypeBundle.BUNDLE);
    }
}
