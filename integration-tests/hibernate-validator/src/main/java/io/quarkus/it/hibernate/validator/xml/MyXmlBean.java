package io.quarkus.it.hibernate.validator.xml;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeAccessor;

public class MyXmlBean {
    @ReflectionFreeAccessor
    int id = 0;
    @ReflectionFreeAccessor
    String name;
}
