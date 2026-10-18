package io.quarkus.arc.test.producer.primitive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;

public class PrimitiveWrapperProducerTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Producers.class, Injection.class, Observer.class,
            ProducerDisposer.class);

    @Test
    public void testPrimitiveWrapperNullProducers() {
        // field injection, constructor injetion, initializer method
        Injection bean = Arc.container().instance(Injection.class).get();
        assertEquals(false, bean.bool);
        assertEquals((byte) 0, bean.b);
        assertEquals((short) 0, bean.s);
        assertEquals(0, bean.i);
        assertEquals(0L, bean.l);
        assertEquals(0.0F, bean.f);
        assertEquals(0.0, bean.d);
        assertEquals((char) 0, bean.c);

        // observer method
        Arc.container().beanManager().getEvent().fire("foo");
        assertEquals(false, Observer.bool);
        assertEquals((byte) 0, Observer.b);
        assertEquals((short) 0, Observer.s);
        assertEquals(0, Observer.i);
        assertEquals(0L, Observer.l);
        assertEquals(0.0F, Observer.f);
        assertEquals(0.0, Observer.d);
        assertEquals((char) 0, Observer.c);

        // producer method
        InstanceHandle<MyPojo> handle = Arc.container().instance(MyPojo.class);
        assertNotNull(handle.get());
        assertEquals(false, ProducerDisposer.producer_bool);
        assertEquals((byte) 0, ProducerDisposer.producer_b);
        assertEquals((short) 0, ProducerDisposer.producer_s);
        assertEquals(0, ProducerDisposer.producer_i);
        assertEquals(0L, ProducerDisposer.producer_l);
        assertEquals(0.0F, ProducerDisposer.producer_f);
        assertEquals(0.0, ProducerDisposer.producer_d);
        assertEquals((char) 0, ProducerDisposer.producer_c);

        // disposer method
        handle.destroy();
        assertEquals(false, ProducerDisposer.disposer_bool);
        assertEquals((byte) 0, ProducerDisposer.disposer_b);
        assertEquals((short) 0, ProducerDisposer.disposer_s);
        assertEquals(0, ProducerDisposer.disposer_i);
        assertEquals(0L, ProducerDisposer.disposer_l);
        assertEquals(0.0F, ProducerDisposer.disposer_f);
        assertEquals(0.0, ProducerDisposer.disposer_d);
        assertEquals((char) 0, ProducerDisposer.disposer_c);
    }

    @Dependent
    static class Producers {
        @Produces
        Boolean bool() {
            return null;
        }

        @Produces
        Byte b = null;

        @Produces
        Short s() {
            return null;
        }

        @Produces
        Integer i = null;

        @Produces
        Long l() {
            return null;
        }

        @Produces
        Float f = null;

        @Produces
        Double d() {
            return null;
        }

        @Produces
        Character c = null;
    }

    @Dependent
    static class Injection {
        @Inject
        boolean bool = true;
        @Inject
        byte b = 1;
        @Inject
        short s = 1;
        int i = 1;
        long l = 1L;
        float f = 1.0F;
        double d = 1.0;
        char c = 'a';

        @Inject
        Injection(int i, long l) {
            this.i = i;
            this.l = l;
        }

        @Inject
        void doubleParamInit(float f, double d) {
            this.f = f;
            this.d = d;
        }

        @Inject
        void singleParamInit(char c) {
            this.c = c;
        }
    }

    @Dependent
    static class Observer {
        static boolean bool = true;
        static byte b = 1;
        static short s = 1;
        static int i = 1;
        static long l = 1L;
        static float f = 1.0F;
        static double d = 1.0;
        static char c = 'a';

        void observe(@Observes String event, boolean bool, byte b, short s, int i, long l, float f, double d, char c) {
            Observer.bool = bool;
            Observer.b = b;
            Observer.s = s;
            Observer.i = i;
            Observer.l = l;
            Observer.f = f;
            Observer.d = d;
            Observer.c = c;
        }
    }

    static class MyPojo {
    }

    @Dependent
    static class ProducerDisposer {
        static boolean producer_bool = true;
        static byte producer_b = 1;
        static short producer_s = 1;
        static int producer_i = 1;
        static long producer_l = 1L;
        static float producer_f = 1.0F;
        static double producer_d = 1.0;
        static char producer_c = 'a';

        static boolean disposer_bool = true;
        static byte disposer_b = 1;
        static short disposer_s = 1;
        static int disposer_i = 1;
        static long disposer_l = 1L;
        static float disposer_f = 1.0F;
        static double disposer_d = 1.0;
        static char disposer_c = 'a';

        @Produces
        @Dependent
        MyPojo produce(boolean bool, byte b, short s, int i, long l, float f, double d, char c) {
            ProducerDisposer.producer_bool = bool;
            ProducerDisposer.producer_b = b;
            ProducerDisposer.producer_s = s;
            ProducerDisposer.producer_i = i;
            ProducerDisposer.producer_l = l;
            ProducerDisposer.producer_f = f;
            ProducerDisposer.producer_d = d;
            ProducerDisposer.producer_c = c;
            return new MyPojo();
        }

        void dispose(@Disposes MyPojo ignored, boolean bool, byte b, short s, int i, long l, float f, double d, char c) {
            ProducerDisposer.disposer_bool = bool;
            ProducerDisposer.disposer_b = b;
            ProducerDisposer.disposer_s = s;
            ProducerDisposer.disposer_i = i;
            ProducerDisposer.disposer_l = l;
            ProducerDisposer.disposer_f = f;
            ProducerDisposer.disposer_d = d;
            ProducerDisposer.disposer_c = c;
        }
    }
}
