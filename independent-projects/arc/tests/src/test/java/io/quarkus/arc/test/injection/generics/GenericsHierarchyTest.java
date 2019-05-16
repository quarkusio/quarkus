package io.quarkus.arc.test.injection.generics;

import static org.junit.Assert.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;

public class GenericsHierarchyTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Car.class, Engine.class, PetrolEngine.class, Vehicle.class,
            StringListConsumer.class, ListConsumer.class, StringListProducer.class);

    @Test
    public void testSelectingInstanceOfCar() {
        assertTrue(Arc.container().instance(Car.class).isAvailable());
    }

    @Test
    public void testParameterizedTypeWithTypeVariable() {
        assertTrue(Arc.container().instance(StringListConsumer.class).isAvailable());
    }

    @Dependent
    static class StringListConsumer extends ListConsumer<String> {

    }

    static class ListConsumer<T> {

        @Inject
        List<T> list;
    }

    @Dependent
    static class StringListProducer {

        @Produces
        List<String> produce() {
            return new ArrayList<>();
        }

    }

}
