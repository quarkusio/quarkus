package io.quarkus.hibernate.orm.specialmappings;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

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
