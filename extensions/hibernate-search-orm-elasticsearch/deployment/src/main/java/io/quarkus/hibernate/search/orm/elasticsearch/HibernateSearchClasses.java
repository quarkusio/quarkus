package io.quarkus.hibernate.search.orm.elasticsearch;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.gson.spi.GsonClasses;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.jboss.jandex.DotName;

class HibernateSearchClasses {

    static final DotName INDEXED = DotName.createSimple(Indexed.class.getName());

    static final List<DotName> GSON_CLASSES = new ArrayList<>();
    static {
        for (Class<?> gsonClass : GsonClasses.typesRequiringReflection()) {
            GSON_CLASSES.add(DotName.createSimple(gsonClass.getName()));
        }
    }
}
