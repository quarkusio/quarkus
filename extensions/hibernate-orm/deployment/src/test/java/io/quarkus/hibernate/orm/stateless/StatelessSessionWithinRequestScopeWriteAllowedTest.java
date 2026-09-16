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
import io.quarkus.test.QuarkusExtensionTest;

public class StatelessSessionWithinRequestScopeWriteAllowedTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, PrefixPhysicalNamingStrategy.class)
                    .addAsResource(EmptyAsset.INSTANCE, "import.sql"))
            .overrideConfigKey("quarkus.hibernate-orm.request-scoped.stateless-session.allow-write", "true");

    @Inject
    StatelessSession statelessSession;

    @BeforeEach
    public void activateRequestContext() {
        Arc.container().requestContext().activate();
    }

    @Test
    public void write() {
        assertEquals(0L, statelessSession
                .createSelectionQuery("SELECT entity FROM MyEntity entity", MyEntity.class)
                .getResultCount());
        statelessSession.insert(new MyEntity("john"));
        assertEquals(1L, statelessSession
                .createSelectionQuery("SELECT entity FROM MyEntity entity", MyEntity.class)
                .getResultCount());
    }

    @AfterEach
    public void terminateRequestContext() {
        Arc.container().requestContext().terminate();
    }
}
