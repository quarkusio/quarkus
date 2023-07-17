package io.quarkus.narayana.jta;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(TransactionalResource.class)
public class TransactionalTestCase extends BaseTransactionTest {

    @Test
    public void test() {
        runTest();
    }
}
