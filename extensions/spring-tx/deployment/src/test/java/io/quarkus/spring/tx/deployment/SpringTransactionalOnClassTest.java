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

public class SpringTransactionalOnClassTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(ClassLevelTransactionalBean.class));

    @Inject
    ClassLevelTransactionalBean bean;

    @Test
    public void testClassLevelTransactional() throws Exception {
        assertThat(bean.methodOne()).isTrue();
        assertThat(bean.methodTwo()).isTrue();
    }

    @ApplicationScoped
    @Transactional
    static class ClassLevelTransactionalBean {

        @Inject
        TransactionManager tm;

        public boolean methodOne() throws Exception {
            return tm.getTransaction() != null;
        }

        public boolean methodTwo() throws Exception {
            return tm.getTransaction() != null;
        }
    }
}
