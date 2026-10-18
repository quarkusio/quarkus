package io.quarkus.spring.data.deployment;

import java.time.ZonedDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Person {
    @Id
    @GeneratedValue
    private Integer id;

    private String name;
    private Integer age;
    private ZonedDateTime birthDate;
    private Boolean active;
    private String order;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private Address address;

    private String addressCountry;

    public Person() {
    }

    public Person(String name, Integer age, ZonedDateTime birthDate, Boolean active, String order) {
        this.name = name;
        this.age = age;
        this.birthDate = birthDate;
        this.active = active;
        this.order = order;
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

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getAddressCountry() {
        return addressCountry;
    }

    public void setAddressCountry(String addressCountry) {
        this.addressCountry = addressCountry;
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
