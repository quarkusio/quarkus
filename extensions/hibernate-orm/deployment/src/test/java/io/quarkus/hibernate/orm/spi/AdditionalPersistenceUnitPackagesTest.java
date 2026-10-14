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
import io.quarkus.hibernate.orm.spi.pkgscan.AlsoManagedEntity;
import io.quarkus.hibernate.orm.spi.pkgscan.PackageScannedEntity;
import io.quarkus.test.QuarkusExtensionTest;

public class AdditionalPersistenceUnitPackagesTest {

    static final String PERSISTENCE_UNIT_NAME = "contributed";

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(PackageScannedEntity.class)
                    .addClass(AlsoManagedEntity.class)
                    .addAsResource("application-additional-persistence-unit-packages.properties", "application.properties"))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(AdditionalPersistenceUnitBuildItem.builder(PERSISTENCE_UNIT_NAME)
                                .dataSourceName("vector")
                                .managedClass(AlsoManagedEntity.class.getName())
                                .packages(PackageScannedEntity.class.getPackageName())
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
    public void packageScannedEntityIsUsable() {
        PackageScannedEntity entity = new PackageScannedEntity("hello");
        contributedSession.persist(entity);
        contributedSession.flush();
        contributedSession.clear();

        PackageScannedEntity loaded = contributedSession.get(PackageScannedEntity.class, entity.getId());
        assertThat(loaded).isNotNull();
        assertThat(loaded.getName()).isEqualTo("hello");
    }

    @Test
    @Transactional
    public void entityAssignedByBothPackageAndManagedClassIsUsable() {
        AlsoManagedEntity entity = new AlsoManagedEntity("world");
        contributedSession.persist(entity);
        contributedSession.flush();
        contributedSession.clear();

        AlsoManagedEntity loaded = contributedSession.get(AlsoManagedEntity.class, entity.getId());
        assertThat(loaded).isNotNull();
        assertThat(loaded.getName()).isEqualTo("world");
    }
}
