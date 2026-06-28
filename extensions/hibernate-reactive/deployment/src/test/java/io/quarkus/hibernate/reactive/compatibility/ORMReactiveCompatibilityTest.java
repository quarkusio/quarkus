package io.quarkus.hibernate.reactive.compatibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.logging.LogRecord;

import org.hibernate.SessionFactory;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.BeforeParameterizedClassInvocation;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.arc.Arc;
import io.quarkus.builder.Version;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.reactive.entities.Hero;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.vertx.sqlclient.Pool;

/**
 * Tests ORM/Reactive compatibility for all 10 supported configuration scenarios.
 */
@ParameterizedClass
@MethodSource("scenarios")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ORMReactiveCompatibilityTest extends CompatibilityUnitTestBase {

    private static final List<Dependency> JDBC_DEPS = List.of(
            Dependency.of("io.quarkus", "quarkus-jdbc-postgresql-deployment", Version.getVersion()));

    // Common connection properties shared across all default-datasource scenarios.
    private static final String DEFAULT_DS_CREDENTIALS = """
            quarkus.datasource.db-kind=postgresql
            quarkus.datasource.username=hibernate_orm_test
            quarkus.datasource.password=hibernate_orm_test
            """;

    // Common connection properties shared across named-datasource scenarios.
    private static final String NAMED_DS_CREDENTIALS = """
            quarkus.datasource."named-datasource".db-kind=postgresql
            quarkus.datasource."named-datasource".username=hibernate_orm_test
            quarkus.datasource."named-datasource".password=hibernate_orm_test
            """;

    enum Scenario {
        DEFAULT_BOTH(
                JDBC_DEPS,
                DEFAULT_DS_CREDENTIALS + """
                        quarkus.hibernate-orm.schema-management.strategy=drop-and-create
                        quarkus.datasource.reactive=true
                        quarkus.hibernate-orm.log.format-sql=false
                        quarkus.hibernate-orm.log.highlight-sql=false
                        quarkus.log.category."org.hibernate.SQL".level=DEBUG
                        """,
                true, true),
        DEFAULT_BLOCKING_ONLY(
                JDBC_DEPS,
                DEFAULT_DS_CREDENTIALS + """
                        quarkus.hibernate-orm.schema-management.strategy=drop-and-create
                        quarkus.datasource.reactive=false
                        """,
                false, true),
        DEFAULT_REACTIVE_ONLY_NO_JDBC_DRIVER(
                List.of(),
                DEFAULT_DS_CREDENTIALS + """
                        quarkus.hibernate-orm.schema-management.strategy=drop-and-create
                        quarkus.datasource.reactive=true
                        """,
                true, false),
        DEFAULT_REACTIVE_BLOCKING_SESSION_DISABLED(
                JDBC_DEPS,
                DEFAULT_DS_CREDENTIALS + """
                        quarkus.hibernate-orm.schema-management.strategy=drop-and-create
                        quarkus.hibernate-orm.blocking=false
                        quarkus.datasource.reactive=true
                        """,
                true, false),
        DEFAULT_REACTIVE_JDBC_DISABLED(
                JDBC_DEPS,
                DEFAULT_DS_CREDENTIALS + """
                        quarkus.hibernate-orm.schema-management.strategy=drop-and-create
                        quarkus.datasource.jdbc=false
                        quarkus.datasource.reactive=true
                        """,
                true, false),
        NAMED_DATASOURCE_BOTH(
                JDBC_DEPS,
                NAMED_DS_CREDENTIALS + """
                        quarkus.hibernate-orm.schema-management.strategy=drop-and-create
                        quarkus.hibernate-orm.datasource=named-datasource
                        quarkus.datasource."named-datasource".reactive=true
                        """,
                true, true),
        NAMED_DATASOURCE_NAMED_PU_BOTH(
                JDBC_DEPS,
                NAMED_DS_CREDENTIALS + """
                        quarkus.hibernate-orm."named-pu".schema-management.strategy=drop-and-create
                        quarkus.hibernate-orm."named-pu".datasource=named-datasource
                        quarkus.hibernate-orm."named-pu".packages=io.quarkus.hibernate.reactive.entities
                        quarkus.datasource."named-datasource".reactive=true
                        quarkus.log.category."io.quarkus.hibernate".level=DEBUG
                        """,
                true, true,
                "named-pu", "named-pu", null),
        NAMED_DATASOURCE_REACTIVE_ONLY(
                List.of(),
                NAMED_DS_CREDENTIALS + """
                        quarkus.hibernate-orm.schema-management.strategy=drop-and-create
                        quarkus.hibernate-orm.datasource=named-datasource
                        quarkus.datasource."named-datasource".reactive=true
                        """,
                true, false,
                null, null, "named-datasource"),
        NAMED_REACTIVE_DEFAULT_BLOCKING(
                JDBC_DEPS,
                NAMED_DS_CREDENTIALS + DEFAULT_DS_CREDENTIALS + """
                        quarkus.datasource."named-datasource".jdbc=false
                        quarkus.datasource."named-datasource".reactive=true
                        quarkus.hibernate-orm."named-pu".schema-management.strategy=drop-and-create
                        quarkus.hibernate-orm."named-pu".datasource=named-datasource
                        quarkus.hibernate-orm."named-pu".packages=io.quarkus.hibernate.reactive.entities
                        quarkus.datasource.reactive=false
                        quarkus.hibernate-orm.packages=io.quarkus.hibernate.reactive.entities
                        quarkus.hibernate-orm.database.generation=drop-and-create
                        quarkus.log.category."io.quarkus.hibernate".level=DEBUG
                        """,
                true, true,
                "named-pu", null, null),
        DIFFERENT_NAMED_DATASOURCES_NAMED_PU_BOTH(
                JDBC_DEPS,
                """
                        quarkus.datasource."named-datasource-reactive".jdbc=false
                        quarkus.datasource."named-datasource-reactive".reactive=true
                        quarkus.datasource."named-datasource-reactive".db-kind=postgresql
                        quarkus.datasource."named-datasource-reactive".username=hibernate_orm_test
                        quarkus.datasource."named-datasource-reactive".password=hibernate_orm_test
                        quarkus.hibernate-orm."named-pu-reactive".schema-management.strategy=drop-and-create
                        quarkus.hibernate-orm."named-pu-reactive".datasource=named-datasource-reactive
                        quarkus.hibernate-orm."named-pu-reactive".packages=io.quarkus.hibernate.reactive.entities
                        quarkus.datasource."named-datasource-blocking".jdbc=true
                        quarkus.datasource."named-datasource-blocking".reactive=false
                        quarkus.datasource."named-datasource-blocking".db-kind=postgresql
                        quarkus.datasource."named-datasource-blocking".username=hibernate_orm_test
                        quarkus.datasource."named-datasource-blocking".password=hibernate_orm_test
                        quarkus.hibernate-orm."named-pu-blocking".schema-management.strategy=drop-and-create
                        quarkus.hibernate-orm."named-pu-blocking".datasource=named-datasource-blocking
                        quarkus.hibernate-orm."named-pu-blocking".packages=io.quarkus.hibernate.reactive.entities
                        quarkus.log.category."io.quarkus.hibernate".level=DEBUG
                        """,
                true, true,
                "named-pu-reactive", "named-pu-blocking", null);

        final List<Dependency> dependencies;
        final String configuration;
        final boolean reactiveWorks;
        final boolean blockingWorks;
        final String reactivePuName;
        final String blockingPuName;
        final String namedPoolDatasource;

        Scenario(List<Dependency> dependencies, String configuration,
                boolean reactiveWorks, boolean blockingWorks) {
            this(dependencies, configuration, reactiveWorks, blockingWorks, null, null, null);
        }

        Scenario(List<Dependency> dependencies, String configuration,
                boolean reactiveWorks, boolean blockingWorks,
                String reactivePuName, String blockingPuName,
                String namedPoolDatasource) {
            this.dependencies = dependencies;
            this.configuration = configuration;
            this.reactiveWorks = reactiveWorks;
            this.blockingWorks = blockingWorks;
            this.reactivePuName = reactivePuName;
            this.blockingPuName = blockingPuName;
            this.namedPoolDatasource = namedPoolDatasource;
        }
    }

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Hero.class)
                    .addAsResource("complexMultilineImports.sql", "import.sql"));

    @Parameter(0)
    public Scenario scenario;

    public static List<Arguments> scenarios() {
        return Arrays.stream(Scenario.values()).map(Arguments::of).toList();
    }

    @BeforeParameterizedClassInvocation
    static void configure(Scenario scenario) {
        config.resetForParameterizedClass()
                .setForcedDependencies(scenario.dependencies)
                .withConfiguration(scenario.configuration);

        if (scenario == Scenario.DEFAULT_BOTH) {
            config.setLogRecordPredicate(record -> "org.hibernate.SQL".equals(record.getLoggerName()))
                    .assertLogRecords(records -> assertThat(records.stream().map(LogRecord::getMessage))
                            .containsOnlyOnce("create sequence hero_SEQ start with 1 increment by 50"));
        }
    }

    @Test
    @RunOnVertxContext
    public void testReactive(UniAsserter asserter) {
        if (scenario.reactiveWorks) {
            testReactiveWorks(lookupReactiveSf(), asserter);
            if (scenario.namedPoolDatasource != null) {
                assertThat(lookupNamedPool()).isNotNull();
            }
        } else {
            testReactiveDisabled();
        }
    }

    @Test
    public void testBlocking() {
        if (scenario.blockingWorks) {
            testBlockingWorks(lookupBlockingSf());
        } else {
            testBlockingDisabled();
        }
    }

    private Mutiny.SessionFactory lookupReactiveSf() {
        if (scenario.reactivePuName != null) {
            return Arc.container()
                    .select(Mutiny.SessionFactory.class,
                            new PersistenceUnit.PersistenceUnitLiteral(scenario.reactivePuName))
                    .get();
        }
        return Arc.container().instance(Mutiny.SessionFactory.class).get();
    }

    private SessionFactory lookupBlockingSf() {
        if (scenario.blockingPuName != null) {
            return Arc.container()
                    .select(SessionFactory.class,
                            new PersistenceUnit.PersistenceUnitLiteral(scenario.blockingPuName))
                    .get();
        }
        return Arc.container().instance(SessionFactory.class).get();
    }

    private Pool lookupNamedPool() {
        return Arc.container()
                .select(Pool.class, new ReactiveDataSource.ReactiveDataSourceLiteral(scenario.namedPoolDatasource))
                .get();
    }
}
