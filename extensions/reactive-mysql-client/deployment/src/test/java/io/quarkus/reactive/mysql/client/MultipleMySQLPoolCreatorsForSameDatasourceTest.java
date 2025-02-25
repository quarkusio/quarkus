package io.quarkus.reactive.mysql.client;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.mysqlclient.spi.MySQLDriver;
import io.vertx.sqlclient.Pool;

public class MultipleMySQLPoolCreatorsForSameDatasourceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(CustomCredentialsProvider.class)
                    .addClass(CredentialsTestResource.class)
                    .addClass(LocalhostMySQLPoolCreator.class)
                    .addClass(AnotherMySQLPoolCreator.class)
                    .addAsResource("application-credentials-with-erroneous-url.properties", "application.properties"))
            .setExpectedException(DeploymentException.class);

    @Test
    public void test() {
        fail("Should never have been called");
    }

    @Singleton
    public static class AnotherMySQLPoolCreator implements MySQLPoolCreator {

        @Override
        public Pool create(Input input) {
            return MySQLDriver.INSTANCE.createPool(input.vertx(), input.mySQLConnectOptionsList(), input.poolOptions());
        }
    }

}
