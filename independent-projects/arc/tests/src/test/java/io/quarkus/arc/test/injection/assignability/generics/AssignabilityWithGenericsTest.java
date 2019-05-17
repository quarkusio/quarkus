package io.quarkus.arc.test.injection.assignability.generics;

import static org.junit.Assert.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;

public class AssignabilityWithGenericsTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Car.class, Engine.class, PetrolEngine.class, Vehicle.class,
            StringListConsumer.class, ListConsumer.class, ProducerBean.class, DefinitelyNotBar.class, Bar.class,
            GenericInterface.class, AlmostCompleteBean.class, ActualBean.class);

    @Test
    public void testSelectingInstanceOfCar() {
        assertTrue(Arc.container().instance(Car.class).isAvailable());
    }

    @Test
    public void testParameterizedTypeWithTypeVariable() {
        assertTrue(Arc.container().instance(StringListConsumer.class).isAvailable());
    }

    @Test
    public void testHierarchyWithInterfacesAndMap() {
        assertTrue(Arc.container().instance(ActualBean.class).isAvailable());
    }

    @Dependent
    static class StringListConsumer extends ListConsumer<String> {

    }

    static class ListConsumer<T> {

        @Inject
        List<T> list;
    }

    @Dependent
    static class ProducerBean {

        @Produces
        List<String> produceList() {
            return new ArrayList<>();
        }

        @Produces
        Map<String, Bar> produceMap() {
            return new HashMap<>();
        }

        @Produces
        String foo = "foo";

    }

    static interface GenericInterface<T, K> {

    }

    static class DefinitelyNotBar<D> {

    }

    static class Bar extends DefinitelyNotBar<Integer> {

    }

    static abstract class AlmostCompleteBean<T, K extends DefinitelyNotBar<Integer>> implements GenericInterface<T, K> {

        @Inject
        Map<T, K> injectedMap;

        public void observeSomething(@Observes String event, T injectedInstance) {
            // inject-ability is verified at bootstrap
        }

        public void observeSomethingElse(@Observes String event, K injectedInstance) {
            // inject-ability is verified at bootstrap
        }
    }

    @ApplicationScoped
    static class ActualBean extends AlmostCompleteBean<String, Bar> {

    }

}
