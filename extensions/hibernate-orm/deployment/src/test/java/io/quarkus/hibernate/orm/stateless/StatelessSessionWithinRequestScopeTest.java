package io.quarkus.hibernate.orm.stateless;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.hibernate.StatelessSession;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.naming.PrefixPhysicalNamingStrategy;
import io.quarkus.test.QuarkusUnitTest;

public class StatelessSessionWithinRequestScopeTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, PrefixPhysicalNamingStrategy.class)
                    .addAsResource(EmptyAsset.INSTANCE, "import.sql")
                    .addAsResource("application-physical-naming-strategy.properties", "application.properties"));

    @Inject
    StatelessSession statelessSession;

    @BeforeEach
    public void activateRequestContext() {
        Arc.container().requestContext().activate();
    }

    @Test
    public void test() throws Exception {
        Number result = (Number) statelessSession.createNativeQuery("SELECT COUNT(*) FROM TBL_MYENTITY").getSingleResult();
        assertEquals(0, result.intValue());
    }

    @AfterEach
    public void terminateRequestContext() {
        Arc.container().requestContext().terminate();
    }
}
