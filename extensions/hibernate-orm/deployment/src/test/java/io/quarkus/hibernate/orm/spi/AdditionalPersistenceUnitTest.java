package io.quarkus.hibernate.orm.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalPersistenceUnitBuildItem;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Tests that an extension can contribute a static persistence unit through
 * {@link AdditionalPersistenceUnitBuildItem}, even though the application does not declare it in
 * configuration, and that doing so does not spuriously activate the default persistence unit
 * (see <a href="https://github.com/quarkusio/quarkus/issues/54592">#54592</a> and
 * <a href="https://github.com/quarkusio/quarkus/issues/54493">#54493</a>).
 */
public class AdditionalPersistenceUnitTest {

    static final String PERSISTENCE_UNIT_NAME = "contributed";

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(ContributedEntity.class)
                    .addAsResource("application-additional-persistence-unit.properties", "application.properties"))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        // Declare the persistence unit; its managed classes are automatically added to the JPA
                        // model and assigned to it by the Hibernate ORM extension.
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

    @Inject
    @PersistenceUnit(PERSISTENCE_UNIT_NAME)
    Session contributedSession;

    @Test
    @Transactional
    public void contributedPersistenceUnitIsUsable() {
        ContributedEntity entity = new ContributedEntity("hello");
        contributedSession.persist(entity);
        contributedSession.flush();
        contributedSession.clear();

        ContributedEntity loaded = contributedSession.get(ContributedEntity.class, entity.getId());
        assertThat(loaded).isNotNull();
        assertThat(loaded.getName()).isEqualTo("hello");
    }
}
