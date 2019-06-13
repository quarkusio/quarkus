package io.quarkus.arc.processor;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.Objects;
import org.jboss.jandex.DotName;

public class ScopeInfo {

    private final DotName dotName;

    private final boolean isNormal;

    private boolean declaresInherited;

    ScopeInfo(Class<? extends Annotation> clazz, boolean isNormal) {
        this.dotName = DotName.createSimple(clazz.getName());
        this.isNormal = isNormal;
        declaresInherited = clazz.getAnnotation(Inherited.class) == null ? false : true;
    }

    public DotName getDotName() {
        return dotName;
    }

    public boolean isNormal() {
        return isNormal;
    }

    public boolean declaresInherited() {
        return declaresInherited;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dotName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ScopeInfo other = (ScopeInfo) obj;
        return Objects.equals(dotName, other.dotName);
    }

}
