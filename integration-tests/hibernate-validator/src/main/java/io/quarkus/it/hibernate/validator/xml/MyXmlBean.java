package io.quarkus.it.hibernate.validator.xml;

import io.quarkus.hibernate.accessor.runtime.ReflectionFreeFieldAccessor;

public class MyXmlBean {
    @ReflectionFreeFieldAccessor
    int id = 0;
    @ReflectionFreeFieldAccessor
    String name;
}
