package io.quarkus.spring.data.deployment;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Person {
    @Id
    private Integer id;

    private Address address;

    private String addressCountry;

    @Entity
    public static class Address {
        @Id
        private Integer id;

        private String zipCode;

        private Country country;
    }

    @Entity
    public static class Country {
        @Id
        private Integer id;

        private String isoCode;
    }

}
