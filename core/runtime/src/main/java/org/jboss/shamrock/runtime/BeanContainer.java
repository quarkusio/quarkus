package org.jboss.shamrock.runtime;

import java.lang.annotation.Annotation;

public interface BeanContainer {

    <T> T instance(Class<T> type, Annotation... qualifiers);

}
