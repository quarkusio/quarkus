package io.quarkus.it.panache.next;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class PanacheNextInjectMockTest {

    @Inject
    PersonService personService;

    @InjectMock
    Person.Repository personRepository;

    @Test
    void testMockFindById() {
        Person testPerson = new Person();
        testPerson.id = 999L;
        testPerson.name = "Mock Person";
        testPerson.age = 42;
        Mockito.when(personRepository.findById(1L)).thenReturn(testPerson);

        Person result = personService.findById(1L);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(999L, result.id);
        Assertions.assertEquals("Mock Person", result.name);
    }

    @Test
    void testMockCount() {
        Mockito.when(personRepository.count()).thenReturn(23L);
        Assertions.assertEquals(23L, personService.count());

        Mockito.when(personRepository.count()).thenReturn(42L);
        Assertions.assertEquals(42L, personService.count());
    }
}
