package io.quarkus.spring.data.deployment;

import java.util.List;

import org.springframework.data.repository.Repository;

// issue 13067:
public interface PersonRepository extends Repository<Person, Integer> {

    List<Person> findAllByAddressZipCode(String zipCode);

    List<Person> findAllByAddressCountry(String zipCode);

    List<Person> findAllByAddress_Country(String zipCode);

    List<Person> findAllByAddressCountryIsoCode(String zipCode);

    List<Person> findAllByAddress_CountryIsoCode(String zipCode);

    List<Person> findAllByAddress_Country_IsoCode(String zipCode);

    List<Person> findAllByAddress_CountryInvalid(String zipCode);

    List<Person> findAllBy_(String zipCode);
}
