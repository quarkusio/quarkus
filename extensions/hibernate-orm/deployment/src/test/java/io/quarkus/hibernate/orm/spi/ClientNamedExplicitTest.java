package io.quarkus.hibernate.orm.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.Session;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.datasource.deployment.spi.component.DataSourceRequestBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.HibernateOrmClientDefinedBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.HibernateOrmClientLookupHandlerBuildItem;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Tests that a persistence unit configured with an explicit client name
 * ({@code quarkus.hibernate-orm.client=myfakeclient}) uses the matching client.
 * <p>
 * The fake client redirects to a named datasource ("ds1") at runtime.
 */
public class ClientNamedExplicitTest {

    private static final String CLIENT_NAME = "myfakeclient";

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(ContributedEntity.class)
                    .addClass(FakeClientRuntimeInitListener.class))
            .withConfiguration("""
                    quarkus.hibernate-orm.client=myfakeclient
                    """)
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new HibernateOrmClientLookupHandlerBuildItem((name, paradigm) -> {
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
                                        PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME)
                                        .setInitListener(listener));
                    }
                })
                        .produces(HibernateOrmClientLookupHandlerBuildItem.class)
                        .produces(HibernateOrmClientDefinedBuildItem.class)
                        .produces(DataSourceRequestBuildItem.class)
                        .produces(HibernateOrmIntegrationRuntimeConfiguredBuildItem.class)
                        .build();
            }
        };
    }

    @Inject
    Session session;

    @Test
    @Transactional
    public void persistenceUnitUsesNamedClient() {
        ContributedEntity entity = new ContributedEntity("world");
        session.persist(entity);
        session.flush();
        session.clear();

        ContributedEntity loaded = session.get(ContributedEntity.class, entity.getId());
        assertThat(loaded).isNotNull();
        assertThat(loaded.getName()).isEqualTo("world");
    }

    @Test
    public void dialectIsH2() {
        SessionFactoryImplementor sf = session.getSessionFactory().unwrap(SessionFactoryImplementor.class);
        assertThat(sf.getJdbcServices().getDialect()).isInstanceOf(H2Dialect.class);
    }
}
