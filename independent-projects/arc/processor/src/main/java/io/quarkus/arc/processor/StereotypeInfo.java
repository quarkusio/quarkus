package io.quarkus.arc.processor;

import java.util.List;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;

public class StereotypeInfo {

    private final ScopeInfo defaultScope;
    private final List<AnnotationInstance> interceptorBindings;
    private final boolean alternative;
    private final Integer alternativePriority;
    private final boolean isNamed;
    private final ClassInfo target;
    // allows to differentiate between standard stereotype and one that is in fact additional bean defining annotation
    private final boolean isAdditionalBeanDefiningAnnotation;

    public StereotypeInfo(ScopeInfo defaultScope, List<AnnotationInstance> interceptorBindings, boolean alternative,
            Integer alternativePriority,
            boolean isNamed, boolean isAdditionalBeanDefiningAnnotation, ClassInfo target) {
        this.defaultScope = defaultScope;
        this.interceptorBindings = interceptorBindings;
        this.alternative = alternative;
        this.alternativePriority = alternativePriority;
        this.isNamed = isNamed;
        this.target = target;
        this.isAdditionalBeanDefiningAnnotation = isAdditionalBeanDefiningAnnotation;
    }

    public StereotypeInfo(ScopeInfo defaultScope, List<AnnotationInstance> interceptorBindings, boolean alternative,
            Integer alternativePriority,
            boolean isNamed, ClassInfo target) {
        this(defaultScope, interceptorBindings, alternative, alternativePriority, isNamed, false, target);
    }

    public ScopeInfo getDefaultScope() {
        return defaultScope;
    }

    public List<AnnotationInstance> getInterceptorBindings() {
        return interceptorBindings;
    }

    public boolean isAlternative() {
        return alternative;
    }

    public Integer getAlternativePriority() {
        return alternativePriority;
    }

    public boolean isNamed() {
        return isNamed;
    }

    public ClassInfo getTarget() {
        return target;
    }

    public boolean isAdditionalBeanDefiningAnnotation() {
        return isAdditionalBeanDefiningAnnotation;
    }
}
