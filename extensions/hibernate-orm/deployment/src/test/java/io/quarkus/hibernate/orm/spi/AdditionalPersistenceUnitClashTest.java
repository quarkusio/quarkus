package io.quarkus.hibernate.orm.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalPersistenceUnitBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class AdditionalPersistenceUnitClashTest {

    static final String PERSISTENCE_UNIT_NAME = "contributed";

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(ContributedEntity.class)
                    .addAsResource("application-additional-persistence-unit-clash.properties", "application.properties"))
            .addBuildChainCustomizer(buildCustomizer())
            .assertException(throwable -> assertThat(throwable)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining(PERSISTENCE_UNIT_NAME)
                    .hasMessageContaining("contributed by an extension but is also configured by the application"));

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        // Contribute a persistence unit whose name clashes with the one configured in
                        // application.properties.
                        context.produce(AdditionalPersistenceUnitBuildItem.builder(PERSISTENCE_UNIT_NAME)
                                .dataSourceName("vector")
                                .managedClass(ContributedEntity.class.getName())
                                .build());
                    }
                })
                        .produces(AdditionalPersistenceUnitBuildItem.class)
                        .build();
            }
        };
    }

    @Test
    public void buildFailsOnClash() {
        // The build is expected to fail; the assertion is performed in assertException above.
        Assertions.assertThat(true).isTrue();
    }
}
