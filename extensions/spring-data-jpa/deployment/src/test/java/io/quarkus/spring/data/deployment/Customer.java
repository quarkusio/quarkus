package io.quarkus.spring.data.deployment;

import java.time.ZonedDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Customer {

    @Id
    @GeneratedValue
    private Integer id;
    private String name;
    private Integer age;
    private ZonedDateTime birthDate;
    private Boolean active;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private Address address;

    public Customer() {
    }

    public Customer(String name, Integer age, ZonedDateTime birthDate, Boolean active) {
        this.name = name;
        this.age = age;
        this.birthDate = birthDate;
        this.active = active;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public ZonedDateTime getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(ZonedDateTime birthDate) {
        this.birthDate = birthDate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Entity
    public static class Address {
        @Id
        @GeneratedValue
        private Integer id;

        private String zipCode;

        @ManyToOne(cascade = CascadeType.ALL)
        @JoinColumn(name = "country_id", referencedColumnName = "id")
        private Country country;

        public Address(String zipCode, Country country) {
            this.zipCode = zipCode;
            this.country = country;
        }

        public Address() {

        }
    }

    @Entity
    public static class Country {
        @Id
        @GeneratedValue
        private Integer id;

        private String name;

        private String isoCode;

        public Country(String name, String isoCode) {
            this.name = name;
            this.isoCode = isoCode;
        }

        public Country() {

        }
    }
}
