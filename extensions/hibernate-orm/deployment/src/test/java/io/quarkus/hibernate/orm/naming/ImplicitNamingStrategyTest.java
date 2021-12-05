package io.quarkus.hibernate.orm.naming;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.test.QuarkusUnitTest;

public class ImplicitNamingStrategyTest {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class, CustomImplicitNamingStrategy.class)
                    .addAsResource(EmptyAsset.INSTANCE, "import.sql")
                    .addAsResource("application-implicit-naming-strategy.properties", "application.properties"));

    @Inject
    EntityManager entityManager;

    @BeforeEach
    public void activateRequestContext() {
        Arc.container().requestContext().activate();
    }

    @Test
    public void testImplicitNamingStrategy() throws Exception {
        // Check if "TBL_MyEntity" was created
        Number result = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM TBL_IO_QUARKUS_HIBERNATE_ORM_MYENTITY")
                .getSingleResult();
        assertEquals(0, result.intValue());
    }

    @AfterEach
    public void terminateRequestContext() {
        Arc.container().requestContext().terminate();
    }

}
