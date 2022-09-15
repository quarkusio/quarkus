package io.quarkus.arc.processor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Singleton;
import java.lang.annotation.Annotation;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
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

    public static BuiltinScope from(DotName scopeAnnotationName) {
        for (BuiltinScope scope : BuiltinScope.values()) {
            if (scope.getInfo().getDotName().equals(scopeAnnotationName)) {
                return scope;
            }
        }
        return null;
    }

    public static BuiltinScope from(ClassInfo clazz) {
        for (BuiltinScope scope : BuiltinScope.values()) {
            if (clazz.classAnnotation(scope.getName()) != null) {
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

    public static boolean isDeclaredOn(ClassInfo clazz) {
        for (BuiltinScope scope : BuiltinScope.values()) {
            if (clazz.classAnnotation(scope.getName()) != null) {
                return true;
            }
        }
        return false;
    }

}
