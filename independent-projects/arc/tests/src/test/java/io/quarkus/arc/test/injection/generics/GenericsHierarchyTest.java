package io.quarkus.arc.test.injection.generics;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class GenericsHierarchyTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Car.class, Engine.class, PetrolEngine.class, Vehicle.class);

    @Test
    public void testSelectingInstanceOfCar() {
       Assert.assertTrue(Arc.container().instance(Car.class).isAvailable());
    }
}
