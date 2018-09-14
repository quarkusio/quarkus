package org.jboss.protean.arc.processor;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;

public class StereotypeInfo {

    private final ScopeInfo defaultScope;

    private final List<AnnotationInstance> interceptorBindings;

    private final boolean isAlternative;

    private final ClassInfo target;

    public StereotypeInfo(ScopeInfo defaultScope, List<AnnotationInstance> interceptorBindings, boolean isAlternative, ClassInfo target) {
        this.defaultScope = defaultScope;
        this.interceptorBindings = interceptorBindings;
        this.isAlternative = isAlternative;
        this.target = target;
    }

    ScopeInfo getDefaultScope() {
        return defaultScope;
    }

    List<AnnotationInstance> getInterceptorBindings() {
        return interceptorBindings;
    }

    boolean isAlternative() {
        return isAlternative;
    }

    ClassInfo getTarget() {
        return target;
    }

}
