package io.quarkus.hibernate.search.standalone.elasticsearch.deployment;

import java.util.List;

import org.jboss.jandex.DotName;

class HibernateSearchTypes {

    static final DotName SEARCH_EXTENSION = DotName
            .createSimple("io.quarkus.hibernate.search.standalone.elasticsearch.SearchExtension");
    static final DotName INDEXED = DotName
            .createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed");

    static final DotName ROOT_MAPPING = DotName
            .createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.RootMapping");
    static final List<DotName> BUILT_IN_ROOT_MAPPING_ANNOTATIONS = List.of(
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor"),
            DotName.createSimple("org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity"));

}
