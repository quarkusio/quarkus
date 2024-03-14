package io.quarkus.it.hibernate.search.standalone.elasticsearch.search;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

@ProjectionConstructor
public record AddressDTO(String city) {
}
