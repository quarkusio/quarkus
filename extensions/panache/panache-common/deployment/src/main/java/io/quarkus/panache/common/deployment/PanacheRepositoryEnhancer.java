package io.quarkus.panache.common.deployment;

import java.lang.reflect.Modifier;
import java.util.function.BiFunction;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

public abstract class PanacheRepositoryEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    protected final IndexView indexView;

    public PanacheRepositoryEnhancer(IndexView index) {
        this.indexView = index;
    }

    @Override
    public abstract ClassVisitor apply(String className, ClassVisitor outputClassVisitor);

    public boolean skipRepository(ClassInfo classInfo) {
        // we don't want to add methods to abstract/generic entities/repositories: they get added to bottom types
        // which can't be either
        return Modifier.isAbstract(classInfo.flags())
                || !classInfo.typeParameters().isEmpty();
    }
}
