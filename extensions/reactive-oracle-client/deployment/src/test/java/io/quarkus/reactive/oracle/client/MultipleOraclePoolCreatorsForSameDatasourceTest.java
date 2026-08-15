package io.quarkus.reactive.oracle.client;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.reactive.datasource.PoolCreator;
import io.quarkus.test.QuarkusExtensionTest;
import io.vertx.oracleclient.OracleConnectOptions;
import io.vertx.sqlclient.Pool;

public class MultipleOraclePoolCreatorsForSameDatasourceTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(CustomCredentialsProvider.class)
                    .addClass(CredentialsTestResource.class)
                    .addClass(LocalhostOraclePoolCreator.class)
                    .addClass(AnotherOraclePoolCreator.class)
                    .addAsResource("application-credentials-with-erroneous-url.properties", "application.properties"))
            .setExpectedException(DeploymentException.class);

    @Test
    public void test() {
        fail("Should never have been called");
    }

    @Singleton
    public static class AnotherOraclePoolCreator implements PoolCreator {

        @Override
        public Pool create(Input input) {
            OracleConnectOptions oracleConnectOptions = (OracleConnectOptions) input.connectOptionsList().get(0);
            return Pool.pool(input.vertx(), oracleConnectOptions, input.poolOptions());
        }
    }

}
