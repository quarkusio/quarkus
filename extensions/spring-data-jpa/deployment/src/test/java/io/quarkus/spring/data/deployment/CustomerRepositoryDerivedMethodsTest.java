package io.quarkus.spring.data.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import jakarta.transaction.Transactional;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import io.quarkus.test.QuarkusUnitTest;

class CustomerRepositoryDerivedMethodsTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("import_customers.sql", "import.sql")
                    .addClasses(Customer.class, CustomerRepository.class))
            .withConfigurationResource("application.properties");

    private static final ZonedDateTime BIRTHDATE = ZonedDateTime.now();

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @Transactional
    void whenFindByNameThenReturnsCorrectResult() {

        assertEquals(2, customerRepository.findByName("Adam")
                .size());

        assertEquals(2, customerRepository.findByNameIs("Adam")
                .size());

        assertEquals(2, customerRepository.findByNameEquals("Adam")
                .size());

        assertEquals(1, customerRepository.findByNameIsNull()
                .size());
        assertEquals(5, customerRepository.findByNameIsNotNull()
                .size());
    }

    @Test
    @Transactional
    void whenFindingByNameNotAdamThenReturnsCorrectResult() {

        assertEquals(3, customerRepository.findByNameNot("Adam")
                .size());
        assertEquals(3, customerRepository.findByNameIsNot("Adam")
                .size());

    }

    @Test
    @Transactional
    void whenFindByNameStartingWith_thenReturnsCorrectResult() {

        assertEquals(2, customerRepository.findByNameStartingWith("A")
                .size());
    }

    @Test
    @Transactional
    void whenFindByNameLikePatternThenReturnsCorrectResult() {

        assertEquals(2, customerRepository.findByNameLike("%im%")
                .size());
    }

    @Test
    @Transactional
    void whenFindByNameEndingWith_thenReturnsCorrectResult() {

        assertEquals(2, customerRepository.findByNameEndingWith("e")
                .size());
    }

    @Test
    @Transactional
    void whenByNameContaining_thenReturnsCorrectResult() {

        assertEquals(1, customerRepository.findByNameContaining("v")
                .size());
    }

    @Test
    @Transactional
    void whenFindingByNameEndingWithMThenReturnsThree() {

        assertEquals(3, customerRepository.findByNameEndingWith("m")
                .size());
    }

    @Test
    @Transactional
    void whenByAgeLessThanThenReturnsCorrectResult() {

        assertEquals(3, customerRepository.findByAgeLessThan(25)
                .size());
    }

    @Test
    @Transactional
    void whenByAgeLessThanEqualThenReturnsCorrectResult() {

        assertEquals(4, customerRepository.findByAgeLessThanEqual(25)
                .size());
    }

    @Test
    @Transactional
    void whenByAgeGreaterThan25ThenReturnsTwo() {
        assertEquals(2, customerRepository.findByAgeGreaterThan(25)
                .size());
    }

    @Test
    @Transactional
    void whenFindingByAgeGreaterThanEqual25ThenReturnsThree() {

        assertEquals(3, customerRepository.findByAgeGreaterThanEqual(25)
                .size());
    }

    @Test
    @Transactional
    void whenFindingByAgeBetween20And30ThenReturnsFour() {

        assertEquals(4, customerRepository.findByAgeBetween(20, 30)
                .size());
    }

    @Test
    @Transactional
    void whenFindingByBirthDateAfterYesterdayThenReturnsCorrectResult() {

        final ZonedDateTime yesterday = BIRTHDATE.minusDays(1);
        assertEquals(6, customerRepository.findByBirthDateAfter(yesterday)
                .size());
    }

    @Test
    @Transactional
    void whenByBirthDateBeforeThenReturnsCorrectResult() {

        final ZonedDateTime yesterday = BIRTHDATE.minusDays(1);
        assertEquals(0, customerRepository.findByBirthDateBefore(yesterday)
                .size());
    }

    @Test
    @Transactional
    void whenByActiveTrueThenReturnsCorrectResult() {

        assertEquals(2, customerRepository.findByActiveTrue()
                .size());
    }

    @Test
    @Transactional
    void whenByActiveFalseThenReturnsCorrectResult() {

        assertEquals(4, customerRepository.findByActiveFalse()
                .size());
    }

    @Test
    @Transactional
    void whenByAgeInThenReturnsCorrectResult() {

        final List<Integer> ages = Arrays.asList(20, 25);
        assertEquals(3, customerRepository.findByAgeIn(ages)
                .size());
    }

    @Test
    @Transactional
    void whenByNameOrAge() {

        assertEquals(3, customerRepository.findByNameOrAge("Adam", 20)
                .size());
    }

    @Test
    @Transactional
    void whenByNameOrAgeAndActive() {

        assertEquals(2, customerRepository.findByNameOrAgeAndActive("Adam", 20, false)
                .size());
    }

    @Test
    @Transactional
    void whenByNameAndAgeAndActive() {

        assertEquals(1, customerRepository.findAllByNameAndAgeAndActive("Adam", 20, false)
                .size());
    }

    @Test
    @Transactional
    void whenByNameOrAgeOrActive() {

        assertEquals(3, customerRepository.findAllByNameOrAgeOrActive("Adam", 20, true)
                .size());
    }

    @Test
    @Transactional
    void whenByNameAndAgeOrActive() {

        assertEquals(3, customerRepository.findAllByNameAndAgeOrActive("Adam", 20, true)
                .size());
    }

    @Test
    @Transactional
    void whenByNameOrderByName() {

        assertEquals(2, customerRepository.findByNameOrderByName("Adam")
                .size());
        assertEquals(2, customerRepository.findByNameOrderByNameDesc("Adam")
                .size());
        assertEquals(2, customerRepository.findByNameOrderByNameAsc("Adam")
                .size());
    }

}
