package io.quarkus.arc.test.transientreference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.TransientReference;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class TransientReferenceDestroyedTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Controller.class, InterceptedController.class, BeerProducer.class,
            Beer.class);

    @Test
    public void testTransientReferences() {
        Controller controller = Arc.container().instance(Controller.class).get();
        assertNotNull(controller.theBeer);
        assertTrue(Arc.container().instance(Integer.class).get() > 0);
        assertEquals(3, BeerProducer.DESTROYED.size(), "Destroyed beers: " + BeerProducer.DESTROYED);
        assertTrue(BeerProducer.DESTROYED.contains(1));
        assertTrue(BeerProducer.DESTROYED.contains(2));
        assertTrue(BeerProducer.DESTROYED.contains(3));

        BeerProducer.COUNTER.set(0);
        BeerProducer.DESTROYED.clear();

        InterceptedController interceptedController = Arc.container().instance(InterceptedController.class).get();
        assertNotNull(interceptedController.getTheBeer());
        assertTrue(Arc.container().instance(Long.class).get() > 0);
        assertEquals(3, BeerProducer.DESTROYED.size(), "Destroyed beers: " + BeerProducer.DESTROYED);
        assertTrue(BeerProducer.DESTROYED.contains(1));
        assertTrue(BeerProducer.DESTROYED.contains(2));
        assertTrue(BeerProducer.DESTROYED.contains(3));
    }

    @Singleton
    static class Controller {

        @Inject
        Beer theBeer;

        Controller(@TransientReference Beer beer) {
        }

        @Inject
        void setBeer(@TransientReference Beer beer) {
        }

        @Produces
        int produceInt(@TransientReference Beer beer) {
            return beer.id;
        }

    }

    @ActivateRequestContext
    @ApplicationScoped
    static class InterceptedController {

        @Inject
        Beer theBeer;

        InterceptedController() {
        }

        @Inject
        InterceptedController(@TransientReference Beer beer) {
        }

        public Beer getTheBeer() {
            return theBeer;
        }

        @Inject
        void setBeer(@TransientReference Beer beer) {
        }

        @Produces
        long produceLong(@TransientReference Beer beer) {
            return beer.id;
        }

    }

    @Singleton
    static class BeerProducer {

        static final AtomicInteger COUNTER = new AtomicInteger();
        static final List<Integer> DESTROYED = new CopyOnWriteArrayList<>();

        @Dependent
        @Produces
        Beer newBeer(InjectionPoint injectionPoint) {
            int id;
            if (injectionPoint.getAnnotated() instanceof AnnotatedField) {
                id = 0;
            } else {
                id = COUNTER.incrementAndGet();
            }
            return new Beer(id);
        }

        void disposeBeer(@Disposes Beer beer) {
            DESTROYED.add(beer.id);
        }

    }

    static class Beer {

        int id;

        public Beer(int id) {
            this.id = id;
        }

    }

}
