package io.quarkus.arc.processor;

import jakarta.enterprise.inject.spi.InterceptionType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;

public final class InterceptorResolver {

    private final BeanDeployment beanDeployment;

    InterceptorResolver(BeanDeployment beanDeployment) {
        this.beanDeployment = beanDeployment;
    }

    /**
     *
     * @param interceptionType
     * @param bindings
     * @return the list of interceptors for a set of interceptor bindings and a type of interception
     */
    public List<InterceptorInfo> resolve(InterceptionType interceptionType, Set<AnnotationInstance> bindings) {
        if (bindings.isEmpty()) {
            return Collections.emptyList();
        }
        List<InterceptorInfo> interceptors = new ArrayList<>();
        for (InterceptorInfo interceptor : beanDeployment.getInterceptors()) {
            if (!interceptor.intercepts(interceptionType)) {
                continue;
            }
            boolean matches = true;
            for (AnnotationInstance interceptorBinding : interceptor.getBindings()) {
                if (!hasInterceptorBinding(bindings, interceptorBinding)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                interceptors.add(interceptor);
            }
        }
        if (interceptors.isEmpty()) {
            return Collections.emptyList();
        }
        interceptors.sort(this::compare);
        return interceptors;
    }

    private int compare(InterceptorInfo i1, InterceptorInfo i2) {
        return Integer.compare(i1.getPriority(), i2.getPriority());
    }

    private boolean hasInterceptorBinding(Collection<AnnotationInstance> bindings, AnnotationInstance interceptorBinding) {
        for (AnnotationInstance binding : bindings) {
            if (isInterceptorBinding(interceptorBinding, binding)) {
                return true;
            } else {
                // could be transitive binding
                Set<AnnotationInstance> transitiveInterceptorBindings = beanDeployment
                        .getTransitiveInterceptorBindings(binding.name());
                if (transitiveInterceptorBindings == null) {
                    continue;
                }
                for (AnnotationInstance transitiveBindingInstance : transitiveInterceptorBindings) {
                    if (isInterceptorBinding(interceptorBinding,
                            transitiveBindingInstance)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isInterceptorBinding(AnnotationInstance interceptorBinding, AnnotationInstance candidate) {
        ClassInfo interceptorBindingClass = beanDeployment.getInterceptorBinding(interceptorBinding.name());
        if (candidate.name().equals(interceptorBinding.name())) {
            // Must have the same annotation member value for each member which is not annotated @Nonbinding
            boolean matches = true;
            Set<String> nonBindingFields = beanDeployment.getInterceptorNonbindingMembers(interceptorBinding.name());
            for (AnnotationValue value : candidate.valuesWithDefaults(beanDeployment.getBeanArchiveIndex())) {
                String annotationField = value.name();
                if (!interceptorBindingClass.method(annotationField).hasAnnotation(DotNames.NONBINDING)
                        && !nonBindingFields.contains(annotationField)
                        && !value.equals(
                                interceptorBinding.valueWithDefault(beanDeployment.getBeanArchiveIndex(), annotationField))) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return true;
            }
        }
        return false;
    }

}
