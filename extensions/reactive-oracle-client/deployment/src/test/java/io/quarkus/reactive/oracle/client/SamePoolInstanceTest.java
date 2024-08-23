package io.quarkus.reactive.oracle.client;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.ClientProxy;
import io.quarkus.test.QuarkusUnitTest;

public class SamePoolInstanceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest();

    @Inject
    io.vertx.mutiny.oracleclient.OraclePool mutinyPool;
    @Inject
    io.vertx.oracleclient.OraclePool pool;

    @Test
    public void test() {
        Assertions.assertThat(ClientProxy.unwrap(pool)).isSameAs(ClientProxy.unwrap(mutinyPool.getDelegate()));
    }
}
