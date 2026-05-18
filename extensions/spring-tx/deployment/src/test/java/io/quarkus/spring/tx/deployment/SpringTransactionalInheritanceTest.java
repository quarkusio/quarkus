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

public class SpringTransactionalInheritanceTest {

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(BaseTransactionalBean.class, SubclassBean.class));

    @Inject
    SubclassBean bean;

    @Test
    public void testInheritedTransactional() throws Exception {
        assertThat(bean.inheritedMethod()).isTrue();
    }

    @Test
    public void testOwnMethod() throws Exception {
        assertThat(bean.ownMethod()).isTrue();
    }

    @Transactional
    static class BaseTransactionalBean {

        @Inject
        TransactionManager tm;

        public boolean inheritedMethod() throws Exception {
            return tm.getTransaction() != null;
        }
    }

    @ApplicationScoped
    static class SubclassBean extends BaseTransactionalBean {

        public boolean ownMethod() throws Exception {
            return tm.getTransaction() != null;
        }
    }
}
