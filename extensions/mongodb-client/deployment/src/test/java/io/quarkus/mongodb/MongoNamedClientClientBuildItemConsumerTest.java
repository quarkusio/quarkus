package io.quarkus.mongodb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.literal.NamedLiteral;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkus.arc.Arc;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.mongodb.deployment.MongoClientBuildItem;
import io.quarkus.mongodb.deployment.MongoClientNameBuildItem;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.qlue.annotation.Step;
import io.quarkus.test.QuarkusUnitTest;

public class MongoNamedClientClientBuildItemConsumerTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(MongoTestBase.class))
            .withConfigurationResource("named-mongoclient.properties")
            .addBuildStepObject(new Object() {
                // This represents the extension.
                @Step
                MongoClientNameBuildItem step1(ApplicationArchivesBuildItem ignored) {
                    return new MongoClientNameBuildItem("second");
                }

                @Step
                FeatureBuildItem step2(List<MongoClientBuildItem> ignored) {
                    return new FeatureBuildItem("dummy");
                }
            });

    @Test
    public void testContainerHasBeans() {
        assertThat(Arc.container().instance(MongoClient.class, Default.Literal.INSTANCE).get()).isNotNull();
        assertThat(Arc.container().instance(MongoClient.class, NamedLiteral.of("second")).get()).isNotNull();
        assertThat(Arc.container().instance(ReactiveMongoClient.class, Default.Literal.INSTANCE).get()).isNotNull();
        assertThat(Arc.container().instance(ReactiveMongoClient.class, NamedLiteral.of("secondreactive")).get()).isNotNull();
    }
}
