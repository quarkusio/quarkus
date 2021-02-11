package io.quarkus.arc.test.unproxyable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ProducerAddMissingNoargsConstructorTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ProducerAddMissingNoargsConstructorTest.class, MyProducer.class, MyBean.class));

    @Inject
    MyBean bean;

    @Test
    public void testConstructorWasAdded() {
        assertEquals("bar", bean.getFoo());
    }

    @Singleton
    static class MyProducer {

        @Produces
        @ApplicationScoped
        MyBean produce() {
            return new MyBean("bar");
        }

    }

    static class MyBean extends BeanBase {

        final String foo;

        // The absence of a no-args constructor should normally result in deployment exception
        public MyBean(String foo) {
            this.foo = foo;
        }

        public String getFoo() {
            return foo;
        }

    }

    static class BeanBase {

    }

}
