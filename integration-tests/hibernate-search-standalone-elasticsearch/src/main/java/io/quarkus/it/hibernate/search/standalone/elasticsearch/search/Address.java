package io.quarkus.it.hibernate.search.standalone.elasticsearch.search;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;

public class Address {

    @FullTextField(analyzer = "standard")
    private String city;

    public Address(String city) {
        this.city = city;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String name) {
        this.city = name;
    }
}
