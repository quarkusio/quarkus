package io.quarkus.panache.hibernate.common.deployment;

import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;
import io.quarkus.panache.common.deployment.TypeBundle;
import io.quarkus.panache.common.deployment.visitors.PanacheRepositoryClassOperationGenerationVisitor;

public class PanacheJpaRepositoryEnhancer extends PanacheRepositoryEnhancer {

    public PanacheJpaRepositoryEnhancer(IndexView index, TypeBundle bundle) {
        super(index, bundle);
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new PanacheRepositoryClassOperationGenerationVisitor(className, outputClassVisitor,
                this.indexView, this.bundle);
    }
}
