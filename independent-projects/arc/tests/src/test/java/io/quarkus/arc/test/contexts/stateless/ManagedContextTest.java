package io.quarkus.arc.test.contexts.stateless;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ManagedContextTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Person.class, PersonManager.class);

    @Test
    public void testManagedContext() {
        PersonManager manager = Arc.container().select(PersonManager.class).get();
        Person person = Arc.container().select(Person.class).get();
        manager.setPerson(new Person("Bob"));
        Assertions.assertEquals("Bob", person.getName());
        manager.setPerson(new Person("Billy"));
        Assertions.assertEquals("Billy", person.getName());
    }

}
