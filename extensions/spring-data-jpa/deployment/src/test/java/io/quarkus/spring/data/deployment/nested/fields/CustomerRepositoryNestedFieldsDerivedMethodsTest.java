package io.quarkus.spring.data.deployment.nested.fields;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;

import org.hibernate.query.QueryArgumentException;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import io.quarkus.spring.data.deployment.Customer;
import io.quarkus.spring.data.deployment.CustomerRepository;
import io.quarkus.test.QuarkusUnitTest;

class CustomerRepositoryNestedFieldsDerivedMethodsTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("import_customers.sql", "import.sql")
                    .addClasses(Customer.class, CustomerRepository.class))
            .withConfigurationResource("application.properties");

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @Transactional
    void findByAddressZipCode() {

        List<Customer> allByAddressZipCode = customerRepository.findAllByAddressZipCode("28004");
        assertEquals(2, allByAddressZipCode.size());
        assertThat(allByAddressZipCode.stream().map(c -> c.getName()).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("Adam", "Tim");
    }

    @Test
    @Transactional
    void findByAddressCountryIsoCode() {

        assertEquals(2, customerRepository.findAllByAddressCountryIsoCode("ES")
                .size());

        assertEquals(2, customerRepository.findAllByAddress_CountryIsoCode("ES")
                .size());

        assertEquals(2, customerRepository.findAllByAddress_Country_IsoCode("ES")
                .size());
    }

    @Test
    @Transactional
    void findByAddressCountry() {

        QueryArgumentException exception = assertThrows(QueryArgumentException.class,
                () -> customerRepository.findAllByAddressCountry("Spain"));
        assertThat(exception).hasMessageContaining("Argument [Spain] of type [java.lang.String] did not match parameter type");
        assertThrows(QueryArgumentException.class,
                () -> customerRepository.findAllByAddress_Country("Spain"));
        assertThat(exception).hasMessageContaining("Argument [Spain] of type [java.lang.String] did not match parameter type");

    }

    @Test
    @Transactional
    void shouldCountSpanishCustomers() {

        long spanishCustomers = customerRepository.countCustomerByAddressCountryName("Spain");
        Assertions.assertEquals(2, spanishCustomers);

    }

    @Test
    @Transactional
    void shouldCountCustomersByZipCode() {

        long spanishCustomers = customerRepository.countCustomerByAddress_ZipCode("28004");
        Assertions.assertEquals(2, spanishCustomers);

    }
}
