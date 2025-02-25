package io.quarkus.spring.data.deployment;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {

    List<Customer> findByName(String name);

    List<Customer> findByNameIs(String name);

    List<Customer> findByNameEquals(String name);

    List<Customer> findByNameIsNull();

    List<Customer> findByNameNot(String name);

    List<Customer> findByNameIsNot(String name);

    List<Customer> findByNameStartingWith(String name);

    List<Customer> findByNameEndingWith(String name);

    List<Customer> findByNameContaining(String name);

    List<Customer> findByNameLike(String name);

    List<Customer> findByAgeLessThan(Integer age);

    List<Customer> findByAgeLessThanEqual(Integer age);

    List<Customer> findByAgeGreaterThan(Integer age);

    List<Customer> findByAgeGreaterThanEqual(Integer age);

    List<Customer> findByAgeBetween(Integer startAge, Integer endAge);

    List<Customer> findByBirthDateAfter(ZonedDateTime birthDate);

    List<Customer> findByBirthDateBefore(ZonedDateTime birthDate);

    List<Customer> findByActiveTrue();

    List<Customer> findByActiveFalse();

    List<Customer> findByAgeIn(Collection<Integer> ages);

    List<Customer> findByNameOrAge(String name, Integer age);

    List<Customer> findAllByNameAndAgeAndActive(String name, int age, boolean active);

    List<Customer> findAllByNameAndAgeOrActive(String name, int age, boolean active);

    List<Customer> findAllByNameOrAgeOrActive(String name, int age, boolean active);

    List<Customer> findByNameOrAgeAndActive(String name, Integer age, Boolean active);

    List<Customer> findByNameOrderByName(String name);

    List<Customer> findByNameOrderByNameDesc(String name);

    List<Customer> findByNameIsNotNull();

    List<Customer> findByNameOrderByNameAsc(String name);

    //-------------------nested fields--------------------------------
    List<Customer> findAllByAddressZipCode(String zipCode);

    List<Customer> findAllByAddressCountryIsoCode(String isoCode);

    List<Customer> findAllByAddressCountry(String country);

    List<Customer> findAllByAddress_Country(String country);

    List<Customer> findAllByAddress_CountryIsoCode(String isoCode);

    List<Customer> findAllByAddress_Country_IsoCode(String isoCode);

    long countCustomerByAddressCountryName(String name);

    long countCustomerByAddress_ZipCode(String zipCode);

}
