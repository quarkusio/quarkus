package org.jboss.protean.arc.processor;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.spi.DefinitionException;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;

/**
 *
 * @author Martin Kouba
 */
public class DisposerInfo {

    private final BeanInfo declaringBean;

    private final MethodInfo disposerMethod;

    private final Injection injection;

    private final MethodParameterInfo disposedParameter;

    DisposerInfo(BeanInfo declaringBean, MethodInfo disposerMethod, Injection injection) {
        this.declaringBean = declaringBean;
        this.disposerMethod = disposerMethod;
        this.injection = injection;
        this.disposedParameter = initDisposedParam(disposerMethod);
    }

    public BeanInfo getDeclaringBean() {
        return declaringBean;
    }

    public MethodInfo getDisposerMethod() {
        return disposerMethod;
    }

    public MethodParameterInfo getDisposedParameter() {
        return disposedParameter;
    }

    Injection getInjection() {
        return injection;
    }

    public List<InjectionPointInfo> getAllInjectionPoints() {
        List<InjectionPointInfo> injectionPoints = new ArrayList<>();
        injectionPoints.addAll(injection.injectionPoints);
        return injectionPoints;
    }

    void init() {
        for (InjectionPointInfo injectionPoint : injection.injectionPoints) {
            Beans.resolveInjectionPoint(declaringBean.getDeployment(), null, injectionPoint);
        }
    }

    MethodParameterInfo initDisposedParam(MethodInfo disposerMethod) {
        List<MethodParameterInfo> disposedParams = new ArrayList<>();
        for (AnnotationInstance annotation : disposerMethod.annotations()) {
            if (Kind.METHOD_PARAMETER.equals(annotation.target().kind()) && annotation.name().equals(DotNames.DISPOSES)) {
                disposedParams.add(annotation.target().asMethodParameter());
            }
        }
        if (disposedParams.isEmpty()) {
            throw new DefinitionException("No disposed parameters found for " + disposerMethod);
        } else if (disposedParams.size() > 1) {
            throw new DefinitionException("Multiple disposed parameters found for " + disposerMethod);
        }
        return disposedParams.get(0);
    }
}
