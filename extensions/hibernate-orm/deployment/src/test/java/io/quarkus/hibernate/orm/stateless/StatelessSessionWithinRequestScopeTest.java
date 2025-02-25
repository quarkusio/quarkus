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
                    .addAsResource(EmptyAsset.INSTANCE, "import.sql"));

    @Inject
    StatelessSession statelessSession;

    @BeforeEach
    public void activateRequestContext() {
        Arc.container().requestContext().activate();
    }

    @Test
    public void read() {
        assertEquals(0L, statelessSession
                .createSelectionQuery("SELECT entity FROM MyEntity entity WHERE name IS NULL", MyEntity.class)
                .getResultCount());
    }

    @Test
    public void write() {
        assertEquals(0L, statelessSession
                .createSelectionQuery("SELECT entity FROM MyEntity entity", MyEntity.class)
                .getResultCount());
        // TODO: On contrary to Session, it seems we don't prevent writes on StatelessSessions with no transaction active?
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
