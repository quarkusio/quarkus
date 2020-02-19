package io.quarkus.hibernate.orm.entitylistener;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

public class EntityListenerInjectionTestCase {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(FooBean.class, FooEntity.class, FooListener.class)
                    .addAsResource("application.properties"));

    @BeforeEach
    public void activateRequestContext() {
        Arc.container().requestContext().activate();
    }

    @AfterEach
    public void terminateRequestContext() {
        Arc.container().requestContext().terminate();
    }

    @Inject
    EntityManager em;

    @Test
    @Transactional
    public void shouldNotCrash() {
        FooEntity o = new FooEntity();
        em.persist(o);
    }

    @Test
    @Transactional
    public void shouldInvokeEntityListener() {
        FooEntity o = new FooEntity();
        em.persist(o);
        assertEquals("Yeah!", o.getData());
    }
}
