package io.quarkus.panache.common.deployment;

import java.util.List;
import java.util.function.BiFunction;

import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

public abstract class PanacheCompanionEnhancer
        implements BiFunction<String, ClassVisitor, ClassVisitor> {

    protected final IndexView indexView;
    protected final List<PanacheMethodCustomizer> methodCustomizers;

    public PanacheCompanionEnhancer(IndexView index, List<PanacheMethodCustomizer> methodCustomizers) {
        this.indexView = index;
        this.methodCustomizers = methodCustomizers;
    }

    @Override
    public abstract ClassVisitor apply(String className, ClassVisitor outputClassVisitor);

}
