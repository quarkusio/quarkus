package io.quarkus.hibernate.orm.stateless;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

public class StatelessSessionWithinRequestScopeTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
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
        assertThatThrownBy(() -> statelessSession.insert(new MyEntity("john")))
                .hasMessageContaining("Transaction is not active");
    }

    @AfterEach
    public void terminateRequestContext() {
        Arc.container().requestContext().terminate();
    }
}
