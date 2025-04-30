package io.quarkus.reactive.mysql.client;

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
    io.vertx.mutiny.sqlclient.Pool mutinyPool;
    @Inject
    io.vertx.sqlclient.Pool pool;

    @Test
    public void test() {
        Assertions.assertThat(ClientProxy.unwrap(pool)).isSameAs(ClientProxy.unwrap(mutinyPool.getDelegate()));
    }
}
