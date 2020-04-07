package io.quarkus.hibernate.orm.specialmappings;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * The particular model used in this test requires access to the ProxyFactoryFactory service
 * during the metadata validation.
 * This test verifies that such service is available in that phase: otherwise we
 * wouldn't be able to bootstrap successfully.
 * See also https://github.com/quarkusio/quarkus/issues/8350
 */
public class EarlyMetadaValidationTest {

    @RegisterExtension
    final static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DataIdentity.class, IdVersionPK.class, NormalPointEntity.class, PointEntity.class)
                    .addAsResource("application.properties"));

    @Test
    public void testSuccessfulBoot() {
        // Should be able to boot with these entities
    }

}
