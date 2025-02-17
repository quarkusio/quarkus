package io.quarkus.reactive.mssql.client;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.sqlclient.Pool;

public class MultipleMSSQLPoolCreatorsForSameDatasourceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(CustomCredentialsProvider.class)
                    .addClass(CredentialsTestResource.class)
                    .addClass(LocalhostMSSQLPoolCreator.class)
                    .addClass(AnotherMSSQLPoolCreator.class)
                    .addAsResource("application-credentials-with-erroneous-url.properties", "application.properties"))
            .setExpectedException(DeploymentException.class);

    @Test
    public void test() {
        fail("Should never have been called");
    }

    @Singleton
    public static class AnotherMSSQLPoolCreator implements MSSQLPoolCreator {

        @Override
        public Pool create(Input input) {
            return Pool.pool(input.vertx(), input.msSQLConnectOptions(), input.poolOptions());
        }
    }

}
