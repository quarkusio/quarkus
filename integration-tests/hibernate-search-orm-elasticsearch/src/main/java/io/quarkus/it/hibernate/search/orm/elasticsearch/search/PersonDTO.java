package io.quarkus.it.hibernate.search.orm.elasticsearch.search;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IdProjection;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ProjectionConstructor;

// Ideally we'd use records, but Quarkus still has to compile with JDK 11 at the moment.
public class PersonDTO {
    public final long id;
    public final String name;
    public final AddressDTO address;

    @ProjectionConstructor
    public PersonDTO(@IdProjection long id, String name, AddressDTO address) {
        this.id = id;
        this.name = name;
        this.address = address;
    }

    @Override
    public String toString() {
        return "PersonDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", address=" + address +
                '}';
    }
}
