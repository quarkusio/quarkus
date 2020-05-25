package io.quarkus.hibernate.orm;

import static org.junit.jupiter.api.Assertions.assertFalse;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.enhancer.Address;
import io.quarkus.test.QuarkusUnitTest;

public class HibernateMetadataWithoutInjectionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(Address.class)
                    .addClass(MyEntity.class)
                    .addAsResource("application.properties"));

    @Inject
    EntityManager em;

    @Test
    public void testNoBean() {
        assertFalse(Arc.container().instance(HibernateMetadata.class).isAvailable());
    }
}
