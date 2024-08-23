package com.example.grpc.hibernate;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

import com.example.test.MutinyTestRawGrpc;
import com.example.test.TestRaw;
import com.example.test.TestRawClient;

import io.quarkus.test.junit.QuarkusIntegrationTest;

@QuarkusIntegrationTest
public class BlockingRawIT extends BlockingRawTestBase {

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
    TestRaw getClient() {
        if (client == null) {
            client = GrpcIntegrationTestHelper.createClient(9000, TestRawClient.class, MutinyTestRawGrpc::newMutinyStub);
        }
        return client;
    }
}
