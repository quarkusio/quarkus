package io.quarkus.spring.data.deployment;

import java.util.List;

import org.springframework.data.repository.Repository;

// issue 13067: This repo is used to test the MethodNameParser class. See MethodNameParserTest class
public interface PersonRepository extends Repository<Person, Integer> {

    List<Person> findAllByAddressZipCode(String zipCode);

    List<Person> findAllByNameAndOrder(String name, String order);

    List<Person> findAllByNameOrOrder(String name, String order);

    List<Person> findAllByAddressCountry(String zipCode);

    List<Person> findAllByNameAndAgeAndActive(String name, int age, boolean active);

    List<Person> findAllByNameAndAgeOrActive(String name, int age, boolean active);

    List<Person> findAllByNameOrAgeOrActive(String name, int age, boolean active);

    List<Person> findAllByAddress_Country(String zipCode);

    List<Person> findAllByAddressCountryIsoCode(String zipCode);

    List<Person> findAllByAddress_CountryIsoCode(String zipCode);

    List<Person> findAllByAddress_Country_IsoCode(String zipCode);

    List<Person> findAllByAddress_CountryInvalid(String zipCode);

    List<Person> findAllBy_(String zipCode);
}
