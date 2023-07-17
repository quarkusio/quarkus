package io.quarkus.hibernate.orm.panache.deployment.test.multiple_pu;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.panache.deployment.test.multiple_pu.first.FirstEntity;
import io.quarkus.hibernate.orm.panache.deployment.test.multiple_pu.second.SecondEntity;
import io.quarkus.test.QuarkusUnitTest;

public class ErroneousPersistenceUnitConfigTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setExpectedException(IllegalStateException.class)
            .withApplicationRoot((jar) -> jar
                    .addClasses(FirstEntity.class, SecondEntity.class, PanacheTestResource.class)
                    .addAsResource("application-erroneous-multiple-persistence-units.properties", "application.properties"));

    @Test
    public void shouldNotReachHere() {
        Assertions.fail();
    }
}
