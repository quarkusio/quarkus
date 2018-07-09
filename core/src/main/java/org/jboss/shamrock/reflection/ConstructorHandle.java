package org.jboss.shamrock.reflection;

public interface ConstructorHandle {

    Object newInstance(Object... args);

}
