package io.quarkus.mongodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkus.test.QuarkusUnitTest;

public class MongoCommandListenerTest extends MongoTestBase {

    @Inject
    MongoClient client;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class).addClasses(MongoTestBase.class, MockCommandListener.class))
            .withConfigurationResource("default-mongoclient.properties");

    @AfterEach
    void cleanup() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void testClientInitialization() {
        assertThat(client.listDatabaseNames().first()).isNotEmpty();
        assertThat(MockCommandListener.EVENTS, hasSize(1));
        assertThat(MockCommandListener.EVENTS, hasItems(equalTo("listDatabases")));
    }

}
