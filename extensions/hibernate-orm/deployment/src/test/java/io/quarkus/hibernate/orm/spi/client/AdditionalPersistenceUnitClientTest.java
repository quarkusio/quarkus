package io.quarkus.hibernate.orm.spi.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.Session;
import org.hibernate.dialect.H2Dialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.datasource.deployment.spi.component.DataSourceRequestBuildItem;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalPersistenceUnitBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.client.HibernateOrmClientDefinedBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.client.HibernateOrmClientHandlerBuildItem;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Tests that an extension can contribute a persistence unit through
 * {@link AdditionalPersistenceUnitBuildItem} backed by an external client
 * (instead of a datasource).
 */
public class AdditionalPersistenceUnitClientTest {

    static final String PERSISTENCE_UNIT_NAME = "contributed";
    private static final String CLIENT_NAME = "myfakeclient";

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(ClientEntity.class)
                    .addClass(FakeClientRuntimeInitListener.class))
            .withConfiguration("""
                    quarkus.datasource.ds1.db-kind=h2
                    """)
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(AdditionalPersistenceUnitBuildItem.builder(PERSISTENCE_UNIT_NAME)
                                .clientName(CLIENT_NAME)
                                .managedClass(ClientEntity.class.getName())
                                .build());
                        context.produce(new HibernateOrmClientHandlerBuildItem((name, paradigm) -> {
                            if (paradigm == ProgrammingParadigm.REACTIVE) {
                                return List.of(new Reason("Fake client does not support Hibernate Reactive"));
                            }
                            if (!CLIENT_NAME.equals(name)) {
                                return List.of(new Reason(String.format(java.util.Locale.ROOT,
                                        "Fake client does not handle client '%s'", name)));
                            }
                            return List.of();
                        }));
                        context.produce(new HibernateOrmClientDefinedBuildItem(
                                CLIENT_NAME,
                                Set.of(ProgrammingParadigm.BLOCKING),
                                H2Dialect.class.getName(),
                                Map.of(),
                                true));
                        context.produce(new DataSourceRequestBuildItem(
                                "ds1", ProgrammingParadigm.BLOCKING,
                                "Fake client needs datasource 'ds1' to redirect to"));
                        FakeClientRuntimeInitListener listener = new FakeClientRuntimeInitListener();
                        listener.setRedirectDataSourceName("ds1");
                        context.produce(
                                new HibernateOrmIntegrationRuntimeConfiguredBuildItem("fake-client",
                                        PERSISTENCE_UNIT_NAME)
                                        .setInitListener(listener));
                    }
                })
                        .produces(AdditionalPersistenceUnitBuildItem.class)
                        .produces(HibernateOrmClientHandlerBuildItem.class)
                        .produces(HibernateOrmClientDefinedBuildItem.class)
                        .produces(DataSourceRequestBuildItem.class)
                        .produces(HibernateOrmIntegrationRuntimeConfiguredBuildItem.class)
                        .build();
            }
        };
    }

    @Inject
    @PersistenceUnit(PERSISTENCE_UNIT_NAME)
    Session contributedSession;

    @Test
    @Transactional
    public void contributedPersistenceUnitUsesClient() {
        ClientEntity entity = new ClientEntity("hello");
        contributedSession.persist(entity);
        contributedSession.flush();
        contributedSession.clear();

        ClientEntity loaded = contributedSession.get(ClientEntity.class, entity.getId());
        assertThat(loaded).isNotNull();
        assertThat(loaded.getName()).isEqualTo("hello");
    }
}
