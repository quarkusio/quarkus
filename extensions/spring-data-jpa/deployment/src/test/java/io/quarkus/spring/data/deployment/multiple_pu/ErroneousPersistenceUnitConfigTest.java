package io.quarkus.spring.data.deployment.multiple_pu;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.spring.data.deployment.multiple_pu.first.FirstEntity;
import io.quarkus.spring.data.deployment.multiple_pu.first.FirstEntityRepository;
import io.quarkus.spring.data.deployment.multiple_pu.second.SecondEntity;
import io.quarkus.spring.data.deployment.multiple_pu.second.SecondEntityRepository;
import io.quarkus.test.QuarkusUnitTest;

public class ErroneousPersistenceUnitConfigTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest().setExpectedException(IllegalStateException.class)
            .withApplicationRoot((jar) -> jar
                    .addClasses(FirstEntity.class, SecondEntity.class, FirstEntityRepository.class,
                            SecondEntityRepository.class, PanacheTestResource.class)
                    .addAsResource("application-erroneous-multiple-persistence-units.properties",
                            "application.properties"));

    @Test
    public void shouldNotReachHere() {
        Assertions.fail();
    }
}
