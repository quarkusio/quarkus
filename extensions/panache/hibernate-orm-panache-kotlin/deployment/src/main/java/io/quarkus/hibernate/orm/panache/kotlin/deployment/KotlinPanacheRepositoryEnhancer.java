package io.quarkus.hibernate.orm.panache.kotlin.deployment;

import static io.quarkus.hibernate.orm.panache.kotlin.deployment.KotlinJpaTypeBundle.BUNDLE;

import java.util.List;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;
import io.quarkus.panache.common.deployment.visitors.KotlinPanacheClassVisitor;

public class KotlinPanacheRepositoryEnhancer extends PanacheRepositoryEnhancer {

    private List<PanacheMethodCustomizer> methodCustomizers;

    public KotlinPanacheRepositoryEnhancer(IndexView index, List<PanacheMethodCustomizer> methodCustomizers) {
        super(index);
        this.methodCustomizers = methodCustomizers;
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new KotlinPanacheClassVisitor(outputClassVisitor,
                indexView.getClassByName(DotName.createSimple(className)), indexView, BUNDLE,
                BUNDLE.repositoryBase(), methodCustomizers);
    }
}
