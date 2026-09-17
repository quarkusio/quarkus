package io.quarkus.arc.processor;

import java.util.List;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

public class StereotypeInfo {

    private final ScopeInfo defaultScope;
    private final List<AnnotationInstance> interceptorBindings;
    private final boolean alternative;
    private final boolean reserve;
    private final Integer priority;
    private final boolean isNamed;
    private final boolean isEager;
    private final boolean isAutoClose;
    private final boolean isInherited;
    private final List<AnnotationInstance> parentStereotypes;
    private final ClassInfo target;
    // used to differentiate between standard stereotype and one that was added through StereotypeRegistrarBuildItem
    private final boolean isAdditionalStereotype;

    public StereotypeInfo(ScopeInfo defaultScope, List<AnnotationInstance> interceptorBindings, boolean alternative,
            boolean reserve, Integer priority, boolean isNamed, boolean isEager, boolean isAutoClose,
            boolean isAdditionalStereotype, ClassInfo target, boolean isInherited, List<AnnotationInstance> parentStereotypes) {
        this.defaultScope = defaultScope;
        this.interceptorBindings = interceptorBindings;
        this.alternative = alternative;
        this.reserve = reserve;
        this.priority = priority;
        this.isNamed = isNamed;
        this.isEager = isEager;
        this.isAutoClose = isAutoClose;
        this.isInherited = isInherited;
        this.parentStereotypes = parentStereotypes;
        this.target = target;
        this.isAdditionalStereotype = isAdditionalStereotype;
    }

    public StereotypeInfo(ScopeInfo defaultScope, List<AnnotationInstance> interceptorBindings, boolean alternative,
            boolean reserve, Integer priority, boolean isNamed, boolean isEager, boolean isAutoClose, ClassInfo target,
            boolean isInherited, List<AnnotationInstance> parentStereotype) {
        this(defaultScope, interceptorBindings, alternative, reserve, priority, isNamed, isEager, isAutoClose,
                false, target, isInherited, parentStereotype);
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

    public boolean isReserve() {
        return reserve;
    }

    public boolean isInherited() {
        return isInherited;
    }

    public Integer getPriority() {
        return priority;
    }

    /**
     * @deprecated use {@link #getPriority()}
     */
    @Deprecated(forRemoval = true, since = "4.0")
    public Integer getAlternativePriority() {
        return priority;
    }

    public boolean isNamed() {
        return isNamed;
    }

    public boolean isEager() {
        return isEager;
    }

    public boolean isAutoClose() {
        return isAutoClose;
    }

    public ClassInfo getTarget() {
        return target;
    }

    public DotName getName() {
        return target.name();
    }

    public boolean isAdditionalStereotype() {
        return isAdditionalStereotype;
    }

    public boolean isGenuine() {
        return !isAdditionalStereotype;
    }

    public List<AnnotationInstance> getParentStereotypes() {
        return parentStereotypes;
    }
}
