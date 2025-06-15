package io.quarkus.panache.common.deployment;

import java.util.List;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.panache.common.deployment.visitors.KotlinPanacheClassOperationGenerationVisitor;

public class KotlinPanacheEntityEnhancer extends PanacheEntityEnhancer {

    private TypeBundle bundle;

    public KotlinPanacheEntityEnhancer(IndexView index, List<PanacheMethodCustomizer> methodCustomizers,
            TypeBundle bundle) {
        super(index, methodCustomizers);
        this.bundle = bundle;
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new KotlinPanacheClassOperationGenerationVisitor(outputClassVisitor,
                indexView.getClassByName(DotName.createSimple(className)), indexView, bundle, bundle.entityBase(),
                methodCustomizers);
    }

}
