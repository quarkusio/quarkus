package io.quarkus.reactive.pg.client;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactive.datasource.PoolCreator;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.pgclient.PgBuilder;
import io.vertx.sqlclient.Pool;

public class MultiplePgPoolCreatorsForSameDatasourceTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(CustomCredentialsProvider.class)
                    .addClass(CredentialsTestResource.class)
                    .addClass(LocalhostPgPoolCreator.class)
                    .addClass(AnotherPgPoolCreator.class)
                    .addAsResource("application-credentials-with-erroneous-url.properties", "application.properties"))
            .setExpectedException(DeploymentException.class);

    @Test
    public void test() {
        fail("Should never have been called");
    }

    @Singleton
    public static class AnotherPgPoolCreator implements PoolCreator {

        @Override
        public Pool create(Input input) {
            return PgBuilder.pool().with(input.poolOptions()).using(input.vertx()).connectingTo(input.connectOptionsList())
                    .build();
        }
    }

}
