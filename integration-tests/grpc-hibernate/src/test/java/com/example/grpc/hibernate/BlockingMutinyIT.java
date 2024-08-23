package com.example.grpc.hibernate;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

import com.example.test.MutinyTestGrpc;
import com.example.test.Test;
import com.example.test.TestClient;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class BlockingMutinyIT extends BlockingMutinyTestBase {

    @BeforeAll
    static void init() {
        GrpcIntegrationTestHelper.init();
    }

    @AfterAll
    static void cleanup() {
        GrpcIntegrationTestHelper.cleanup();
    }

    @AfterEach
    void close() {
        if (client != null) {
            client = null;
        }
    }

    /**
     * Native tests cannot get the injected client, thus we build the client directly.
     *
     * @return the test client
     */
    @Override
    Test getClient() {
        if (client == null) {
            client = GrpcIntegrationTestHelper.createClient(9000, TestClient.class, MutinyTestGrpc::newMutinyStub);
        }
        return client;
    }
}
