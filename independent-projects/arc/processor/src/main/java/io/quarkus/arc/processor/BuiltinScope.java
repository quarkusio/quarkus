package io.quarkus.arc.processor;

import java.lang.annotation.Annotation;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.inject.Singleton;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

public enum BuiltinScope {

    DEPENDENT(Dependent.class, false),
    SINGLETON(Singleton.class, false),
    APPLICATION(ApplicationScoped.class, true),
    REQUEST(RequestScoped.class, true);

    private ScopeInfo info;

    private BuiltinScope(Class<? extends Annotation> clazz, boolean isNormal) {
        this.info = new ScopeInfo(clazz, isNormal);
    }

    public ScopeInfo getInfo() {
        return info;
    }

    public DotName getName() {
        return info.getDotName();
    }

    public static BuiltinScope from(DotName name) {
        for (BuiltinScope scope : BuiltinScope.values()) {
            if (scope.getInfo().getDotName().equals(name)) {
                return scope;
            }
        }
        return null;
    }

    public static boolean isDefault(ScopeInfo scope) {
        return DEPENDENT.is(scope);
    }

    public boolean is(ScopeInfo scope) {
        return getInfo().equals(scope);
    }

    public static boolean isIn(Iterable<AnnotationInstance> annotations) {
        for (AnnotationInstance annotation : annotations) {
            if (from(annotation.name()) != null) {
                return true;
            }
        }
        return false;
    }

}
