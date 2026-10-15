package io.quarkus.panache.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.component.QuarkusComponentTest;

@QuarkusComponentTest
@MockPanacheEntities({ Person.class, Subclass.class })
public class EntityComponentTest {

    @Inject
    MyComponent myComponent;

    @Test
    public void testMock() {
        Mockito.when(Person.count()).thenReturn(23L);
        Mockito.when(Person.count("from foo")).thenReturn(13L);
        Mockito.when(Person.findOrdered()).thenReturn(List.of(new Person()));
        assertEquals(23, Person.count());
        assertEquals(13, Person.count("from foo"));
        assertEquals(23, myComponent.ping());
        // user method
        List<Person> list = Person.findOrdered();
        assertEquals(1, list.size());
        assertNull(list.get(0).name);
        // default values
        assertEquals(0, Person.deleteAll());
        assertNull(Person.findById("1"));
    }

    @Test
    public void testSubclass() {
        Mockito.when(Subclass.count()).thenReturn(23L);
        Mockito.when(Subclass.count("from foo")).thenReturn(13L);
        assertEquals(23, Subclass.count());
        assertEquals(13, Subclass.count("from foo"));
    }

}
