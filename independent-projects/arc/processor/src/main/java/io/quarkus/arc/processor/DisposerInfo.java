package io.quarkus.arc.processor;

import jakarta.enterprise.inject.spi.DefinitionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;

/**
 *
 * @author Martin Kouba
 */
public class DisposerInfo implements InjectionTargetInfo {

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

    @Override
    public TargetKind kind() {
        return TargetKind.DISPOSER;
    }

    @Override
    public DisposerInfo asDisposer() {
        return this;
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

    void init(List<Throwable> errors) {
        for (InjectionPointInfo injectionPoint : injection.injectionPoints) {
            Beans.resolveInjectionPoint(declaringBean.getDeployment(), this, injectionPoint, errors);
        }
    }

    Collection<AnnotationInstance> getDisposedParameterQualifiers() {
        Set<AnnotationInstance> resultingQualifiers = new HashSet<>();
        Annotations.getParameterAnnotations(declaringBean.getDeployment(), disposerMethod, disposedParameter.position())
                .stream().forEach(a -> resultingQualifiers.addAll(declaringBean.getDeployment().extractQualifiers(a)));
        return resultingQualifiers;
    }

    Type getDisposedParameterType() {
        return disposerMethod.parameterType(disposedParameter.position());
    }

    MethodParameterInfo initDisposedParam(MethodInfo disposerMethod) {
        List<MethodParameterInfo> disposedParams = new ArrayList<>();
        for (AnnotationInstance annotation : disposerMethod.annotations()) {
            if (Kind.METHOD_PARAMETER == annotation.target().kind() && annotation.name().equals(DotNames.DISPOSES)) {
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
