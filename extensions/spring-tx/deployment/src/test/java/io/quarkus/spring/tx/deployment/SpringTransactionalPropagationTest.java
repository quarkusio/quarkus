package io.quarkus.spring.tx.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionManager;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.quarkus.test.QuarkusExtensionTest;

public class SpringTransactionalPropagationTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(PropagationBean.class));

    @Inject
    PropagationBean bean;

    @Test
    public void testSupportsWithoutExistingTransaction() throws Exception {
        assertThat(bean.supportsMethod()).isFalse();
    }

    @Test
    public void testRequiresNew() throws Exception {
        assertThat(bean.requiresNewMethod()).isTrue();
    }

    @ApplicationScoped
    static class PropagationBean {

        @Inject
        TransactionManager tm;

        @Transactional(propagation = Propagation.SUPPORTS)
        public boolean supportsMethod() throws Exception {
            return tm.getTransaction() != null;
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public boolean requiresNewMethod() throws Exception {
            return tm.getTransaction() != null;
        }
    }
}
