package org.jboss.shamrock.runtime;

import java.lang.annotation.Annotation;

public interface BeanContainer {

    default <T> T instance(Class<T> type, Annotation... qualifiers) {
        return instanceFactory(type, qualifiers).get();
    }
    <T> Factory<T> instanceFactory(Class<T> type, Annotation... qualifiers);


    interface Factory<T> {

        T get();
    }

}
