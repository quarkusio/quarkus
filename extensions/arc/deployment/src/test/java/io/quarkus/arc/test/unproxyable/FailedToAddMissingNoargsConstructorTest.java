package io.quarkus.arc.test.unproxyable;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FailedToAddMissingNoargsConstructorTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(FailedToAddMissingNoargsConstructorTest.class, MyBean.class))
            .setExpectedException(DeploymentException.class);

    @Inject
    MyBean bean;

    @Test
    public void testConstructorWasNotAdded() {
        Assertions.fail();
    }

    @ApplicationScoped
    static class MyBean extends BaseBean {

        String foo;

        MyBean(BeanManager beanManager) {
            super(beanManager);
        }

    }

    static class BaseBean {

        BeanManager beanManager;

        public BaseBean(BeanManager beanManager) {
            this.beanManager = beanManager;
        }

        BeanManager getBeanManager() {
            return beanManager;
        }

    }

}
