package io.quarkus.arc.test.injection.assignability.generics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;

public class AssignabilityWithGenericsTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Car.class, Engine.class, PetrolEngine.class, Vehicle.class,
            StringListConsumer.class, ListConsumer.class, ProducerBean.class, DefinitelyNotBar.class,
            Bar.class, GenericInterface.class, AlmostCompleteBean.class, ActualBean.class,
            BetaFace.class, GammaFace.class, GammaImpl.class, AbstractAlpha.class, AlphaImpl.class);

    @Test
    public void testSelectingInstanceOfCar() {
        InstanceHandle<Car> instance = Arc.container().instance(Car.class);
        assertTrue(instance.isAvailable());
        assertNotNull(instance.get().getEngine());
    }

    @Test
    public void testParameterizedTypeWithTypeVariable() {
        InstanceHandle<StringListConsumer> instance = Arc.container().instance(StringListConsumer.class);
        assertTrue(instance.isAvailable());
        assertNotNull(instance.get().getList());
    }

    @Test
    public void testHierarchyWithInterfacesAndMap() {
        InstanceHandle<ActualBean> instance = Arc.container().instance(ActualBean.class);
        assertTrue(instance.isAvailable());
        assertNotNull(instance.get().getInjectedMap());
    }

    @Test
    public void testProxiedBeanWithGenericMethodParams() {
        InstanceHandle<AlphaImpl> alphaInstance = Arc.container().instance(AlphaImpl.class);
        InstanceHandle<GammaImpl> gammaInstance = Arc.container().instance(GammaImpl.class);
        assertTrue(alphaInstance.isAvailable());
        assertTrue(gammaInstance.isAvailable());
        AlphaImpl alpha = alphaInstance.get();
        assertEquals(GammaImpl.class.getSimpleName(), alpha.ping(alpha.getParam()));
    }

    interface GenericInterface<T, K> {

    }

    interface BetaFace<K> {
        K ping();
    }

    interface GammaFace extends BetaFace<String> {

    }

    @Dependent
    static class StringListConsumer extends ListConsumer<String> {

    }

    static class ListConsumer<T> {

        @Inject
        List<T> list;

        public List<T> getList() {
            return list;
        }
    }

    @Dependent
    static class ProducerBean {

        @Produces
        String foo = "foo";

        @Produces
        List<String> produceList() {
            return new ArrayList<>();
        }

        @Produces
        Map<String, Bar> produceMap() {
            return new HashMap<>();
        }

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

        public void observeSomethingElse(@ObservesAsync String event, K injectedInstance) {
            // inject-ability is verified at bootstrap
        }

        public Map<T, K> getInjectedMap() {
            return injectedMap;
        }
    }

    @ApplicationScoped
    static class ActualBean extends AlmostCompleteBean<String, Bar> {

    }

    @ApplicationScoped
    static class GammaImpl implements GammaFace {

        @Override
        public String ping() {
            return GammaImpl.class.getSimpleName();
        }
    }

    static abstract class AbstractAlpha<T extends BetaFace> {

        @Inject
        T param;

        public T getParam() {
            return param;
        }

        public String ping(T param) {
            return param.ping().toString();
        }

    }

    @ApplicationScoped
    static class AlphaImpl extends AbstractAlpha<GammaFace> {

    }

}
