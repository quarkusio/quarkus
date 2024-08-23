package io.quarkus.narayana.jta;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestHTTPEndpoint(TransactionalResource.class)
@TestProfile(JdbcObjectStoreTestProfile.class)
public class TransactionJdbcObjectStoreTest extends BaseTransactionTest {
    @Test
    public void test() {
        runTest();
    }
}
