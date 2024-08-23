package io.quarkus.it.hibernate.search.standalone.opensearch.search;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

@ProjectionConstructor
public record AddressDTO(String city) {
}
