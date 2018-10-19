package org.jboss.protean.arc.processor;

import java.lang.annotation.Annotation;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.inject.Singleton;

import org.jboss.jandex.DotName;

public enum ScopeInfo {

    DEPENDENT(Dependent.class),
    SINGLETON(Singleton.class),
    APPLICATION(ApplicationScoped.class, true),
    REQUEST(RequestScoped.class, true),
    ;

    private final DotName dotName;

    private final Class<? extends Annotation> clazz;

    private final boolean isNormal;

    private ScopeInfo(Class<? extends Annotation> clazz) {
        this(clazz, false);
    }

    private ScopeInfo(Class<? extends Annotation> clazz, boolean isNormal) {
        this.dotName = DotNames.create(clazz);
        this.clazz = clazz;
        this.isNormal = isNormal;
    }

    DotName getDotName() {
        return dotName;
    }

    Class<? extends Annotation> getClazz() {
        return clazz;
    }

    boolean isNormal() {
        return isNormal;
    }

    boolean isDefault() {
        return DEPENDENT == this;
    }

    static ScopeInfo from(DotName name) {
        for (ScopeInfo scope : ScopeInfo.values()) {
            if (scope.getDotName().equals(name)) {
                return scope;
            }
        }
        return null;
    }

}
