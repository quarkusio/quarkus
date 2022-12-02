package io.quarkus.arc.test.unproxyable;

import static org.junit.jupiter.api.Assertions.fail;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ProducerFailedToAddMissingNoargsConstructorTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ProducerFailedToAddMissingNoargsConstructorTest.class, MyProducer.class, MyBean.class,
                            MyBase.class))
            .setExpectedException(DeploymentException.class);

    @Inject
    MyBean bean;

    @Test
    public void testConstructorWasNotAdded() {
        fail();
    }

    @Singleton
    static class MyProducer {

        @Produces
        @ApplicationScoped
        MyBean produce() {
            return new MyBean("bar");
        }

    }

    static class MyBean extends MyBase {

        public MyBean(String foo) {
            super(foo);
        }

    }

    static class MyBase {

        public MyBase(String foo) {
        }

    }

}
