package io.quarkus.hibernate.orm;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import javax.inject.Inject;

import org.hibernate.StatelessSession;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.enhancer.Address;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @author Max Rydahl Andersen <manderse@redhat.com>
 */
public class StatelessSessionTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(Address.class)
                    .addAsResource("application.properties"));

    @Inject
    StatelessSession statelessSession;

    @Test
    public void testStatelessSession() {
        Arc.container().requestContext().activate();
        try {
            List list = statelessSession.createNativeQuery("SELECT VALUE FROM INFORMATION_SCHEMA.SETTINGS")
                    .addScalar("VALUE")
                    .list();
            assertNotNull(list);
        } finally {
            Arc.container().requestContext().terminate();
        }
    }

}
