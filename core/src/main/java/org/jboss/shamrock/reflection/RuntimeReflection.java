package org.jboss.shamrock.reflection;

public interface RuntimeReflection {

    Object newInstance(ConstructorHandle constructor, Object... params);
}
