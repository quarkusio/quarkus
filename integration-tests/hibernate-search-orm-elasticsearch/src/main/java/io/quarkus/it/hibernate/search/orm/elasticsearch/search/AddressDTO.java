package io.quarkus.it.hibernate.search.orm.elasticsearch.search;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

// Ideally we'd use records, but Quarkus still has to compile with JDK 11 at the moment.
public class AddressDTO {
    public final String city;

    @ProjectionConstructor
    public AddressDTO(String city) {
        this.city = city;
    }

    @Override
    public String toString() {
        return "AddressDTO{" +
                "city='" + city + '\'' +
                '}';
    }
}
