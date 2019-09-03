package io.quarkus.arc.test.producer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.IllegalProductException;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class NullProducerMethodTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(NullProducerMethodTest.class, NullProducerMethodTest.MyBean.class,
                            NullProducerMethodTest.MyBeanProducer.class));

    @Inject
    Instance<MyBean> instance;

    @Test
    public void testNullProducerShouldThrowIllegalProductException() {
        assertThrows(IllegalProductException.class, instance::get);
    }

    class MyBean {
    }

    @ApplicationScoped
    static class MyBeanProducer {
        @Produces
        MyBean produceNull() {
            return null;
        }
    }
}
