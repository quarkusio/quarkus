package io.quarkus.it.hibernate.search.standalone.elasticsearch.search;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IdProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

@ProjectionConstructor
public record PersonDTO(
        @IdProjection long id,
        String name,
        AddressDTO address) {
}
