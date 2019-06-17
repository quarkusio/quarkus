package io.quarkus.arc.test.noargsconstructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class AddMissingNoargsConstructorTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(AddMissingNoargsConstructorTest.class, MyBean.class));

    @Inject
    MyBean bean;

    @Inject
    MyOtherBean myOtherBean;

    @Test
    public void testConstructorWasAddedToEachBean() {
        assertNotNull(bean.getBeanManager());
        assertEquals("ok", bean.getFoo());
        assertEquals(true, bean.isVal());
        assertEquals(true, myOtherBean.isVal());
    }

    @ApplicationScoped
    static class MyBean extends BaseBean {

        String foo;

        final boolean val;

        // The absence of a no-args constructor should normally result in deployment exception

        // No need for @Inject
        MyBean(BeanManager beanManager) {
            super.beanManager = beanManager;
            this.val = true;
        }

        BeanManager getBeanManager() {
            return beanManager;
        }

        String getFoo() {
            return foo;
        }

        boolean isVal() {
            return val;
        }

        @PostConstruct
        void init() {
            foo = "ok";
        }

    }

    static class BaseBean {

        BeanManager beanManager;

    }

    @ApplicationScoped
    static class MyOtherBean {

        final boolean val;

        MyOtherBean(BeanManager beanManager) {
            this.val = true;
        }

        boolean isVal() {
            return val;
        }
    }

}
