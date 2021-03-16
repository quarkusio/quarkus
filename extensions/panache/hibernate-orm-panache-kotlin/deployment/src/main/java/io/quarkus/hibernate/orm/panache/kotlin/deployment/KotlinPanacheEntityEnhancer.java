package io.quarkus.hibernate.orm.panache.kotlin.deployment;

import static io.quarkus.hibernate.orm.panache.kotlin.deployment.KotlinJpaTypeBundle.BUNDLE;

import java.util.List;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.visitors.KotlinPanacheClassOperationGenerationVisitor;

public class KotlinPanacheEntityEnhancer extends PanacheEntityEnhancer {

    public KotlinPanacheEntityEnhancer(IndexView index, List<PanacheMethodCustomizer> methodCustomizers) {
        super(index, methodCustomizers);
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new KotlinPanacheClassOperationGenerationVisitor(outputClassVisitor,
                indexView.getClassByName(DotName.createSimple(className)), indexView, BUNDLE,
                BUNDLE.entityBase(), methodCustomizers);
    }

}
