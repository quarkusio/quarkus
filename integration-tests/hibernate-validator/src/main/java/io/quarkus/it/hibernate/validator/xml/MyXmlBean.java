package io.quarkus.it.hibernate.validator.xml;

import io.quarkus.hibernate.accessor.GeneratedFieldAccessor;

public class MyXmlBean {
    @GeneratedFieldAccessor
    int id = 0;
    @GeneratedFieldAccessor
    String name;
}
