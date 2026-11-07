package io.quarkus.mongodb;

import static io.quarkus.mongodb.deployment.spi.MongoClientBuildItem.Type.BLOCKING;
import static io.quarkus.mongodb.deployment.spi.MongoClientBuildItem.Type.REACTIVE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.mongodb.client.MongoClient;

import io.quarkus.arc.Arc;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.mongodb.deployment.spi.MongoClientBuildItem;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * An extension can mark a single client type as unremovable, so only that type is registered when there are no
 * injection points. Client types marked as unremovable by different build items of the same client are merged.
 */
public class MongoUnremovableClientTypesTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .overrideConfigKey("quarkus.mongodb.connection-string", "mongodb://localhost:9999")
            .overrideConfigKey("quarkus.mongodb.second.connection-string", "mongodb://localhost:9999")
            .overrideConfigKey("quarkus.mongodb.third.connection-string", "mongodb://localhost:9999")
            .addBuildChainCustomizer(buildCustomizer());

    @Test
    public void onlyUnremovableTypesAreRegistered() {
        assertThat(Arc.container().select(ReactiveMongoClient.class).isResolvable()).isTrue();
        assertThat(Arc.container().select(MongoClient.class).isUnsatisfied()).isTrue();

        MongoClientName second = MongoClientName.Literal.of("second");
        assertThat(Arc.container().select(MongoClient.class, second).isResolvable()).isTrue();
        assertThat(Arc.container().select(ReactiveMongoClient.class, second).isUnsatisfied()).isTrue();
    }

    @Test
    public void unremovableTypesOfTheSameClientAreMerged() {
        MongoClientName third = MongoClientName.Literal.of("third");
        assertThat(Arc.container().select(MongoClient.class, third).isResolvable()).isTrue();
        assertThat(Arc.container().select(ReactiveMongoClient.class, third).isResolvable()).isTrue();
    }

    private static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            // This represents the extension.
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(context -> context.produce(List.of(
                        MongoClientBuildItem.defaultClientUnremovable(REACTIVE),
                        MongoClientBuildItem.ofUnremovable("second", BLOCKING),
                        MongoClientBuildItem.ofUnremovable("third", BLOCKING),
                        MongoClientBuildItem.ofUnremovable("third", REACTIVE))))
                        .produces(MongoClientBuildItem.class)
                        .build();
            }
        };
    }
}
