package io.quarkus.mongodb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkus.arc.Arc;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.mongodb.deployment.MongoClientBuildItem;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.qlue.annotation.Step;
import io.quarkus.test.QuarkusUnitTest;

public class MongoClientBuildItemConsumerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(MongoTestBase.class))
            .withConfigurationResource("default-mongoclient.properties")
            .addBuildStepObject(new Object() {
                @Step
                FeatureBuildItem run(List<MongoClientBuildItem> ignored) {
                    return new FeatureBuildItem("dummy");
                }
            });

    @Test
    public void testContainerHasBeans() {
        assertThat(Arc.container().instance(MongoClient.class).get()).isNotNull();
        assertThat(Arc.container().instance(ReactiveMongoClient.class).get()).isNotNull();
    }
}
