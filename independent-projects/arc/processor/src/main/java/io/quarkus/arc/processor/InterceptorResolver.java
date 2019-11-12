package io.quarkus.arc.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.enterprise.inject.spi.InterceptionType;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;

public class InterceptorResolver {

    private final BeanDeployment beanDeployment;

    public InterceptorResolver(BeanDeployment beanDeployment) {
        this.beanDeployment = beanDeployment;
    }

    public List<InterceptorInfo> resolve(InterceptionType interceptionType, Set<AnnotationInstance> bindings) {
        if (bindings.isEmpty()) {
            return Collections.emptyList();
        }
        List<InterceptorInfo> interceptors = new ArrayList<>();
        for (InterceptorInfo interceptor : beanDeployment.getInterceptors()) {
            if (!interceptor.intercepts(interceptionType)) {
                continue;
            }
            boolean matches = false;
            for (AnnotationInstance interceptorBinding : interceptor.getBindings()) {
                if (hasInterceptorBinding(bindings, interceptorBinding)) {
                    matches = true;
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
            Set<String> nonBindingFields = beanDeployment.getNonBindingFields(interceptorBinding.name());
            for (AnnotationValue value : candidate.valuesWithDefaults(beanDeployment.getIndex())) {
                String annotationField = value.name();
                if (!interceptorBindingClass.method(annotationField).hasAnnotation(DotNames.NONBINDING)
                        && !nonBindingFields.contains(annotationField)
                        && !value.equals(interceptorBinding.valueWithDefault(beanDeployment.getIndex(), annotationField))) {
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
