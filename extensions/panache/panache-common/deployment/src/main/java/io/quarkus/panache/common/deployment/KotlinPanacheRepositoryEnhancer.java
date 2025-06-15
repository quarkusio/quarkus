package io.quarkus.panache.common.deployment;

import java.util.List;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.panache.common.deployment.visitors.KotlinPanacheClassOperationGenerationVisitor;

public class KotlinPanacheRepositoryEnhancer extends PanacheRepositoryEnhancer {

    private List<PanacheMethodCustomizer> methodCustomizers;
    private TypeBundle bundle;

    public KotlinPanacheRepositoryEnhancer(IndexView index, List<PanacheMethodCustomizer> methodCustomizers,
            TypeBundle bundle) {
        super(index);
        this.methodCustomizers = methodCustomizers;
        this.bundle = bundle;
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new KotlinPanacheClassOperationGenerationVisitor(outputClassVisitor,
                indexView.getClassByName(DotName.createSimple(className)), indexView, bundle, bundle.repositoryBase(),
                methodCustomizers);
    }
}
