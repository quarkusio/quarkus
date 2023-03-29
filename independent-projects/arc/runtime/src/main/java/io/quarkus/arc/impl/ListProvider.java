package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.spi.CreationalContext;

import io.quarkus.arc.All;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableReferenceProvider;

public class ListProvider implements InjectableReferenceProvider<List<?>> {

    private final Type requiredType;
    private final Type injectionPointType;
    private final Set<Annotation> qualifiers;
    private final InjectableBean<?> targetBean;
    private final Set<Annotation> annotations;
    private final Member javaMember;
    private final int position;
    private final boolean isTransient;
    private final boolean needsInstanceHandle;

    public ListProvider(Type requiredType, Type injectionPointType, Set<Annotation> qualifiers, InjectableBean<?> targetBean,
            Set<Annotation> annotations,
            Member javaMember, int position, boolean isTransient, boolean needsInstanceHandle) {
        this.requiredType = requiredType;
        this.injectionPointType = injectionPointType;
        this.qualifiers = qualifiers;
        // the @All annotation is not a qualifier of the instances we need to resolve
        this.qualifiers.remove(All.Literal.INSTANCE);
        this.targetBean = targetBean;
        this.annotations = annotations;
        this.javaMember = javaMember;
        this.position = position;
        this.isTransient = isTransient;
        this.needsInstanceHandle = needsInstanceHandle;
    }

    @Override
    public List<?> get(CreationalContext<List<?>> creationalContext) {
        if (needsInstanceHandle) {
            return Instances.listOfHandles(targetBean, injectionPointType, requiredType, qualifiers, creationalContext,
                    annotations, javaMember,
                    position, isTransient);
        } else {
            return Instances.listOf(targetBean, injectionPointType, requiredType, qualifiers, creationalContext, annotations,
                    javaMember,
                    position, isTransient);
        }
    }
}
