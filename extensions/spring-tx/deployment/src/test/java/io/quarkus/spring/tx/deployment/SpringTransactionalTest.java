package io.quarkus.spring.tx.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionManager;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.transaction.annotation.Transactional;

import io.quarkus.test.QuarkusExtensionTest;

public class SpringTransactionalTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(TransactionalBean.class));

    @Inject
    TransactionalBean bean;

    @Test
    public void testDefaultTransactional() throws Exception {
        assertThat(bean.inTransaction()).isTrue();
    }

    @ApplicationScoped
    static class TransactionalBean {

        @Inject
        TransactionManager tm;

        @Transactional
        public boolean inTransaction() throws Exception {
            return tm.getTransaction() != null;
        }
    }
}
