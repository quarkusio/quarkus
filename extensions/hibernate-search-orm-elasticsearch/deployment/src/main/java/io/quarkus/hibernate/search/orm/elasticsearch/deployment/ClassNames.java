package io.quarkus.hibernate.search.orm.elasticsearch.deployment;

import org.jboss.jandex.DotName;

class ClassNames {

    static final DotName SEARCH_EXTENSION = DotName
            .createSimple("io.quarkus.hibernate.search.orm.elasticsearch.SearchExtension");
    static final DotName INDEXED = DotName
            .createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed");

    static final DotName ROOT_MAPPING = DotName
            .createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.RootMapping");
    static final DotName PROJECTION_CONSTRUCTOR = DotName
            .createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor");

}
